# DS Laboratory

Multi-module project built with Java 17 and Maven. The reactor contains:

- `application`: Spring Boot REST application;
- `kc-extensions`: custom Keycloak extensions.

## Running the project

To compile all modules, build the images, and deploy all services locally:

```bash
./setup.sh build
```

The script runs the Maven tests, builds the Spring application, Angular frontend,
and Keycloak images with the extension installed, and starts the environment with
Docker Compose.

To stop all services while preserving containers, images, networks, volumes, and
data:

```bash
./setup.sh down
```

To stop and remove the deployment while preserving volumes and data:

```bash
./setup.sh destroy
```

To start PostgreSQL, the Spring Boot API, and the Angular frontend directly:

```bash
docker compose up --build -d
```

The services are available at:

- Angular frontend: `http://localhost:4200`
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

Ports can be customized with `SPRING_PORT` and `ANGULAR_PORT`.

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

The frontend proxy forwards `/realms` to Keycloak so that the application and
OIDC endpoints use the same `http://localhost:4200` origin.

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

The `realms/realm-users.json` file is made available in Keycloak's import
directory. The server starts with `--import-realm` and imports the `users` realm
when it does not already exist. Data is stored in the `keycloak_data` volume;
Keycloak does not overwrite an existing realm on subsequent starts.
