package com.example.demo.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@Configuration
public class SecurityConfig {

    private static final String KEYCLOAK_REFRESHED_ATTRIBUTE = "keycloakSessionRefreshed";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${KEYCLOAK_PUBLIC_URL:http://localhost:8081}") String publicKeycloakUrl,
            @Value("${KEYCLOAK_INTERNAL_URL:http://keycloak:8080}") String internalKeycloakUrl) {
        ClientRegistration keycloak = ClientRegistration.withRegistrationId("keycloak")
                .clientId("angular-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(publicKeycloakUrl + "/realms/users/protocol/openid-connect/auth")
                .tokenUri(internalKeycloakUrl + "/realms/users/protocol/openid-connect/token")
                .jwkSetUri(internalKeycloakUrl + "/realms/users/protocol/openid-connect/certs")
                .userNameAttributeName("preferred_username")
                .clientName("Keycloak")
                .build();
        return new InMemoryClientRegistrationRepository(keycloak);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LogoutSuccessHandler keycloakLogoutSuccessHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout", "/persons/**", "/addresses/**", "/api/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/persons/new").hasRole("create_users")
                        .requestMatchers("/persons/*/edit").hasRole("edit_users")
                        .requestMatchers(HttpMethod.POST, "/persons").hasRole("create_users")
                        .requestMatchers("/persons/*/delete").hasRole("delete_users")
                        .requestMatchers("/persons/*").hasRole("edit_users")
                        .requestMatchers("/persons/**", "/addresses/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2.successHandler((request, response, authentication) -> {
                    request.getSession(true).setAttribute(KEYCLOAK_REFRESHED_ATTRIBUTE, true);
                    response.sendRedirect(request.getContextPath() + "/persons");
                }))
                .logout(logout -> logout.logoutSuccessHandler(keycloakLogoutSuccessHandler))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/"),
                        PathPatternRequestMatcher.pathPattern("/persons/**")))
                .addFilterAfter(createKeycloakSessionRefreshFilter(), AnonymousAuthenticationFilter.class)
                .build();
    }

    private OncePerRequestFilter createKeycloakSessionRefreshFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return !"GET".equals(request.getMethod())
                        || !request.getRequestURI().startsWith(request.getContextPath() + "/persons");
            }

            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                    filterChain.doFilter(request, response);
                    return;
                }

                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute(KEYCLOAK_REFRESHED_ATTRIBUTE) != null) {
                    session.removeAttribute(KEYCLOAK_REFRESHED_ATTRIBUTE);
                    filterChain.doFilter(request, response);
                    return;
                }

                response.sendRedirect(request.getContextPath() + "/oauth2/authorization/keycloak");
            }
        };
    }

    @Bean
    GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>(authorities);
            authorities.stream()
                    .filter(OidcUserAuthority.class::isInstance)
                    .map(OidcUserAuthority.class::cast)
                    .forEach(authority -> addKeycloakRoles(
                            authority.getIdToken().getClaims(), mappedAuthorities));
            return mappedAuthorities;
        };
    }

    @SuppressWarnings("unchecked")
    private static void addKeycloakRoles(Map<String, Object> claims, Set<GrantedAuthority> authorities) {
        addRoles(claims.get("realm_access"), authorities);
        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            addRoles(resources.get("angular-client"), authorities);
        }
    }

    private static void addRoles(Object access, Set<GrantedAuthority> authorities) {
        if (!(access instanceof Map<?, ?> accessMap) || !(accessMap.get("roles") instanceof Collection<?> roles)) {
            return;
        }
        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
    }

    @Bean
    LogoutSuccessHandler keycloakLogoutSuccessHandler(
            @Value("${KEYCLOAK_PUBLIC_URL:http://localhost:8081}") String publicKeycloakUrl) {
        return (request, response, authentication) -> {
            String applicationUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath() + "/");
            String redirectUri = URLEncoder.encode(applicationUrl, StandardCharsets.UTF_8);
            String idTokenHint = authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser
                    ? "&id_token_hint=" + URLEncoder.encode(oidcUser.getIdToken().getTokenValue(), StandardCharsets.UTF_8)
                    : "";
            response.sendRedirect(publicKeycloakUrl
                    + "/realms/users/protocol/openid-connect/logout?client_id=angular-client&post_logout_redirect_uri="
                    + redirectUri
                    + idTokenHint);
        };
    }
}
