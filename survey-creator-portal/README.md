# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

## Environment Configuration

The application uses Vite and `.env` files for environment-specific settings, primarily the API base URL (`VITE_API_BASE_URL`).

### Running Locally
- Run `npm run dev` (or `yarn dev`).
- This uses `.env.local` (for `VITE_API_BASE_URL=http://127.0.0.1:8080/api/auth`) to connect to the local Spring Boot backend.
- Ensure the backend (`auth-service`) is running, configured for the `local` profile.

### Building for Environments
- **Production:** `npm run build` (uses `.env.production`)
- **UAT:** `npm run build:uat` (uses `.env.uat`)
- `.env.development`, `.env.uat`, and `.env.production` contain placeholder API URLs that must be updated with actual backend URLs for those environments.

## Components and Routes

The main routing logic is defined in `src/App.jsx`. Access to routes is primarily determined by whether a user is authenticated.

### Public Routes (No Login Required)
These routes are accessible when no user is logged in.

#### `/login`
- **Component:** `LoginScreen`
- **Access:** Public (Available when no user is logged in)

#### `/change-password`
- **Component:** `ChangePasswordForm`
- **Access:** Public (Based on `App.jsx` structure, accessible when no user is logged in. Also accessible when logged in. This might be intended for scenarios like first-time password change or if the user is already logged in and wants to change their password from a direct link/bookmark).

#### `/forgot-password`
- **Component:** `ForgotPasswordScreen`
- **Access:** Public (Available when no user is logged in)

#### `/reset-password/:token`
- **Component:** `ResetPasswordConfirmForm`
- **Access:** Public (Available when no user is logged in, requires a valid token in the path)

#### `*` (Any other unmatched public path)
- **Component:** `Navigate to /login`
- **Access:** Public (Redirects to `/login`)

### Authenticated Routes (Login Required)
These routes are accessible only after a user has successfully logged in.

#### `/`
- **Component:** `Dashboard`
- **Access:** Authenticated

#### `/admin/users`
- **Component:** `UserList`
- **Access:** Authenticated (Typically Admin-only, though `App.jsx` doesn't show specific role checks; these are likely enforced by API calls or within the component itself)

#### `/admin/users/new`
- **Component:** `UserForm`
- **Access:** Authenticated (Typically Admin-only, for creating new users)

#### `/admin/users/:userId/edit`
- **Component:** `UserForm`
- **Access:** Authenticated (Typically Admin-only, for editing existing users)

#### `/survey-creator/:surveyId?`
- **Component:** `SurveyJsCreatorComponent`
- **Access:** Authenticated (The `?` indicates `surveyId` is optional, likely for creating new or editing existing surveys)

#### `/survey-list`
- **Component:** `SurveyList`
- **Access:** Authenticated

#### `/profile`
- **Component:** `UserProfilePage`
- **Access:** Authenticated

#### `/change-password` (Authenticated context)
- **Component:** `ChangePasswordForm`
- **Access:** Authenticated (Available for logged-in users to change their current password)

#### `/login` (Authenticated context)
- **Component:** `Navigate to /`
- **Access:** Authenticated (Redirects to Dashboard if user is already logged in)

#### `/forgot-password` (Authenticated context)
- **Component:** `Navigate to /`
- **Access:** Authenticated (Redirects to Dashboard if user is already logged in)

#### `/reset-password/:token` (Authenticated context)
- **Component:** `Navigate to /`
- **Access:** Authenticated (Redirects to Dashboard if user is already logged in)

#### `*` (Any other unmatched authenticated path)
- **Component:** `Navigate to /`
- **Access:** Authenticated (Redirects to Dashboard)
