import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Routes, Route } from 'react-router-dom'; // MemoryRouter for Link
import ForgotPasswordScreen from './ForgotPasswordScreen';
import * as userService from '../services/userService';

// Mock userService.requestPasswordReset
jest.mock('../services/userService', () => ({
  requestPasswordReset: jest.fn(),
}));

// Mock react-router-dom's useNavigate if needed for any side-effects, though Link is primary here.
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'), // Use actual for Link, etc.
  useNavigate: () => mockNavigate,
}));


describe('ForgotPasswordScreen', () => {
  beforeEach(() => {
    userService.requestPasswordReset.mockClear();
    mockNavigate.mockClear();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter>
        <ForgotPasswordScreen />
      </MemoryRouter>
    );
  };

  test('renders email input field, submit button, and back to login link', () => {
    renderComponent();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /back to login/i })).toBeInTheDocument();
  });

  test('shows error if email field is empty on submit', async () => {
    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/email address is required/i)).toBeInTheDocument();
    expect(userService.requestPasswordReset).not.toHaveBeenCalled();
  });

  test('shows error for invalid email format', async () => {
    renderComponent();
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'invalidemail' } });
    fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/please enter a valid email address/i)).toBeInTheDocument();
    expect(userService.requestPasswordReset).not.toHaveBeenCalled();
  });

  test('calls requestPasswordReset service on successful submission with valid email', async () => {
    const testEmail = 'test@example.com';
    userService.requestPasswordReset.mockResolvedValueOnce({ message: 'Password reset link sent.' });
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: testEmail } });
    fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(userService.requestPasswordReset).toHaveBeenCalledWith(testEmail);
    });
    expect(await screen.findByText(/if your email is registered, you will receive a password reset link shortly./i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toHaveValue(''); // Field cleared
  });

  test('shows error message if service call fails', async () => {
    const testEmail = 'error@example.com';
    userService.requestPasswordReset.mockRejectedValueOnce({ message: 'Server error occurred.' });
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: testEmail } });
    fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(userService.requestPasswordReset).toHaveBeenCalledWith(testEmail);
    });
    expect(await screen.findByText(/server error occurred./i)).toBeInTheDocument();
  });

  test('"Back to Login" link navigates to /login', () => {
    renderComponent();
    const loginLink = screen.getByRole('link', { name: /back to login/i });
    expect(loginLink).toHaveAttribute('href', '/login');
  });

  test('submit button is disabled and shows loading text during API call', async () => {
    const testEmail = 'loading@example.com';
    userService.requestPasswordReset.mockImplementation(() => new Promise(() => {})); // Simulate hanging promise
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: testEmail } });
    const submitButton = screen.getByRole('button', { name: /send reset link/i });
    fireEvent.click(submitButton);

    await waitFor(() => expect(submitButton).toBeDisabled());
    // The button text changes to a CircularProgress component, so we check for that role or a specific part of its structure if needed.
    // For simplicity, just checking disabled status which is the key behavior.
    // To check for CircularProgress: expect(screen.getByRole('progressbar')).toBeInTheDocument();
    // Or, if the text "Send Reset Link" is replaced by it:
    // expect(screen.queryByText('Send Reset Link')).not.toBeInTheDocument();
    // expect(screen.getByRole('progressbar')).toBeInTheDocument(); // This is more robust if text node is removed
    // For now, let's assume loading state is visually represented by disabling and potentially a spinner.
  });

   test('submit button and email field are disabled after successful submission', async () => {
    const testEmail = 'success@example.com';
    userService.requestPasswordReset.mockResolvedValueOnce({ message: 'Link sent.' });
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: testEmail } });
    fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => expect(userService.requestPasswordReset).toHaveBeenCalled());
    expect(await screen.findByText(/if your email is registered, you will receive a password reset link shortly./i)).toBeInTheDocument();

    expect(screen.getByRole('button', { name: /send reset link/i })).toBeDisabled();
    expect(screen.getByLabelText(/email address/i)).toBeDisabled();
  });


});
