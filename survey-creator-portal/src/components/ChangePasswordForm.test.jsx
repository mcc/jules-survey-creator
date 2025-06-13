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
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'newPass' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'newPass' } });
    fireEvent.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(userService.changeCurrentUserPassword).toHaveBeenCalledWith({
        oldPassword: 'oldPass',
        newPassword: 'newPass',
      });
    });
    expect(await screen.findByText(/password changed successfully!/i)).toBeInTheDocument();
  });

  test('shows error message if service call fails', async () => {
    userService.changeCurrentUserPassword.mockRejectedValueOnce({ message: 'Invalid old password.' });
    render(<ChangePasswordForm />);

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'newPass' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'newPass' } });
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
    fireEvent.change(newPasswordInput, { target: { value: 'newPass' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'newPass' } });
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
    fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: 'newPass' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'newPass' } });

    const submitButton = screen.getByRole('button', { name: /change password/i });
    fireEvent.click(submitButton);

    await waitFor(() => expect(submitButton).toBeDisabled());
    expect(submitButton).toHaveTextContent('Changing...');

    // Clean up the hanging promise if possible or ensure tests don't leak state.
    // For this simple case, it might be okay, but for more complex scenarios,
    // ensure promises resolve/reject to prevent test timeouts or interference.
    // Since jest.mock is used, the mock will be cleared by beforeEach.
  });

});
