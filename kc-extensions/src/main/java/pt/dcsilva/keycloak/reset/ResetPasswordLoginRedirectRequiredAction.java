package pt.dcsilva.keycloak.reset;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.ModelException;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.SingleUseObjectKeyModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

/**
 * Resets a password and then starts a fresh OIDC login for the configured client.
 */
public final class ResetPasswordLoginRedirectRequiredAction implements RequiredActionProvider {

    static final String PROVIDER_ID = "RESET_PASSWORD_LOGIN_REDIRECT";
    static final String TARGET_CLIENT_ID = "targetClientId";
    static final String TARGET_REDIRECT_URI = "targetRedirectUri";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // This action is assigned explicitly by an administrator or action token.
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.challenge(context.form()
                .setAttribute("username", context.getUser().getUsername())
                .createResponse(UserModel.RequiredAction.UPDATE_PASSWORD));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String password = formData.getFirst("password-new");
        String confirmation = formData.getFirst("password-confirm");

        if (Validation.isBlank(password)) {
            challenge(context, Messages.MISSING_PASSWORD);
            return;
        }
        if (!password.equals(confirmation)) {
            challenge(context, Messages.NOTMATCH_PASSWORD);
            return;
        }

        try {
            context.getUser().credentialManager().updateCredential(UserCredentialModel.password(password, false));
            redirectToLogin(context, loginUrl(context));
        } catch (ModelException exception) {
            challenge(context, exception.getMessage());
        }
    }

    private void redirectToLogin(RequiredActionContext context, URI loginUrl) {
        invalidateActionToken(context);

        context.getAuthenticationSession().removeRequiredAction(PROVIDER_ID);
        context.getUser().removeRequiredAction(PROVIDER_ID);
        context.getEvent().success();

        new AuthenticationSessionManager(context.getSession()).removeAuthenticationSession(
                context.getRealm(), context.getAuthenticationSession(), true);

        context.challenge(Response.seeOther(loginUrl).build());
    }

    private void invalidateActionToken(RequiredActionContext context) {
        String tokenKey = context.getAuthenticationSession()
                .getAuthNote(AuthenticationManager.INVALIDATE_ACTION_TOKEN);
        if (tokenKey == null) {
            return;
        }

        SingleUseObjectKeyModel actionTokenKey = DefaultActionTokenKey.from(tokenKey);
        if (actionTokenKey != null) {
            SingleUseObjectProvider singleUseObjects = context.getSession().singleUseObjects();
            singleUseObjects.put(tokenKey, actionTokenKey.getExp() - Time.currentTime(), null);
        }
    }

    private void challenge(RequiredActionContext context, String message) {
        Response response = context.form()
                .setAttribute("username", context.getUser().getUsername())
                .addError(new FormMessage(Validation.FIELD_PASSWORD, message))
                .createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
        context.challenge(response);
    }

    private URI loginUrl(RequiredActionContext context) {
        RequiredActionConfigModel config = context.getConfig();
        Map<String, String> values = config == null ? Map.of() : config.getConfig();
        String clientId = values.get(TARGET_CLIENT_ID);
        String requestedRedirectUri = values.get(TARGET_REDIRECT_URI);

        if (Validation.isBlank(clientId) || Validation.isBlank(requestedRedirectUri)) {
            throw new ModelException("Configure targetClientId and targetRedirectUri for this required action.");
        }

        ClientModel client = context.getRealm().getClientByClientId(clientId);
        if (client == null || !client.isEnabled()) {
            throw new ModelException("The configured target client is not available.");
        }

        String redirectUri = RedirectUtils.verifyRedirectUri(context.getSession(), requestedRedirectUri, client);
        if (redirectUri == null) {
            throw new ModelException("The configured target redirect URI is not allowed for the target client.");
        }

        return UriBuilder.fromUri(context.getUriInfo().getBaseUri())
                .path("realms")
                .path(context.getRealm().getName())
                .path("protocol/openid-connect/auth")
                .queryParam("client_id", client.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid")
                .queryParam("prompt", "login")
                .build();
    }

    @Override
    public void close() {
    }
}
