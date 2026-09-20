# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.7.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Autenticação e roles

A página inicial é pública. O botão **Entrar** encaminha o utilizador para o
formulário de login do Keycloak em `http://localhost:8081`; as credenciais não
são recolhidas pela aplicação Angular. Depois da autenticação, o Keycloak
redireciona o utilizador de volta para o frontend. Ao atualizar a página, o
frontend verifica a sessão SSO de forma silenciosa para manter o contexto atual.

As seguintes roles do Keycloak determinam as operações apresentadas na
interface. Podem ser realm roles ou client roles do cliente `angular-client`.

| Role | Operação disponível |
| --- | --- |
| `create_users` | Apresenta o formulário para adicionar uma pessoa. |
| `import_users` | Apresenta a importação de ficheiros CSV. |
| `edit_users` | Apresenta a ação de editar e permite guardar alterações. |
| `delete_users` | Apresenta a ação de eliminar pessoas. |

Depois de alterar as roles de um utilizador no Keycloak, termine a sessão e
autentique-se novamente para receber um token atualizado. Estas roles controlam
a interface; a autorização dos endpoints REST deve ser aplicada no backend
quando também for necessário impedir chamadas diretas à API.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
