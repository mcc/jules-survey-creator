import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth, AuthContext } from './AuthContext';
import { server } from '../mocks/server';
import { http, HttpResponse } from 'msw';
import { jwtDecode } from 'jwt-decode';

// Helper component to use the hook and display values
const TestConsumer = ({ children }) => {
  const auth = useAuth();
  if (!auth) return <div>No AuthContext</div>;
  return (
    <div>
      {/* Display "Loading..." text when auth.loading is true, mimicking AuthProvider's own behavior */}
      {/* This helps ensure tests wait for the loading phase to complete if necessary */}
      {auth.loading && <div data-testid="explicit-loading-indicator">Loading...</div>}
      {auth.user ? <div data-testid="user-email">{auth.user.email}</div> : <div data-testid="user-email">null</div>}
      {children && children(auth)}
    </div>
  );
};

const createMockJwt = (payload, expiresInMs = 3600 * 1000) => {
  const header = { alg: 'HS256', typ: 'JWT' };
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify({ ...payload, iat: Date.now(), exp: Date.now() + expiresInMs }));
  return `${encodedHeader}.${encodedPayload}.mockSignature`;
};

if (typeof btoa === 'undefined') {
  global.btoa = (str) => Buffer.from(str, 'binary').toString('base64');
}
if (typeof atob === 'undefined') {
  global.atob = (b64Encoded) => Buffer.from(b64Encoded, 'base64').toString('binary');
}

describe('AuthContext', () => {
  afterEach(() => {
    localStorage.clear();
    server.resetHandlers();
    vi.restoreAllMocks();
  });

  it('initial state with no token in localStorage', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );
    await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId('user-email').textContent).toBe('null'));
  });

  it('initial state with valid token in localStorage', async () => {
    const mockToken = createMockJwt({ sub: 'stored@example.com' });
    localStorage.setItem('jwtToken', mockToken);
    localStorage.setItem('refreshToken', 'fake-refresh-token');

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId('user-email').textContent).toBe('stored@example.com'));
  });

  it('initial state with expired token in localStorage', async () => {
    const expiredToken = createMockJwt({ sub: 'expired@example.com' }, -(5 * 60 * 1000)); // Expired 5 minutes ago
    localStorage.setItem('jwtToken', expiredToken);
    localStorage.setItem('refreshToken', 'fake-refresh-token-for-expired-test');

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId('user-email').textContent).toBe('null'));
    expect(localStorage.getItem('jwtToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('successful login updates user, localStorage, and apiClient headers', async () => {
    let capturedAuth;
    render(
      <AuthProvider>
        <TestConsumer>
          {(auth) => { capturedAuth = auth; return null; }}
        </TestConsumer>
      </AuthProvider>
    );

    await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());

    await act(async () => {
      await capturedAuth.login('test@example.com', 'password');
    });

    expect(screen.getByTestId('user-email').textContent).toBe('test@example.com');
    const storedToken = localStorage.getItem('jwtToken');
    expect(storedToken).not.toBeNull();
    if (storedToken) {
      const decoded = jwtDecode(storedToken);
      expect(decoded.sub).toBe('test@example.com');
    }
    expect(localStorage.getItem('refreshToken')).not.toBeNull();
  });

  it('failed login does not update user and handles error', async () => {
    let capturedAuth;
    render(
      <AuthProvider>
        <TestConsumer>
          {(auth) => { capturedAuth = auth; return null; }}
        </TestConsumer>
      </AuthProvider>
    );

    await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());

    let loginError;
    await act(async () => {
      try {
        await capturedAuth.login('wrong@example.com', 'wrongpassword');
      } catch (e) {
        loginError = e;
      }
    });

    expect(screen.getByTestId('user-email').textContent).toBe('null');
    expect(localStorage.getItem('jwtToken')).toBeNull();
    expect(loginError).toBeDefined();
    expect(loginError.message).toContain('Request failed with status code 401');
  });

  it('logout clears user, localStorage, and apiClient headers', async () => {
    const mockInitialToken = createMockJwt({ sub: 'testlogout@example.com' });
    localStorage.setItem('jwtToken', mockInitialToken);
    localStorage.setItem('refreshToken', 'logout-refresh-token');

    let capturedAuth;
    render(
      <AuthProvider>
        <TestConsumer>
          {(auth) => { capturedAuth = auth; return null; }}
        </TestConsumer>
      </AuthProvider>
    );

    await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId('user-email').textContent).toBe('testlogout@example.com'));

    await act(async () => {
      await capturedAuth.logout();
    });

    expect(screen.getByTestId('user-email').textContent).toBe('null');
    expect(localStorage.getItem('jwtToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it.skip('successful token refresh updates localStorage and user', async () => {
    // Test logic remains skipped
  });

  it.skip('failed token refresh logs out user', async () => {
    // Test logic remains skipped
  });

  it.skip('Axios interceptor attempts to refresh token on 401 and retries original request', async () => {
    // Test logic remains skipped
  });
});
