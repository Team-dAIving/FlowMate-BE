# FlowMate Backend

## Local database configuration

Set the following environment variables in the IntelliJ run configuration before starting the application:

```text
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=flowmate
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-password
GITHUB_CLIENT_ID=your-github-oauth-client-id
GITHUB_CLIENT_SECRET=your-github-oauth-client-secret
JWT_SECRET=use-a-random-secret-at-least-32-bytes-long
```

For local development, Hibernate creates or updates the schema by default. Set `JPA_DDL_AUTO=validate` when the schema is managed separately.

Tests use an in-memory H2 database in PostgreSQL compatibility mode, so they do not require a running PostgreSQL instance.

## GitHub OAuth login

Register the following authorization callback URL in the GitHub OAuth App for local development:

```text
http://localhost:8080/login/oauth2/code/github
```

Start login by navigating the browser to:

```text
http://localhost:8080/oauth2/authorization/github
```

After GitHub authentication, the backend upserts the user by the immutable GitHub user ID,
creates its own JWT, and redirects to `http://localhost:5173/oauth/callback` by default.
The redirect includes the JWT and onboarding state as query parameters:

```text
http://localhost:5173/oauth/callback?accessToken=<JWT>&onboardingRequired=true
```

For a packaged Electron app, register a custom protocol and set the redirect URI to that
protocol, for example `flowmate://oauth/callback`.

The Electron app should read `accessToken` from the callback, keep it in its token provider,
and send it in the `Authorization` header on every authenticated API request. For example:

```javascript
fetch("http://localhost:8080/api/auth/me", {
  headers: {
    Authorization: `Bearer ${accessToken}`,
  },
});
```

Logging out is client-side: discard the stored access token. `POST /api/auth/logout` is kept
as a no-op endpoint and returns `204 No Content`.

If `onboardingRequired` is `true`, collect a service nickname and send:

```http
PATCH /api/auth/me/nickname
Content-Type: application/json

{"nickname":"flowmate-user"}
```

Optional environment variables:

```text
FRONTEND_ORIGIN=http://localhost:5173
FRONTEND_OAUTH_REDIRECT_URI=http://localhost:5173/oauth/callback
JWT_ACCESS_TOKEN_TTL=PT1H
```

The `github_user_id` column should have a unique constraint (or unique index) so concurrent
first logins cannot create duplicate accounts.
