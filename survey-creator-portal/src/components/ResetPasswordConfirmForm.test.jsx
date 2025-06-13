import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ResetPasswordConfirmForm from './ResetPasswordConfirmForm';
import * as userService from '../services/userService';

// Mock react-router-dom hooks and services
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'), // use actual for all non-hook parts
  useParams: jest.fn(),
  useNavigate: () => mockNavigate,
}));

jest.mock('../services/userService', () => ({
  confirmPasswordReset: jest.fn(),
}));

const mockToken = 'test-reset-token';

describe('ResetPasswordConfirmForm', () => {
  beforeEach(() => {
    // Reset mocks and params before each test
    userService.confirmPasswordReset.mockClear();
    mockNavigate.mockClear();
    // Setup useParams to return the mock token for each test
    require('react-router-dom').useParams.mockReturnValue({ token: mockToken });
  });

  const renderComponent = () => {
    // Render within MemoryRouter to allow a path that includes the token
    return render(
      <MemoryRouter initialEntries={[`/reset-password/${mockToken}`]}>
        <Routes>
          <Route path="/reset-password/:token" element={<ResetPasswordConfirmForm />} />
        </Routes>
      </MemoryRouter>
    );
  };

  test('renders all input fields and submit button', () => {
    renderComponent();
    expect(screen.getByLabelText(/new password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm new password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /set new password/i })).toBeInTheDocument();
  });

  test('shows error if token is missing (simulated by useParams returning undefined)', () => {
    require('react-router-dom').useParams.mockReturnValue({ token: undefined });
    render( // Render without initialEntries to avoid token in path, relying on useParams mock
        <MemoryRouter>
            <ResetPasswordConfirmForm />
        </MemoryRouter>
    );
    expect(screen.getByText(/password reset token is missing/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /set new password/i })).toBeDisabled();
  });


  // Password Policy Checklist Tests (similar to ChangePasswordForm)
  describe('Password Policy Checklist', () => {
    test('checklist is not visible when new password field is empty', () => {
      renderComponent();
      expect(screen.queryByText(/password policy:/i)).not.toBeInTheDocument();
    });

    test('checklist becomes visible when typing in new password field', () => {
      renderComponent();
      const newPasswordInput = screen.getByLabelText(/new password/i);
      fireEvent.change(newPasswordInput, { target: { value: 'T' } });
      expect(screen.getByText(/password policy:/i)).toBeInTheDocument();
      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
    });

    const policyTestCases = [
      { rule: /at least 8 characters/i, password: 'short', met: false },
      { rule: /at least 8 characters/i, password: 'LongEnough', met: true },
      { rule: /at least one uppercase letter/i, password: 'noupper1@', met: false },
      { rule: /at least one uppercase letter/i, password: 'WithUpper1@', met: true },
      // Add more cases if desired, these cover the mechanism
    ];

    policyTestCases.forEach(({ rule, password, met }) => {
      test(`rule "${rule}" is ${met ? 'met' : 'not met'} for password "${password}"`, () => {
        renderComponent();
        const newPasswordInput = screen.getByLabelText(/new password/i);
        fireEvent.change(newPasswordInput, { target: { value: password } });
        const ruleElement = screen.getByText(rule);
        expect(ruleElement).toBeInTheDocument();
        expect(ruleElement).toHaveStyle(met ? 'color: success.main' : 'color: error.main');
      });
    });
  });

  // Submit Button Disabling Tests
  describe('Submit Button Disabling', () => {
    test('submit button is disabled initially', () => {
      renderComponent();
      expect(screen.getByRole('button', { name: /set new password/i })).toBeDisabled();
    });

    test('submit button is disabled if policy is not met', () => {
      renderComponent();
      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'short' } });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'short' } });
      expect(screen.getByRole('button', { name: /set new password/i })).toBeDisabled();
    });

    test('submit button is disabled if passwords do not match', () => {
      renderComponent();
      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidP@ss2' } }); // Mismatch
      expect(screen.getByRole('button', { name: /set new password/i })).toBeDisabled();
    });

    test('submit button is enabled when all conditions are met', () => {
      renderComponent();
      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidP@ss1' } });
      expect(screen.getByRole('button', { name: /set new password/i })).toBeEnabled();
    });
  });

  // API Call and Message Handling Tests
  describe('API Call and Message Handling', () => {
    test('calls confirmPasswordReset service on successful submission and navigates', async () => {
      userService.confirmPasswordReset.mockResolvedValueOnce({ message: 'Password reset successfully!' });
      renderComponent();

      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.click(screen.getByRole('button', { name: /set new password/i }));

      await waitFor(() => {
        expect(userService.confirmPasswordReset).toHaveBeenCalledWith(mockToken, 'ValidP@ss1');
      });
      expect(await screen.findByText(/password has been reset successfully/i)).toBeInTheDocument();
      // Check for navigation after timeout
      await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/login'), { timeout: 3500 });
    });

    test('shows error message if service call fails', async () => {
      userService.confirmPasswordReset.mockRejectedValueOnce({ message: 'Token invalid or expired.' });
      renderComponent();

      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.click(screen.getByRole('button', { name: /set new password/i }));

      await waitFor(() => {
        expect(userService.confirmPasswordReset).toHaveBeenCalledTimes(1);
      });
      expect(await screen.findByText(/token might be invalid or expired/i)).toBeInTheDocument();
      expect(mockNavigate).not.toHaveBeenCalled();
    });

    test('shows error if passwords do not match on submit (safety net)', async () => {
        renderComponent();
        fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } });
        fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'WrongConfirm' } });
        fireEvent.click(screen.getByRole('button', { name: /set new password/i }));
        expect(await screen.findByText(/new passwords do not match/i)).toBeInTheDocument();
        expect(userService.confirmPasswordReset).not.toHaveBeenCalled();
    });

    test('shows error if password policy not met on submit (safety net)', async () => {
        renderComponent();
        fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'weak' } });
        fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'weak' } });
        fireEvent.click(screen.getByRole('button', { name: /set new password/i }));
        expect(await screen.findByText(/Password does not meet all policy requirements./i)).toBeInTheDocument();
        expect(userService.confirmPasswordReset).not.toHaveBeenCalled();
    });
  });
});
