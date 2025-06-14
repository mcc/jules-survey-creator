# Auth Service

## Environment Configuration

The Spring Boot application utilizes Maven profiles to manage environment-specific configurations, particularly for CORS settings.

### Profiles
- `local`: For local development. Sets CORS for `http://127.0.0.1:5173` and `http://localhost:5173`.
- `dev`: For the development environment.
- `uat`: For the UAT environment.
- `prod`: For the production environment.

Each profile activates a corresponding `application-<profile>.properties` file. The `dev`, `uat`, and `prod` profiles have placeholder CORS origins that need to be configured with the actual frontend URLs for those environments.

### Running Locally
- **Using IDE:** Select the `local` Maven profile when running the application.
- **Command Line:** Navigate to the `auth-service` directory and run `mvn spring-boot:run -Plocal`.

CORS is configured in `mcc.survey.creator.config.WebConfig` based on properties loaded from the active profile.

## API Endpoints

### AdminController
Base Path: `/api/admin`

#### `POST /api/admin/users`
- **Description:** Creates a new user.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

#### `GET /api/admin/users`
- **Description:** Retrieves a list of all users.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

#### `GET /api/admin/users/{userId}`
- **Description:** Retrieves a specific user by their ID.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

#### `PUT /api/admin/users/{userId}`
- **Description:** Edits an existing user's details.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

#### `POST /api/admin/users/reset-password`
- **Description:** Resets a user's password. The username is provided in the request body.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

#### `PUT /api/admin/users/{userId}/status`
- **Description:** Updates the active status of a user.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

### AuthController
Base Path: `/api/auth`

#### `POST /api/auth/login`
- **Description:** Authenticates a user and returns JWT and refresh tokens.
- **Privileges:** Public.

#### `POST /api/auth/signup`
- **Description:** Registers a new user.
- **Privileges:** Public.

#### `POST /api/auth/refresh`
- **Description:** Refreshes an access token using a refresh token.
- **Privileges:** Public (requires a valid refresh token).

#### `POST /api/auth/logout`
- **Description:** Logs out a user. (Server-side context is cleared; client should delete tokens).
- **Privileges:** Public (effectively, for authenticated users).

#### `GET /api/auth/users/by-username/{username}`
- **Description:** Retrieves basic user information (ID and username) by username.
- **Privileges:** Requires authentication (`isAuthenticated()`).

#### `POST /api/auth/forgot-password`
- **Description:** Initiates the password reset process for a user by sending an email.
- **Privileges:** Public.

#### `POST /api/auth/reset-password`
- **Description:** Completes the password reset process using a token and new password.
- **Privileges:** Public (requires a valid reset token).

#### `POST /api/auth/users/change-password`
- **Description:** Allows an authenticated user to change their own password.
- **Privileges:** Requires authentication (`isAuthenticated()`).

### RoleController
Base Path: `/api/roles`

#### `GET /api/roles/authorities`
- **Description:** Retrieves a list of all available authorities/permissions.
- **Privileges:** Public (assumed, as no `@PreAuthorize`).

#### `POST /api/roles`
- **Description:** Creates a new role.
- **Privileges:** Requires `OP_CREATE_ROLES` authority or `ROLE_SYSTEM_ADMIN`.

#### `GET /api/roles`
- **Description:** Retrieves a list of all roles.
- **Privileges:** Public (assumed, as no `@PreAuthorize`).

#### `GET /api/roles/{id}`
- **Description:** Retrieves a specific role by its ID.
- **Privileges:** Public (assumed, as no `@PreAuthorize`).

#### `PUT /api/roles/{id}`
- **Description:** Updates an existing role.
- **Privileges:** Requires `OP_UPDATE_ROLES` authority or `ROLE_SYSTEM_ADMIN`.

#### `DELETE /api/roles/{id}`
- **Description:** Deletes a role by its ID.
- **Privileges:** Requires `OP_DELETE_ROLES` authority or `ROLE_SYSTEM_ADMIN`.

### SurveyController
Base Path: `/api/surveys`

#### `POST /api/surveys/`
- **Description:** Creates a new survey for the authenticated user. (Note: there are two POST / mappings, one with trailing slash, one without. Assuming this one is for creation by an authenticated user with specific privilege).
- **Privileges:** Requires `OP_CREATE_SURVEY` authority.

