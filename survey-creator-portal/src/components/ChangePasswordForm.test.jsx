import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom'; // For extended matchers like .toBeInTheDocument()
import ChangePasswordForm from './ChangePasswordForm';
import * as userService from '../services/userService'; // To mock the service

// Mock the userService.changeCurrentUserPassword function
jest.mock('../services/userService', () => ({
  changeCurrentUserPassword: jest.fn(),
}));

describe('ChangePasswordForm', () => {
  beforeEach(() => {
    // Reset mocks before each test
    userService.changeCurrentUserPassword.mockClear();
  });

  test('renders all input fields and submit button', () => {
    render(<ChangePasswordForm />);
    expect(screen.getByLabelText(/current password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/new password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm new password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /change password/i })).toBeInTheDocument();
  });

  test('shows error if fields are empty on submit', async () => {
    render(<ChangePasswordForm />);
    fireEvent.click(screen.getByRole('button', { name: /change password/i }));

    expect(await screen.findByText(/all fields are required/i)).toBeInTheDocument();
    expect(userService.changeCurrentUserPassword).not.toHaveBeenCalled();
  });

  test('shows error if new passwords do not match', async () => {
    render(<ChangePasswordForm />);
    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'newPass1' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'newPass2' } });
    fireEvent.click(screen.getByRole('button', { name: /change password/i }));

    expect(await screen.findByText(/new passwords do not match/i)).toBeInTheDocument();
    expect(userService.changeCurrentUserPassword).not.toHaveBeenCalled();
  });

  test('calls changeCurrentUserPassword service on successful submission', async () => {
    userService.changeCurrentUserPassword.mockResolvedValueOnce({ message: 'Password changed successfully!' });
    render(<ChangePasswordForm />);

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(userService.changeCurrentUserPassword).toHaveBeenCalledWith({
        oldPassword: 'oldPass',
        newPassword: 'ValidNewP@ss1',
      });
    });
    expect(await screen.findByText(/password changed successfully!/i)).toBeInTheDocument();
  });

  test('shows error message if service call fails', async () => {
    userService.changeCurrentUserPassword.mockRejectedValueOnce({ message: 'Invalid old password.' });
    render(<ChangePasswordForm />);

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(userService.changeCurrentUserPassword).toHaveBeenCalledTimes(1);
    });
    expect(await screen.findByText(/invalid old password./i)).toBeInTheDocument();
  });

  test('form fields are cleared and button re-enabled after successful submission', async () => {
    userService.changeCurrentUserPassword.mockResolvedValueOnce({ message: 'Password changed successfully!' });
    render(<ChangePasswordForm />);

    const currentPasswordInput = screen.getByLabelText(/current password/i);
    const newPasswordInput = screen.getByLabelText(/new password/i);
    const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
    const submitButton = screen.getByRole('button', { name: /change password/i });

    fireEvent.change(currentPasswordInput, { target: { value: 'oldPass' } });
    fireEvent.change(newPasswordInput, { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.change(confirmPasswordInput, { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.click(submitButton);

    await waitFor(() => expect(userService.changeCurrentUserPassword).toHaveBeenCalled());

    expect(await screen.findByText(/password changed successfully!/i)).toBeInTheDocument();
    expect(currentPasswordInput.value).toBe('');
    expect(newPasswordInput.value).toBe('');
    expect(confirmPasswordInput.value).toBe('');
    expect(submitButton).not.toBeDisabled();
    expect(submitButton).toHaveTextContent('Change Password');
  });

  test('submit button is disabled and shows "Changing..." during API call', async () => {
    // Make the promise hang so we can check the loading state
    userService.changeCurrentUserPassword.mockImplementation(() => new Promise(() => {}));
    render(<ChangePasswordForm />);

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidNewP@ss1' } }); // Policy compliant
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidNewP@ss1' } }); // Policy compliant

    const submitButton = screen.getByRole('button', { name: /change password/i });
    fireEvent.click(submitButton);

    await waitFor(() => expect(submitButton).toBeDisabled());
    expect(submitButton).toHaveTextContent('Changing...');

    // Clean up the hanging promise if possible or ensure tests don't leak state.
    // For this simple case, it might be okay, but for more complex scenarios,
    // ensure promises resolve/reject to prevent test timeouts or interference.
    // Since jest.mock is used, the mock will be cleared by beforeEach.
  });

  // New tests for password policy checklist
  describe('Password Policy Checklist', () => {
    test('checklist is not visible when new password field is empty', () => {
      render(<ChangePasswordForm />);
      expect(screen.queryByText(/password policy:/i)).not.toBeInTheDocument();
    });

    test('checklist becomes visible when typing in new password field', () => {
      render(<ChangePasswordForm />);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      fireEvent.change(newPasswordInput, { target: { value: 'T' } });
      expect(screen.getByText(/password policy:/i)).toBeInTheDocument();
      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
    });

    const testCases = [
      { rule: /at least 8 characters/i, password: 'short', met: false },
      { rule: /at least 8 characters/i, password: 'LongEnough', met: true },
      { rule: /at least one uppercase letter/i, password: 'noupper1@', met: false },
      { rule: /at least one uppercase letter/i, password: 'WithUpper1@', met: true },
      { rule: /at least one lowercase letter/i, password: 'NOLOWER1@', met: false },
      { rule: /at least one lowercase letter/i, password: 'WithLower1@', met: true },
      { rule: /at least one number/i, password: 'NoNumber@@', met: false },
      { rule: /at least one number/i, password: 'WithNumber1@', met: true },
      { rule: /at least one special character/i, password: 'NoSpecial1Aa', met: false },
      { rule: /at least one special character/i, password: 'WithSpecial@1A', met: true },
    ];

    testCases.forEach(({ rule, password, met }) => {
      test(`rule "${rule}" is ${met ? 'met' : 'not met'} for password "${password}"`, () => {
        render(<ChangePasswordForm />);
        const newPasswordInput = screen.getByLabelText(/new password/i);
        fireEvent.change(newPasswordInput, { target: { value: password } });

        const ruleElement = screen.getByText(rule);
        expect(ruleElement).toBeInTheDocument();
        // Check icon by its color/class or test-id if more robust selection is needed
        // For simplicity, checking the color of the text associated with the icon
        if (met) {
          expect(ruleElement).toHaveStyle('color: success.main'); // Material UI theme color
        } else {
          expect(ruleElement).toHaveStyle('color: error.main'); // Material UI theme color
        }
      });
    });

    test('all rules are met for a valid password', () => {
        render(<ChangePasswordForm />);
        const newPasswordInput = screen.getByLabelText(/new password/i);
        fireEvent.change(newPasswordInput, { target: { value: 'ValidP@ss1' } });

        expect(screen.getByText(/at least 8 characters/i)).toHaveStyle('color: success.main');
        expect(screen.getByText(/at least one uppercase letter/i)).toHaveStyle('color: success.main');
        expect(screen.getByText(/at least one lowercase letter/i)).toHaveStyle('color: success.main');
        expect(screen.getByText(/at least one number/i)).toHaveStyle('color: success.main');
        expect(screen.getByText(/at least one special character/i)).toHaveStyle('color: success.main');
    });
  });

  describe('Submit Button Disabling based on Policy', () => {
    test('submit button is disabled initially', () => {
      render(<ChangePasswordForm />);
      expect(screen.getByRole('button', { name: /change password/i })).toBeDisabled();
    });

    test('submit button is disabled if new password does not meet policy', () => {
      render(<ChangePasswordForm />);
      fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'short' } }); // Policy not met
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'short' } });
      expect(screen.getByRole('button', { name: /change password/i })).toBeDisabled();
    });

    test('submit button is disabled if passwords do not match, even if policy is met', () => {
      render(<ChangePasswordForm />);
      fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } }); // Policy met
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidP@ss2' } }); // Mismatch
      expect(screen.getByRole('button', { name: /change password/i })).toBeDisabled();
    });

    test('submit button is enabled when all fields are valid and policy is met', () => {
      render(<ChangePasswordForm />);
      fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
      fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'ValidP@ss1' } });
      fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'ValidP@ss1' } });
      expect(screen.getByRole('button', { name: /change password/i })).toBeEnabled();
    });
  });

  test('shows error if password does not meet policy on submit (safety net)', async () => {
    // This test is a safety net for form submission if button disabling fails or form submitted via Enter key
    render(<ChangePasswordForm />);
    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPassword123' } });
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'weak' } }); // Does not meet policy
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'weak' } });

    // Enable button manually for test, or ensure state allows submission if this path is possible
    // In current setup, button would be disabled. So this tests the internal handleSubmit validation.
    // const submitButton = screen.getByRole('button', { name: /change password/i });
    // submitButton.disabled = false; // Not a good practice to manually change like this in RTL.
    // Instead, this tests the handleSubmit's internal checks.

    fireEvent.click(screen.getByRole('button', { name: /change password/i }));

    expect(await screen.findByText(/Password does not meet all policy requirements./i)).toBeInTheDocument();
    expect(userService.changeCurrentUserPassword).not.toHaveBeenCalled();
  });

});
