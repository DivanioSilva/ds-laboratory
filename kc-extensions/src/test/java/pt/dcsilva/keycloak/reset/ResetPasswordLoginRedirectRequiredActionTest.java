package pt.dcsilva.keycloak.reset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResetPasswordLoginRedirectRequiredActionTest {

    private static final String USERNAME = "alice";
    private final ResetPasswordLoginRedirectRequiredAction action =
            new ResetPasswordLoginRedirectRequiredAction();

    @Mock private RequiredActionContext context;
    @Mock private LoginFormsProvider form;
    @Mock private HttpRequest request;
    @Mock private Response response;
    @Mock private UserModel user;
    @Mock private SubjectCredentialManager credentialManager;
    @Mock private RealmModel realm;
    @Mock private ClientModel client;

    @BeforeEach
    void setUp() {
        when(context.getUser()).thenReturn(user);
        when(user.getUsername()).thenReturn(USERNAME);
    }

    @Test
    void rendersTheUpdatePasswordChallengeForTheCurrentUser() {
        prepareForm(response);

        action.requiredActionChallenge(context);

        verify(form).setAttribute("username", USERNAME);
        verify(form).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
        verify(context).challenge(response);
    }

    @Test
    void rejectsAnEmptyPassword() {
        processWithFormData(null, null);

        assertPasswordError(Messages.MISSING_PASSWORD);
        verify(user, never()).credentialManager();
    }

    @Test
    void rejectsPasswordsThatDoNotMatch() {
        processWithFormData("new-password", "different-password");

        assertPasswordError(Messages.NOTMATCH_PASSWORD);
        verify(user, never()).credentialManager();
    }

    @Test
    void updatesTheCredentialAndReportsMissingRedirectConfiguration() {
        when(user.credentialManager()).thenReturn(credentialManager);
        when(context.getConfig()).thenReturn(null);

        processWithFormData("new-password", "new-password");

        verify(credentialManager).updateCredential(any(UserCredentialModel.class));
        assertPasswordError("Configure targetClientId and targetRedirectUri for this required action.");
    }

    @Test
    void rejectsAnUnknownTargetClient() {
        configureRedirect("angular-app", "http://localhost:4200/");
        when(context.getRealm()).thenReturn(realm);
        when(realm.getClientByClientId("angular-app")).thenReturn(null);
        when(user.credentialManager()).thenReturn(credentialManager);

        processWithFormData("new-password", "new-password");

        assertPasswordError("The configured target client is not available.");
    }

    @Test
    void rejectsADisabledTargetClient() {
        configureRedirect("angular-app", "http://localhost:4200/");
        when(context.getRealm()).thenReturn(realm);
        when(realm.getClientByClientId("angular-app")).thenReturn(client);
        when(client.isEnabled()).thenReturn(false);
        when(user.credentialManager()).thenReturn(credentialManager);

        processWithFormData("new-password", "new-password");

        assertPasswordError("The configured target client is not available.");
    }

    private void processWithFormData(String password, String confirmation) {
        MultivaluedHashMap<String, String> data = new MultivaluedHashMap<>();
        if (password != null) data.putSingle("password-new", password);
        if (confirmation != null) data.putSingle("password-confirm", confirmation);
        when(context.getHttpRequest()).thenReturn(request);
        when(request.getDecodedFormParameters()).thenReturn(data);
        prepareForm(response);

        action.processAction(context);
    }

    private void prepareForm(Response response) {
        when(context.form()).thenReturn(form);
        when(form.setAttribute("username", USERNAME)).thenReturn(form);
        lenient().when(form.addError(any(FormMessage.class))).thenReturn(form);
        when(form.createResponse(UserModel.RequiredAction.UPDATE_PASSWORD)).thenReturn(response);
    }

    private void configureRedirect(String clientId, String redirectUri) {
        RequiredActionConfigModel config = new RequiredActionConfigModel();
        config.setConfig(Map.of(
                ResetPasswordLoginRedirectRequiredAction.TARGET_CLIENT_ID, clientId,
                ResetPasswordLoginRedirectRequiredAction.TARGET_REDIRECT_URI, redirectUri));
        when(context.getConfig()).thenReturn(config);
    }

    private void assertPasswordError(String expectedMessage) {
        ArgumentCaptor<FormMessage> error = ArgumentCaptor.forClass(FormMessage.class);
        verify(form).addError(error.capture());
        assertEquals(Validation.FIELD_PASSWORD, error.getValue().getField());
        assertEquals(expectedMessage, error.getValue().getMessage());
    }
}