#### `GET /api/surveys/`
- **Description:** Retrieves all surveys for the currently authenticated user. (Note: there are two GET / mappings, one with trailing slash, one without. Assuming this one is for the authenticated user).
- **Privileges:** Requires `OP_VIEW_OWN_SURVEY` authority.

#### `GET /api/surveys/{surveyId}`
- **Description:** Retrieves a specific survey by its ID. Access is granted if the user is the owner, it's shared with them, or they have `OP_VIEW_ALL_SURVEYS` authority.
- **Privileges:** Requires `OP_VIEW_OWN_SURVEY` and ownership/shared status (checked by `@surveySecurityService.isOwnerOrSharedUser`) OR `OP_VIEW_ALL_SURVEYS` authority.

#### `POST /api/surveys/{id}/share/{userId}`
- **Description:** Shares a survey with another user.
- **Privileges:** Requires `OP_SHARE_SURVEY` authority and ownership of the survey (checked by `@surveySecurityService.isOwner`).

#### `DELETE /api/surveys/{id}/unshare/{userId}`
- **Description:** Unshares a survey from a user.
- **Privileges:** Requires `OP_SHARE_SURVEY` authority and ownership of the survey (checked by `@surveySecurityService.isOwner`).

#### `PUT /api/surveys/{surveyId}`
- **Description:** Updates an existing survey.
- **Privileges:** Requires `OP_EDIT_OWN_SURVEY` authority and ownership of the survey (checked by `@surveySecurityService.isOwner`). (Note: path variable in annotation is `#id`, but method param is `surveyId`. Assuming they correspond).

#### `DELETE /api/surveys/{surveyId}`
- **Description:** Deletes a survey.
- **Privileges:** Requires `OP_DELETE_OWN_SURVEY` authority and ownership of the survey (checked by `@surveySecurityService.isOwner`). (Note: path variable in annotation is `#id`, but method param is `surveyId`. Assuming they correspond).

#### `GET /api/surveys` (No trailing slash)
- **Description:** Retrieves surveys for the current user. (This seems to be a duplicate or alternative mapping to `GET /api/surveys/`).
- **Privileges:** Public (assumed, as no `@PreAuthorize`, relies on `getCurrentUserId()` for filtering).

#### `POST /api/surveys` (No trailing slash)
- **Description:** Creates a new survey. (This seems to be a duplicate or alternative mapping to `POST /api/surveys/`. Relies on `getCurrentUserId()` for owner assignment).
- **Privileges:** Public (assumed, as no `@PreAuthorize`).

*Note on SurveyController duplicate mappings: The controller has duplicate mappings for `GET /` and `POST /` (one with a trailing slash and PreAuthorize, one without and seemingly public). The descriptions above try to differentiate them based on annotations. This might need clarification in the code.*

### TestController
Base Path: `/api/test`

#### `GET /api/test/all`
- **Description:** Accesses public content.
- **Privileges:** Public.

#### `GET /api/test/user`
- **Description:** Accesses content available to users with `ROLE_USER`, `ROLE_USER_ADMIN`, or `ROLE_SYSTEM_ADMIN`.
- **Privileges:** Requires `ROLE_USER`, `ROLE_USER_ADMIN`, or `ROLE_SYSTEM_ADMIN`.

#### `GET /api/test/useradmin`
- **Description:** Accesses content available to users with `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.
- **Privileges:** Requires `ROLE_USER_ADMIN` or `ROLE_SYSTEM_ADMIN`.

#### `GET /api/test/systemadmin`
- **Description:** Accesses content available to users with `ROLE_SYSTEM_ADMIN`.
- **Privileges:** Requires `ROLE_SYSTEM_ADMIN`.

#### `GET /api/test/init-user`
- **Description:** Initializes a default admin user (developer utility).
- **Privileges:** Public.

## Build Process
This project uses Maven for building.

To package the application, run the following command from the `auth-service` directory:
```bash
mvn package
```
This command will compile the code, run tests, and package the application. The output will be a JAR file located in the `target/` directory (e.g., `auth-service-0.0.1-SNAPSHOT.jar`).
