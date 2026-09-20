package pt.dcsilva.keycloak.reset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.keycloak.provider.ProviderConfigProperty;

class ResetPasswordLoginRedirectRequiredActionFactoryTest {

    private final ResetPasswordLoginRedirectRequiredActionFactory factory =
            new ResetPasswordLoginRedirectRequiredActionFactory();

    @Test
    void exposesProviderIdentityAndCreatesTheRequiredAction() {
        assertEquals(ResetPasswordLoginRedirectRequiredAction.PROVIDER_ID, factory.getId());
        assertEquals("Reset password and redirect to client login", factory.getDisplayText());
        assertTrue(factory.isConfigurable());
        assertInstanceOf(ResetPasswordLoginRedirectRequiredAction.class, factory.create(null));
    }

    @Test
    void exposesTargetClientConfiguration() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        assertEquals(2, metadata.size());
        assertProperty(metadata.get(0), ResetPasswordLoginRedirectRequiredAction.TARGET_CLIENT_ID,
                "Target client ID");
        assertProperty(metadata.get(1), ResetPasswordLoginRedirectRequiredAction.TARGET_REDIRECT_URI,
                "Target redirect URI");
    }

    private static void assertProperty(ProviderConfigProperty property, String name, String label) {
        assertEquals(name, property.getName());
        assertEquals(label, property.getLabel());
        assertEquals(ProviderConfigProperty.STRING_TYPE, property.getType());
        assertTrue(property.getHelpText() != null && !property.getHelpText().isBlank());
    }
}
