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
