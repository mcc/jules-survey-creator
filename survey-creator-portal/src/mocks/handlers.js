import { http, HttpResponse } from 'msw';

const baseURL = '/api/auth';

// Helper to create JWT-like tokens for testing
const createMockJwt = (payload, expiresInMs = 3600 * 1000) => {
  const header = { alg: 'HS256', typ: 'JWT' };
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify({ ...payload, iat: Date.now(), exp: Date.now() + expiresInMs }));
  return `${encodedHeader}.${encodedPayload}.mockSignature`;
};

export const handlers = [
  // Login Handler
  http.post(`${baseURL}/login`, async ({ request }) => {
    const { username, password } = await request.json(); // Changed email to username

    if (username === 'test@example.com' && password === 'password') {
      const mockAccessToken = createMockJwt({ sub: username, roles: ['USER'] }, 5 * 60 * 1000);
      const mockRefreshToken = createMockJwt({ sub: username, type: 'REFRESH' }, 7 * 24 * 60 * 60 * 1000);
      return HttpResponse.json({
        accessToken: mockAccessToken, // Changed token to accessToken
        refreshToken: mockRefreshToken,
        tokenType: 'Bearer',
      });
    } else if (username === 'expired@example.com' && password === 'password') {
        // For testing token expiry and refresh
        const mockAccessToken = createMockJwt({ sub: username, roles: ['USER'] }, -5 * 60 * 1000); // Expired 5 mins ago
        const mockRefreshToken = createMockJwt({ sub: username, type: 'REFRESH' }, 7 * 24 * 60 * 60 * 1000);
        return HttpResponse.json({
            accessToken: mockAccessToken, // Changed token to accessToken
            refreshToken: mockRefreshToken,
            tokenType: 'Bearer',
        });
    }
    return new HttpResponse(JSON.stringify({ message: 'Invalid credentials' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' },
    });
  }),

  // Logout Handler
  http.post(`${baseURL}/logout`, async ({ request }) => {
    const { refreshToken } = await request.json();
    if (refreshToken) {
      // In a real scenario, the backend might invalidate the refresh token.
      // For mock, we just acknowledge.
      return HttpResponse.json({ message: 'Logout successful' }, { status: 200 });
    }
    return new HttpResponse(JSON.stringify({ message: 'Refresh token required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    });
  }),

  // Refresh Token Handler
  http.post(`${baseURL}/refresh`, async ({ request }) => {
    const { refreshToken } = await request.json();

    if (!refreshToken) {
      return new HttpResponse(JSON.stringify({ message: 'Refresh token missing' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    // Simulate validating refresh token. For this mock, any non-empty token is "valid"
    // unless it's explicitly marked as "invalid-refresh-token" for testing failure.
    if (refreshToken === 'invalid-refresh-token') {
        return new HttpResponse(JSON.stringify({ message: 'Invalid refresh token' }), {
            status: 401, // Or 403
            headers: { 'Content-Type': 'application/json' },
          });
    }

    try {
        // "Decode" the mock refresh token to get the subject (username)
        const payloadPart = refreshToken.split('.')[1];
        const decodedPayload = JSON.parse(atob(payloadPart));
        const username = decodedPayload.sub; // Changed email to username

        if (!username) { // Changed email to username
            throw new Error("Invalid mock refresh token payload");
        }

        const newAccessToken = createMockJwt({ sub: username, roles: ['USER'] }, 5 * 60 * 1000);
        // Optionally, issue a new refresh token, though current AuthContext doesn't expect 'newMockRefreshToken' from this specific key
        const newMockRefreshToken = createMockJwt({ sub: username, type: 'REFRESH' }, 7 * 24 * 60 * 60 * 1000);


        return HttpResponse.json({
            token: newAccessToken, // This is the key AuthContext expects for the new access token
            refreshToken: newMockRefreshToken, // Include if backend sends it and AuthContext should handle it
            tokenType: 'Bearer',
        });

    } catch(e) {
        console.error("Mock refresh error processing:", e);
        return new HttpResponse(JSON.stringify({ message: 'Invalid or expired refresh token' }), {
            status: 401,
            headers: { 'Content-Type': 'application/json' },
          });
    }
  }),

  // Example of a protected route that might be called by other parts of the app
  http.get(`/api/data`, async ({request}) => {
    const authorization = request.headers.get('Authorization');
    if (authorization && authorization.startsWith('Bearer ')) {
      const token = authorization.substring(7);
      try {
        const payloadPart = token.split('.')[1];
        const decodedPayload = JSON.parse(atob(payloadPart));
        if (decodedPayload.exp < Date.now()) {
          return new HttpResponse(JSON.stringify({ message: 'Token expired' }), { status: 401 });
        }
        return HttpResponse.json({ message: 'This is protected data for ' + decodedPayload.sub });
      } catch (e) {
        return new HttpResponse(JSON.stringify({ message: 'Invalid token' }), { status: 401 });
      }
    }
    return new HttpResponse(JSON.stringify({ message: 'Authorization header missing' }), { status: 401 });
  }),
];

// Helper function to simulate atob and btoa for Node.js environment if not available (Vitest usually runs in Node)
if (typeof btoa === 'undefined') {
  global.btoa = (str) => Buffer.from(str, 'binary').toString('base64');
}
if (typeof atob === 'undefined') {
  global.atob = (b64Encoded) => Buffer.from(b64Encoded, 'base64').toString('binary');
}
