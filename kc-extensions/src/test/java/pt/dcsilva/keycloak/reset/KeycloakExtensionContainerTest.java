package pt.dcsilva.keycloak.reset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers(disabledWithoutDocker = true)
class KeycloakExtensionContainerTest {

    private static final String KEYCLOAK_VERSION = "26.7.3";
    private static final String ADMIN_USERNAME = "a";
    private static final String ADMIN_PASSWORD = "a";
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final HttpClient http = HttpClient.newHttpClient();

    @Container
    static final GenericContainer<?> keycloak = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(providerJar()),
                    "/opt/keycloak/providers/kc-extensions.jar")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN_USERNAME)
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withCommand("start-dev")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/realms/master")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));


    @Test
    void loadsTheRequiredActionProviderInARealKeycloakServer() throws Exception {
        String token = adminAccessToken();
        HttpResponse<String> response = get(
                "/admin/realms/master/authentication/unregistered-required-actions", token);

        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains(
                "\"providerId\":\"" + ResetPasswordLoginRedirectRequiredAction.PROVIDER_ID + "\""),
                response.body());
        assertTrue(response.body().contains(
                "\"name\":\"Reset password and redirect to client login\""), response.body());
    }

    @Test
    void resetsThePasswordAndRedirectsToTheClientLogin() throws Exception {
        String token = adminAccessToken();
        createFlowRealm(token);

        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpResponse<String> loginPage = browser.send(HttpRequest.newBuilder(uri(
                        "/realms/reset-flow/protocol/openid-connect/auth"
                                + "?client_id=frontend&redirect_uri="
                                + encode("http://app.test/callback")
                                + "&response_type=code&scope=openid"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        loginPage = followGetRedirects(browser, loginPage);
        assertTrue(cookies.getCookieStore().getCookies().stream()
                        .anyMatch(cookie -> cookie.getName().equals("KC_RESTART")),
                "Cookies after login page: " + cookies.getCookieStore().getCookies()
                        + " headers: " + loginPage.headers().map());

        HttpResponse<String> passwordPage = postForm(browser, cookies,
                formAction(loginPage.body(), "kc-form-login"),
                formField("username", "alice") + "&"
                        + formField("password", "old-password") + "&"
                        + formField("credentialId", ""));
        passwordPage = followGetRedirects(browser, cookies, passwordPage);
        assertTrue(passwordPage.body().contains("password-new"), passwordPage.body());
        assertTrue(passwordPage.body().contains("password-confirm"), passwordPage.body());

        HttpResponse<String> resetResponse = postForm(browser, cookies,
                formAction(passwordPage.body(), "kc-passwd-update-form"),
                formField("password-new", "new-password") + "&"
                        + formField("password-confirm", "new-password"));

        assertTrue(resetResponse.statusCode() >= 300 && resetResponse.statusCode() < 400,
                resetResponse.body());
        String location = resetResponse.headers().firstValue("Location").orElseThrow();
        assertTrue(location.contains("/realms/reset-flow/protocol/openid-connect/auth"), location);
        assertTrue(location.contains("client_id=frontend"), location);
        assertTrue(location.contains("redirect_uri=http%3A%2F%2Fapp.test%2Fcallback"), location);
        assertTrue(location.contains("prompt=login"), location);
    }

    private void createFlowRealm(String token) throws Exception {
        assertStatus(201, adminRequest("POST", "/admin/realms", token,
                "{\"realm\":\"reset-flow\",\"enabled\":true}"));
        assertStatus(204, adminRequest("POST",
                "/admin/realms/reset-flow/authentication/register-required-action", token,
                "{\"providerId\":\"RESET_PASSWORD_LOGIN_REDIRECT\","
                        + "\"name\":\"Reset password and redirect to client login\"}"));
        assertStatus(204, adminRequest("PUT",
                "/admin/realms/reset-flow/authentication/required-actions/RESET_PASSWORD_LOGIN_REDIRECT",
                token,
                "{\"alias\":\"RESET_PASSWORD_LOGIN_REDIRECT\","
                        + "\"name\":\"Reset password and redirect to client login\","
                        + "\"enabled\":true,\"defaultAction\":false,\"priority\":100,"
                        + "\"config\":{\"targetClientId\":\"frontend\","
                        + "\"targetRedirectUri\":\"http://app.test/callback\"}}"));
        assertStatus(201, adminRequest("POST", "/admin/realms/reset-flow/clients", token,
                "{\"clientId\":\"frontend\",\"enabled\":true,\"publicClient\":true,"
                        + "\"redirectUris\":[\"http://app.test/callback\"]}"));
        assertStatus(201, adminRequest("POST", "/admin/realms/reset-flow/users", token,
                "{\"username\":\"alice\",\"enabled\":true,"
                        + "\"requiredActions\":[\"RESET_PASSWORD_LOGIN_REDIRECT\"],"
                        + "\"credentials\":[{\"type\":\"password\","
                        + "\"value\":\"old-password\",\"temporary\":false}]}"));
    }

    private String adminAccessToken() throws IOException, InterruptedException {
        String body = formField("client_id", "admin-cli") + "&"
                + formField("username", ADMIN_USERNAME) + "&"
                + formField("password", ADMIN_PASSWORD) + "&"
                + formField("grant_type", "password");
        HttpRequest request = HttpRequest.newBuilder(uri("/realms/master/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        Matcher matcher = ACCESS_TOKEN.matcher(response.body());
        assertTrue(matcher.find(), "The Keycloak token response did not contain an access token");
        return matcher.group(1);
    }

    private HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> adminRequest(String method, String path, String token, String json)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postForm(
            HttpClient browser, CookieManager cookies, URI action, String body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(action)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookieHeader(cookies))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return browser.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> followGetRedirects(
            HttpClient browser, HttpResponse<String> response) throws IOException, InterruptedException {
        return followGetRedirects(browser, null, response);
    }

    private static HttpResponse<String> followGetRedirects(
            HttpClient browser, CookieManager cookies, HttpResponse<String> response)
            throws IOException, InterruptedException {
        HttpResponse<String> current = response;
        for (int redirects = 0; redirects < 10
                && current.statusCode() >= 300 && current.statusCode() < 400; redirects++) {
            URI next = current.uri().resolve(current.headers().firstValue("Location").orElseThrow());
            HttpRequest.Builder request = HttpRequest.newBuilder(next).GET();
            if (cookies != null) request.header("Cookie", cookieHeader(cookies));
            current = browser.send(request.build(),
                    HttpResponse.BodyHandlers.ofString());
        }
        return current;
    }

    private static String cookieHeader(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private static URI formAction(String html, String formId) {
        Pattern form = Pattern.compile("<form(?=[^>]*id=\"" + Pattern.quote(formId)
                + "\")[^>]*action=\"([^\"]+)\"[^>]*>", Pattern.DOTALL);
        Matcher matcher = form.matcher(html);
        assertTrue(matcher.find(), "Form " + formId + " was not found in: " + html);
        return URI.create(matcher.group(1).replace("&amp;", "&"));
    }

    private static void assertStatus(int expected, HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(), response.body());
    }

    private static URI uri(String path) {
        return URI.create("http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + path);
    }

    private static String formField(String name, String value) {
        return encode(name) + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Path providerJar() {
        return Path.of("target", "kc-extensions-1.0.0-SNAPSHOT-provider.jar").toAbsolutePath();
    }
}
