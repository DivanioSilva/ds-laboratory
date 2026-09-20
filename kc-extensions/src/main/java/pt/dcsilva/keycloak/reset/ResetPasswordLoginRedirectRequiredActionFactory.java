package pt.dcsilva.keycloak.reset;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public final class ResetPasswordLoginRedirectRequiredActionFactory implements RequiredActionFactory {

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new ResetPasswordLoginRedirectRequiredAction();
    }

    @Override
    public String getId() {
        return ResetPasswordLoginRedirectRequiredAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Reset password and redirect to client login";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigProperty clientId = new ProviderConfigProperty();
        clientId.setName(ResetPasswordLoginRedirectRequiredAction.TARGET_CLIENT_ID);
        clientId.setLabel("Target client ID");
        clientId.setType(ProviderConfigProperty.STRING_TYPE);
        clientId.setHelpText("OIDC client used to start the login after the password reset.");

        ProviderConfigProperty redirectUri = new ProviderConfigProperty();
        redirectUri.setName(ResetPasswordLoginRedirectRequiredAction.TARGET_REDIRECT_URI);
        redirectUri.setLabel("Target redirect URI");
        redirectUri.setType(ProviderConfigProperty.STRING_TYPE);
        redirectUri.setHelpText("A URI already allowed in the target client's Valid Redirect URIs.");

        return List.of(clientId, redirectUri);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
