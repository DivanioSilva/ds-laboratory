# KC Extensions

Maven module containing custom extensions for Keycloak 26.7.3.

## Available extension

### Reset password and redirect to client login

Required Action with the `RESET_PASSWORD_LOGIN_REDIRECT` identifier.

When this action is assigned to a user, Keycloak:

1. displays the standard password update form;
2. validates and updates the credential;
3. starts a new OIDC authentication request for the configured client;
4. forces the login form with `prompt=login`;
5. redirects the user to the configured URI after authentication.

Keycloak validates the return URI against the client's **Valid Redirect URIs**.
Unauthorized URIs are rejected.

## Building

From the `ds-laboratory` root directory:

```bash
mvn -pl kc-extensions -am clean package
```

The artifact is created at:

```text
kc-extensions/target/kc-extensions-1.0.0-SNAPSHOT.jar
```

You can also build the entire Maven reactor:

```bash
mvn clean package
```

## Testing

The test suite includes unit tests and a Testcontainers integration test. The
integration test builds the provider JAR, starts Keycloak 26.7.3 in Docker,
installs the extension, obtains an administrator token, and confirms that the
Required Action provider is available through the Keycloak Admin API. A second
container test creates a realm, client, and user, signs the user in, submits the
password reset form, and verifies the redirect to a fresh OIDC client login.

Run all tests from the project root while Docker is running:

```bash
mvn -pl kc-extensions -am test
```

When Docker is unavailable, the container test is skipped and the unit tests
continue to run.

## Running with Docker Compose

The project provides a custom image in `Dockerfile.keycloak`. During the build,
the image:

1. compiles the `kc-extensions` module;
2. copies the JAR to `/opt/keycloak/providers/kc-extensions.jar`;
3. runs `/opt/keycloak/bin/kc.sh build` to register the provider;
4. allows Keycloak to start only after those steps complete.

Run this command from the project root:

```bash
docker compose up --build keycloak
```

To start all services:

```bash
docker compose up --build
```

At startup, the root `realms` directory is mounted at
`/opt/keycloak/data/import`, and `--import-realm` imports `realm-users.json`. If
the `users` realm already exists in the persistent volume, Keycloak preserves it
and skips the duplicate import.

The administration console is available at <http://localhost:8081/admin> by
default. Credentials are configured with `KEYCLOAK_ADMIN` and
`KEYCLOAK_ADMIN_PASSWORD`; without these variables, the local environment uses
`a`/`a`.

## Manual installation

To install the extension in an external Keycloak distribution:

1. Build the module.
2. Stop the Keycloak server.
3. Copy the JAR to the providers directory:

   ```bash
   cp kc-extensions/target/kc-extensions-1.0.0-SNAPSHOT.jar /opt/keycloak/providers/kc-extensions.jar
   ```

4. Update the distribution:

   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

5. Start Keycloak again.

The JAR must be rebuilt against the same major Keycloak version used by the
server. The version is defined by the `keycloak.version` property in the parent
POM.

## Configuring Keycloak

### 1. Configure the OIDC client

In the administration console:

1. select the target realm;
2. open **Clients** and select the client that will receive the user;
3. confirm that the client is enabled and uses OpenID Connect;
4. add the required return URI to **Valid Redirect URIs**;
5. save the changes.

Example:

```text
Client ID: frontend
Valid Redirect URIs: http://localhost:4200/*
```

Use HTTPS in production and restrict URIs as much as possible. Avoid broad
patterns such as `*`.

### 2. Register and configure the Required Action

1. open **Authentication** > **Required actions**;
2. select the option to register a new Required Action;
3. select **Reset password and redirect to client login**;
4. enable the action;
5. open its configuration and complete these fields:

| Property | Description | Example |
| --- | --- | --- |
| `targetClientId` | `Client ID` of the target OIDC client | `frontend` |
| `targetRedirectUri` | URI allowed by the client's **Valid Redirect URIs** | `http://localhost:4200/` |

The client configuration must allow `targetRedirectUri`. The client must also
exist in the same realm and be enabled.

### 3. Assign the action to a user

1. open **Users** and select the user;
2. add **Reset password and redirect to client login** to the user's required
   actions;
3. save the changes;
4. ask the user to sign in.

The password update form is displayed at the next login. After changing the
password, the user is sent directly to a new OIDC login for the configured
client. Keycloak's **Account updated** information page is not displayed.

## Troubleshooting

### The action is not displayed in the console

- Confirm that the JAR exists in `/opt/keycloak/providers/`.
- Confirm that `kc.sh build` ran after the JAR was copied.
- Rebuild the image without reusing the previous layer:

  ```bash
  docker compose build --no-cache keycloak
  docker compose up keycloak
  ```

- Search the build or startup logs for `RESET_PASSWORD_LOGIN_REDIRECT`.

### “Configure targetClientId and targetRedirectUri”

The Required Action was registered without its two mandatory fields. Open its
configuration under **Authentication** > **Required actions** and complete them.

### “The configured target client is not available”

`targetClientId` does not identify an enabled client in the current realm. Check
the **Client ID**, not the display name or internal identifier.

### “The configured target redirect URI is not allowed”

The client's **Valid Redirect URIs** do not cover `targetRedirectUri`. Check the
scheme, host, port, path, and trailing slash.

### Non-secure context warning

The local Docker environment uses HTTP, so Keycloak may warn that secure cookies
are unavailable in cross-origin POST requests. This is acceptable only for
development. In production, expose Keycloak over HTTPS and configure the
hostname and reverse proxy correctly.

## Structure

```text
kc-extensions/
├── pom.xml
└── src/main/
    ├── java/pt/dcsilva/keycloak/reset/
    │   ├── ResetPasswordLoginRedirectRequiredAction.java
    │   └── ResetPasswordLoginRedirectRequiredActionFactory.java
    └── resources/META-INF/services/
        └── org.keycloak.authentication.RequiredActionFactory
```

The file under `META-INF/services` exposes the factory to the Java ServiceLoader
mechanism used by Keycloak.

## Compatibility

- Java 17
- Keycloak 26.7.3
- Maven 3.9 or later

The extension implements the internal `required-action` SPI. This SPI may change
between Keycloak versions; always validate the build and functional flow when
upgrading the server.
