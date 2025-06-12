import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event'; // For more realistic user interactions
import LoginScreen from './LoginScreen';
// Correctly import AuthContext (the context object) for the MockAuthProvider
import { AuthProvider, useAuth, AuthContext as ActualAuthContext } from '../contexts/AuthContext';
import { MemoryRouter, Routes, Route } from 'react-router-dom'; // For routing context

// Mock react-router-dom's useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock AuthContext login function specifically for some tests
const mockLogin = vi.fn();

const MockAuthProvider = ({ children, loginFn = mockLogin }) => {
  // This simplified mock provider will be used when we need to control `login` behavior.
  // It should not call useAuth() itself, as it's providing the context.
  const contextValue = {
    user: null,
    login: loginFn,
    logout: vi.fn(),
    refreshToken: vi.fn(),
    loading: false,
    // Add any other properties that components consuming the context might expect,
    // even if they are just dummy values for these specific tests.
    // For example, if a component destructures 'setUser':
    // setUser: vi.fn()
  };
  return (
    <ActualAuthContext.Provider value={contextValue}>
      {children}
    </ActualAuthContext.Provider>
  );
};


describe('LoginScreen', () => {
  beforeEach(() => {
    vi.clearAllMocks(); // Clear mocks before each test
    localStorage.clear(); // Clear local storage
  });

  const renderWithRouter = (ui, { route = '/' } = {}) => {
    window.history.pushState({}, 'Test page', route);
    return render(
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/login" element={ui} />
          <Route path="/dashboard" element={<div>Dashboard Page</div>} />
          {/* Add other routes as needed for testing navigation */}
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders correctly with email and password fields', () => {
    renderWithRouter(
      <AuthProvider> {/* Use real AuthProvider to ensure context is properly set up initially */}
        <LoginScreen />
      </AuthProvider>,
      { route: '/login'}
    );

    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument();
  });

  it('allows user to type into email and password fields', async () => {
    const user = userEvent.setup();
    renderWithRouter(
      <AuthProvider>
        <LoginScreen />
      </AuthProvider>,
      { route: '/login'}
    );

    const usernameInput = screen.getByLabelText(/username/i);
    const passwordInput = screen.getByLabelText(/password/i);

    await user.type(usernameInput, 'test@example.com');
    expect(usernameInput.value).toBe('test@example.com');

    await user.type(passwordInput, 'password123');
    expect(passwordInput.value).toBe('password123');
  });

  it('calls login function from AuthContext on submit and redirects on success', async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValueOnce(); // Simulate successful login

    renderWithRouter(
      // Use MockAuthProvider to provide our mockLogin
      <MockAuthProvider loginFn={mockLogin}>
        <LoginScreen />
      </MockAuthProvider>,
      { route: '/login'}
    );

    await user.type(screen.getByLabelText(/username/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledTimes(1);
      expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password');
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('displays error message on failed login', async () => {
    const user = userEvent.setup();
    // Simulate login failure by having mockLogin throw an error
    const errorMessage = 'Invalid credentials, please try again.';
    mockLogin.mockRejectedValueOnce({
      response: { data: { message: errorMessage } }
    });

    renderWithRouter(
      <MockAuthProvider loginFn={mockLogin}>
        <LoginScreen />
      </MockAuthProvider>,
      { route: '/login'}
    );

    await user.type(screen.getByLabelText(/username/i), 'wrong@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledTimes(1);
    });

    // Check for error message
    expect(await screen.findByText(errorMessage)).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled(); // Should not navigate on error
  });

  it('displays a generic error message if API error format is unexpected', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValueOnce(new Error("Network Error")); // Simulate generic error

    renderWithRouter(
      <MockAuthProvider loginFn={mockLogin}>
        <LoginScreen />
      </MockAuthProvider>,
      { route: '/login'}
    );

    await user.type(screen.getByLabelText(/username/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledTimes(1);
    });

    expect(await screen.findByText('Network Error')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

   it('clears previous error messages on new submit attempt', async () => {
    const user = userEvent.setup();
    const firstErrorMessage = 'First attempt failed.';
    mockLogin.mockRejectedValueOnce({ response: { data: { message: firstErrorMessage } } });

    renderWithRouter(
      <MockAuthProvider loginFn={mockLogin}>
        <LoginScreen />
      </MockAuthProvider>,
      { route: '/login'}
    );

    // First attempt (fails)
    await user.type(screen.getByLabelText(/username/i), 'user@example.com');
    await user.type(screen.getByLabelText(/password/i), 'pass');
    await user.click(screen.getByRole('button', { name: /login/i }));

    expect(await screen.findByText(firstErrorMessage)).toBeInTheDocument();

    // Second attempt (let's say it succeeds this time)
    mockLogin.mockResolvedValueOnce(); // Reset mock for successful login
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
       expect(screen.queryByText(firstErrorMessage)).not.toBeInTheDocument();
    });
    await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

});
