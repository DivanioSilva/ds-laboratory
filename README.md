# DS Laboratory

Multi-module project built with Java 17 and Maven. The reactor contains:

- `application`: Spring Boot REST application;
- `kc-extensions`: custom Keycloak extensions.

## Setup script

[`setup.sh`](setup.sh) is the main command-line entry point for building and
managing the local environment. It can be run from any directory because it
automatically changes to the project root.

### Prerequisites

- Bash
- Docker with the Compose plugin (`docker compose`)
- Maven 3.9 or later for the `build` command
- Java 17 for the Maven build and tests

Make the script executable if required:

```bash
chmod +x setup.sh
```

Display the available commands:

```bash
./setup.sh --help
```

### `build`

```bash
./setup.sh build
```

This command performs the complete build and deployment workflow:

1. validates `docker-compose.yml`;
2. runs `mvn clean verify` for the complete Maven reactor;
3. builds every Docker image, including the Angular frontend and the customized
   Keycloak image;
4. starts all services in detached mode and removes orphaned containers;
5. prints the service status and local URLs.

The command stops immediately if validation, compilation, tests, image creation,
or deployment fails.

### `down`

```bash
./setup.sh down
```

Runs `docker compose stop`. Services are stopped, but their containers, images,
network, volumes, and data remain available. Run `./setup.sh build` to build and
start everything again, or `docker compose start` to restart the unchanged
containers without rebuilding.

### `destroy`

```bash
./setup.sh destroy
```

Runs `docker compose down --remove-orphans`. This stops and removes the Compose
containers and network, including orphaned containers. Docker images and named
volumes are not removed, so PostgreSQL and Keycloak data remain available for the
next deployment.

To remove persistent data as well, use Docker Compose manually with the `--volumes`
option. This is intentionally not exposed by `setup.sh` because it is destructive.

### Environment variables

Docker Compose automatically reads a `.env` file in the project root. Start from
the supplied example when custom values are required:

```bash
cp .env.example .env
```

The relevant variables include:

| Variable | Default | Purpose |
| --- | --- | --- |
| `POSTGRES_DB` | `persondb` | PostgreSQL database name |
| `POSTGRES_USER` | `personapp` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `personapp` | PostgreSQL password |
| `POSTGRES_PORT` | `5432` | PostgreSQL host port |
| `SPRING_PORT` | `8080` | Spring Boot API host port |
| `ANGULAR_PORT` | `4200` | Angular frontend host port |
| `KEYCLOAK_PORT` | `8081` | Keycloak host port |
| `KEYCLOAK_ADMIN` | `a` | Keycloak bootstrap administrator |
| `KEYCLOAK_ADMIN_PASSWORD` | `a` | Keycloak bootstrap password |
| `FAKE_SMTP_PORT` | `8025` | SMTP host port |
| `FAKE_SMTP_WEB_PORT` | `8082` | SMTP web interface host port |
| `FAKE_SMTP_MANAGEMENT_PORT` | `8083` | SMTP management host port |

Never commit a `.env` file containing production secrets.

## Running with Docker Compose directly

To start PostgreSQL, the Spring Boot API, and the Angular frontend directly:

```bash
docker compose up --build -d
```

The services are available at:

- Angular frontend: `http://localhost:4200`
- FreeMarker web interface: `http://localhost:8080/persons`
- Spring Boot API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Keycloak administration: `http://localhost:8081/admin`
- Development SMTP inbox: `http://localhost:8082`

By default, the application connects to
`jdbc:postgresql://localhost:5432/persondb` using `personapp` as both the username
and password. These values can be changed with `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. The container environment variables are documented in
`.env.example`.

To stop all services with Docker Compose:

```bash
docker compose down
```

Ports can be customized through the environment variables described above.

## Importing people with Spring Batch

The job does not run when the application starts. To import a CSV file, send it
in the multipart `file` field:

```bash
curl -X POST http://localhost:8080/api/persons/import \
  -F "file=@application/data/persons.csv"
```

Spring Batch reads the file, validates the records, and writes people to the
database in chunks of 10. People with the same first name, last name, and age are
ignored. The CSV file must contain this header:

```csv
firstName,lastName,age
```

## Swagger/OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON specification: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML specification: `http://localhost:8080/v3/api-docs.yaml`

## People CRUD

FreeMarker web interface: `http://localhost:8080/persons`

The FreeMarker interface uses the same `angular-client` and Keycloak login form
as the Angular frontend. Opening this URL without a session redirects to
Keycloak; after authentication, the user returns to the requested FreeMarker
page.

Address creation: `http://localhost:8080/addresses/new`

| Method | Endpoint | Operation |
| --- | --- | --- |
| `GET` | `/api/persons` | List people |
| `GET` | `/api/persons/search?firstName={name}` | Search by first name |
| `GET` | `/api/persons/{id}` | Get a person |
| `POST` | `/api/persons` | Create a person |
| `PUT` | `/api/persons/{id}` | Update a person |
| `DELETE` | `/api/persons/{id}` | Delete a person |

## Testing

```bash
mvn test
```

To test only one module, including its required reactor dependencies:

```bash
mvn -pl application -am test
mvn -pl kc-extensions -am test
```

## Angular frontend

With the backend running on port `8080`, start the frontend:

```bash
cd frontend
nvm use
pnpm install
pnpm start
```

The interface is available at `http://localhost:4200`. The development proxy
automatically forwards `/api` requests to Spring Boot.

The frontend initially displays a public page with information about the
application. The login button redirects to the `users` realm login form through
the public `angular-client`; the Angular application never collects credentials.
After a successful login, Keycloak redirects the user back to the application.
The flow uses Authorization Code with PKCE S256. Requests to `/api` automatically
include the access token in the `Authorization: Bearer` header, and the token is
refreshed before it expires.

The login form is served directly by Keycloak at `http://localhost:8081`; after
authentication, the user is redirected back to the Angular application.

### Keycloak roles

The Angular interface uses roles from the access token to show the available
person-management operations. Roles may be configured as realm roles or as
client roles of `angular-client`.

| Role | Interface capability |
| --- | --- |
| `create_users` | Displays the form for adding a person. |
| `import_users` | Displays the CSV import area. |
| `edit_users` | Displays the edit button and allows saving changes. |
| `delete_users` | Displays the delete button and allows removing a person. |

Create the roles in **Keycloak Admin Console → users → Realm roles** (or in
the `angular-client` client roles) and assign them to the appropriate user.
The user must sign out and sign in again after a role change so that the new
access token includes the updated roles.

These roles currently control the frontend interface. Protect the REST API
separately if calls made outside the Angular application must also be denied.

Angular requires Node.js 24 LTS or a compatible version specified in
`frontend/package.json`.

## Building the JARs

```bash
mvn clean package
java -jar application/target/ds-laboratory-application-0.0.1-SNAPSHOT.jar
```

The Keycloak extension JAR is generated at
`kc-extensions/target/kc-extensions-1.0.0-SNAPSHOT.jar`.

When `docker compose up --build` runs, the custom Keycloak image compiles this
module, installs the JAR in `/opt/keycloak/providers/`, and runs `kc.sh build`
before starting the server.

The `realms/realm-users-full.json` file is made available in Keycloak's import
directory. The server starts with `--import-realm` and imports the `users` realm
when it does not already exist. Data is stored in the `keycloak_data` volume;
Keycloak does not overwrite an existing realm on subsequent starts.
