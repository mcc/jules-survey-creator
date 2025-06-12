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
