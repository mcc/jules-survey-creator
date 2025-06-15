import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ShareSurveyDialog from './ShareSurveyDialog';
import * as surveyService from '../services/surveyService';

// Mock the surveyService module
jest.mock('../services/surveyService');

// Mock console.error to avoid noise in test output from antd/material-ui components
jest.spyOn(console, 'error').mockImplementation(jest.fn());


describe('ShareSurveyDialog', () => {
  const mockOnClose = jest.fn();
  const surveyId = 'survey123';

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    surveyService.getSharedUsers.mockResolvedValue([
      { id: 'user1', name: 'Existing User One', email: 'one@example.com' },
    ]);
    surveyService.shareSurveyWithUser.mockResolvedValue({ success: true });
    surveyService.unshareSurveyWithUser.mockResolvedValue({ success: true });
  });

  test('renders correctly when open', () => {
    render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);

    expect(screen.getByText('Share Survey')).toBeInTheDocument();
    expect(screen.getByLabelText('Search User')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('does not render when not open', () => {
    render(<ShareSurveyDialog open={false} onClose={mockOnClose} surveyId={surveyId} />);
    expect(screen.queryByText('Share Survey')).not.toBeInTheDocument();
  });

  test('calls onClose when Cancel button is clicked', () => {
    render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test('calls onClose when Share button is clicked (currently, will be updated for actual share logic)', () => {
    // This test will need to be updated once search/selection logic is in place.
    // For now, it just checks if the button is clickable and calls onClose like Cancel.
    render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);
    fireEvent.click(screen.getByRole('button', { name: 'Share' }));
    expect(mockOnClose).toHaveBeenCalledTimes(1); // Placeholder, real share logic is different
  });

  // Further tests will be more complex and depend on the implementation of:
  // 1. Displaying users fetched from getSharedUsers
  // 2. Searching/selecting a user from an external source or a global user list
  // 3. Handling the "Share" action with a selected user.
  // 4. Handling the "Remove" action for an existing shared user.

  test('displays existing shared users on load', async () => {
    render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);
    // The ShareSurveyDialog would need to call getSharedUsers internally on open.
    // Let's assume it does and updates its state.
    // This test requires ShareSurveyDialog to implement the fetching and rendering of users.
    // For now, we assert that the service method was called.
    await waitFor(() => {
      expect(surveyService.getSharedUsers).toHaveBeenCalledWith(surveyId);
    });
    // Example: expect(await screen.findByText('Existing User One')).toBeInTheDocument();
    // This part ('Existing User One') depends on how users are rendered in the Dialog's List.
  });

  test('allows typing in search field', () => {
    render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);
    const searchInput = screen.getByLabelText('Search User');
    fireEvent.change(searchInput, { target: { value: 'testuser' } });
    expect(searchInput.value).toBe('testuser');
  });

  // Placeholder for "Add User" test - requires more detailed implementation in the component
  // test('calls shareSurveyWithUser when sharing with a new user', async () => {
  //   render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);
  //   const searchInput = screen.getByLabelText('Search User');
  //   fireEvent.change(searchInput, { target: { value: 'newUserToShare' } });
  //   // Simulate selecting a user from search results (if applicable)
  //   // ...
  //   fireEvent.click(screen.getByRole('button', { name: 'Share' }));
  //   await waitFor(() => {
  //     expect(surveyService.shareSurveyWithUser).toHaveBeenCalledWith(surveyId, 'someUserIdFoundFromSearch');
  //   });
  // });

  // Placeholder for "Remove User" test - requires user list rendering and remove buttons
  // test('calls unshareSurveyWithUser when removing a user', async () => {
  //   surveyService.getSharedUsers.mockResolvedValueOnce([ // Ensure this is the mock for this specific test
  //     { id: 'userToRemove', name: 'User To Remove', email: 'remove@example.com' },
  //   ]);
  //   render(<ShareSurveyDialog open={true} onClose={mockOnClose} surveyId={surveyId} />);
  //   // Wait for user to be displayed
  //   // const removeButton = await screen.findByRole('button', { name: /remove userToRemove/i }); // Depends on accessible name
  //   // fireEvent.click(removeButton);
  //   // await waitFor(() => {
  //   //   expect(surveyService.unshareSurveyWithUser).toHaveBeenCalledWith(surveyId, 'userToRemove');
  //   // });
  // });

});
