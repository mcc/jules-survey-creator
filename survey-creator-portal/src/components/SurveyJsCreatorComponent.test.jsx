import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { vi } from 'vitest';
import SurveyJsCreatorComponent from './SurveyJsCreatorComponent';
import { AuthContext } from '../contexts/AuthContext';
import { SurveyCreator } from 'survey-creator-core'; // This will be mocked

// Mock react-router-dom
vi.mock('react-router-dom', async () => ({
  ...await vi.importActual('react-router-dom'), // import and retain default behavior
  useParams: vi.fn(),
}));

// Mock SurveyCreator from survey-creator-core (the one new'd up in the component)
// This mock will provide a controlled instance for our component to use.
let mockCoreCreatorInstance; // To hold the instance for tests to interact with
vi.mock('survey-creator-core', () => {
  // console.log('Mocking survey-creator-core');
  const MockSurveyCreatorCore = vi.fn().mockImplementation(() => {
    // console.log('MockSurveyCreatorCore constructor called');
    mockCoreCreatorInstance = {
      JSON: {}, // survey-creator reads/writes this
      text: '', // another property survey-creator might use
      saveSurveyFunc: null, // This is what our component assigns to
      showPreview: vi.fn(),
      // Mock event handlers that SurveyJS creator might try to attach to
      onSurveyInstanceCreated: { add: vi.fn(), remove: vi.fn() },
      onDesignerSurveyCreated: { add: vi.fn(), remove: vi.fn() },
      onModified: { add: vi.fn(), remove: vi.fn() },
      onJSONChanged: { add: vi.fn(), remove: vi.fn() }, // Common event
      onLogicPanelSurveyCreated: { add: vi.fn(), remove: vi.fn() },
      onLogicPanelGetSurvey: { add: vi.fn(), remove: vi.fn() },
      onActiveTabChanged: { add: vi.fn(), remove: vi.fn() },
      onSurveyUpdated: { add: vi.fn(), remove: vi.fn() },
      onGetQuestionTitleActions: { add: vi.fn(), remove: vi.fn() },
      onGetPageTitleActions: { add: vi.fn(), remove: vi.fn() },
      onGetObjectDisplayName: { add: vi.fn(), remove: vi.fn() },
      onQuestionAdded: { add: vi.fn(), remove: vi.fn() },
      onElementDeleting: { add: vi.fn(), remove: vi.fn() },
      onElementModified: { add: vi.fn(), remove: vi.fn() },
      // Add any other properties/methods your component or SurveyJS might expect
    };
    return mockCoreCreatorInstance;
  });
  return { SurveyCreator: MockSurveyCreatorCore };
});


// Mock survey-creator-react's SurveyCreatorComponent
// This mock prevents the actual SurveyJS UI from rendering, which is complex and not needed for these unit tests.
vi.mock('survey-creator-react', async () => {
  const actual = await vi.importActual('survey-creator-react');
  return {
    ...actual,
    SurveyCreatorComponent: ({ creator }) => {
      // This mock receives the `creator` instance from SurveyJsCreatorComponent's state.
      // We don't need to do much with it here, just provide a placeholder.
      // The actual tests will interact with `mockCoreCreatorInstance` which is what our component uses.
      return <div data-testid="survey-creator-react-mock"></div>;
    },
  };
});


// Mock props
const mockOnGetSurvey = vi.fn();
const mockOnCreateSurvey = vi.fn();
const mockOnUpdateSurvey = vi.fn();
const mockOnFetchSharedUsers = vi.fn();
const mockOnShareSurvey = vi.fn();
const mockOnUnshareSurvey = vi.fn();

const mockUser = { sub: 'testuser' };

const renderComponent = (params = {}) => {
  const { surveyIdFromParams, ...otherParams } = params;
  const { useParams } = require('react-router-dom');
  useParams.mockReturnValue({ surveyId: surveyIdFromParams });

  const props = {
    onGetSurvey: mockOnGetSurvey,
    onCreateSurvey: mockOnCreateSurvey,
    onUpdateSurvey: mockOnUpdateSurvey,
    onFetchSharedUsers: mockOnFetchSharedUsers,
    onShareSurvey: mockOnShareSurvey,
    onUnshareSurvey: mockOnUnshareSurvey,
    json: otherParams.json, // Allow passing initial JSON if needed
    options: otherParams.options, // Allow passing options if needed
  };

  return render(
    <AuthContext.Provider value={{ user: mockUser, token: 'fake-token' }}>
      <SurveyJsCreatorComponent {...props} />
    </AuthContext.Provider>
  );
};

describe('SurveyJsCreatorComponent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default mock implementations
    mockOnGetSurvey.mockResolvedValue({ id: 'survey1', surveyJson: { title: 'Existing Survey' }, ownerUsername: 'testuser', sharedWithUsers: [] });
    mockOnCreateSurvey.mockResolvedValue({ id: 'newSurvey123', surveyJson: { title: 'New Survey' }, surveyMode: 'public', dataClassification: 'public', status: 'drafted' });
    mockOnUpdateSurvey.mockResolvedValue({ id: 'survey1', surveyJson: { title: 'Updated Survey' }, surveyMode: 'public', dataClassification: 'public', status: 'drafted' });
    mockOnFetchSharedUsers.mockResolvedValue({ sharedWithUsers: [{ id: 'user2', username: 'sharedUser' }] });
    mockOnShareSurvey.mockResolvedValue({});
    mockOnUnshareSurvey.mockResolvedValue({});

    // Reset the core creator instance for each test, as it's created anew by the component
    // The SurveyCreator constructor mock ensures mockCoreCreatorInstance is set when `new SurveyCreator()` is called
    // No need to do `mockCoreCreatorInstance = new SurveyCreator()` here as the mock constructor handles it.
  });

  describe('Basic Rendering', () => {
    it('renders without crashing when no surveyIdFromParams is provided', () => {
      renderComponent();
      expect(screen.getByTestId('survey-creator-react-mock')).toBeInTheDocument();
      expect(screen.getByText('Survey ID:')).toBeInTheDocument();
      expect(screen.getByText('New Survey')).toBeInTheDocument(); // Default display for new survey
    });

    it('renders without crashing when surveyIdFromParams is provided', async () => {
      renderComponent({ surveyIdFromParams: 'survey1' });
      await waitFor(() => {
        expect(screen.getByTestId('survey-creator-react-mock')).toBeInTheDocument();
      });
      expect(screen.getByText('Survey ID:')).toBeInTheDocument();
    });
  });

  describe('onGetSurvey Interaction', () => {
    it('calls onGetSurvey with surveyIdFromParams on mount if surveyId is present', async () => {
      renderComponent({ surveyIdFromParams: 'survey1' });
      await waitFor(() => {
        expect(mockOnGetSurvey).toHaveBeenCalledWith('survey1');
      });
    });

    it('loads survey JSON from onGetSurvey response', async () => {
      const surveyData = { id: 'survey1', surveyJson: { elements: [{ name: "q1" }] }, ownerUsername: 'testuser' };
      mockOnGetSurvey.mockResolvedValue(surveyData);
      renderComponent({ surveyIdFromParams: 'survey1' });

      await waitFor(() => {
        expect(mockOnGetSurvey).toHaveBeenCalled();
      });
      // Check if the creator's JSON was updated.
      // mockCoreCreatorInstance is the instance created by `new SurveyCreator()` inside the component.
      expect(mockCoreCreatorInstance.JSON).toEqual(surveyData.surveyJson);
    });

    it('handles onGetSurvey failure and sets default JSON', async () => {
        mockOnGetSurvey.mockRejectedValue(new Error('Failed to fetch'));
        renderComponent({ surveyIdFromParams: 'survey1' });

        await waitFor(() => {
            expect(mockOnGetSurvey).toHaveBeenCalled();
        });
        // Check if the creator's JSON was set to defaultJson upon error
        expect(mockCoreCreatorInstance.JSON).toEqual({
            elements: [{ name: "Question1", type: "text", title: "Please enter your name:", isRequired: true }]
        });
        // Check for alert message (optional, depends on how strictly you want to test UI feedback)
        // For alert, you might need to spy on window.alert if not using a custom modal
    });
  });

  describe('creator.saveSurveyFunc (Create/Update Logic)', () => {
    it('calls onCreateSurvey when saving a new survey', async () => {
      renderComponent(); // No surveyIdFromParams initially

      // Wait for component to stabilize (any initial effects)
      await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0)); // let microtasks complete
      });

      // Simulate SurveyJS calling saveSurveyFunc
      // The saveSurveyFunc is assigned to the instance of SurveyCreator (from survey-creator-core)
      // that is created within SurveyJsCreatorComponent.
      expect(mockCoreCreatorInstance).toBeDefined();
      expect(mockCoreCreatorInstance.saveSurveyFunc).toBeInstanceOf(Function);

      const mockSaveCallback = vi.fn();
      mockCoreCreatorInstance.JSON = { title: 'Test Survey' }; // Set some JSON to be saved

      await act(async () => {
        // Directly invoke the function as SurveyJS would
         mockCoreCreatorInstance.saveSurveyFunc(1, mockSaveCallback);
      });

      await waitFor(() => {
        expect(mockOnCreateSurvey).toHaveBeenCalledWith(expect.objectContaining({
          surveyJson: { title: 'Test Survey' },
          // Check other default metadata if necessary
        }));
      });
      expect(mockSaveCallback).toHaveBeenCalledWith(1, true);
    });

    it('calls onUpdateSurvey when saving an existing survey', async () => {
      const initialSurveyData = { id: 'existingSurvey123', surveyJson: { title: 'Initial Title' }, ownerUsername: 'testuser' };
      mockOnGetSurvey.mockResolvedValue(initialSurveyData);

      renderComponent({ surveyIdFromParams: 'existingSurvey123' });

      await waitFor(() => {
        expect(mockOnGetSurvey).toHaveBeenCalledWith('existingSurvey123');
      });

      expect(mockCoreCreatorInstance).toBeDefined();
      expect(mockCoreCreatorInstance.saveSurveyFunc).toBeInstanceOf(Function);

      const mockSaveCallback = vi.fn();
      mockCoreCreatorInstance.JSON = { title: 'Updated Title' }; // Simulate changes

      await act(async () => {
        mockCoreCreatorInstance.saveSurveyFunc(1, mockSaveCallback);
      });

      await waitFor(() => {
        expect(mockOnUpdateSurvey).toHaveBeenCalledWith('existingSurvey123', expect.objectContaining({
          surveyJson: { title: 'Updated Title' },
        }));
      });
      expect(mockSaveCallback).toHaveBeenCalledWith(1, true);
    });
  });

  describe('onFetchSharedUsers Interaction', () => {
    it('calls onFetchSharedUsers after a survey is successfully loaded', async () => {
      const surveyData = { id: 'survey123', surveyJson: { title: 'Test' }, ownerUsername: 'testuser', sharedWithUsers: [] };
      mockOnGetSurvey.mockResolvedValue(surveyData);

      renderComponent({ surveyIdFromParams: 'survey123' });

      await waitFor(() => {
        expect(mockOnGetSurvey).toHaveBeenCalledWith('survey123');
      });

      // onFetchSharedUsers should be called after surveyId state is set from the loaded survey
      await waitFor(() => {
        expect(mockOnFetchSharedUsers).toHaveBeenCalledWith('survey123');
      });
    });

     it('calls onFetchSharedUsers after a new survey is successfully created', async () => {
      const createdSurveyData = { id: 'newSurvey789', surveyJson: { title: 'New' }, ownerUsername: 'testuser', surveyMode: 'public', dataClassification: 'public', status: 'drafted' };
      mockOnCreateSurvey.mockResolvedValue(createdSurveyData);

      renderComponent(); // New survey

      await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0));
      });

      expect(mockCoreCreatorInstance).toBeDefined();
      const mockSaveCallback = vi.fn();
      mockCoreCreatorInstance.JSON = { title: 'A New Survey' };

      await act(async () => {
        mockCoreCreatorInstance.saveSurveyFunc(1, mockSaveCallback);
      });

      await waitFor(() => expect(mockOnCreateSurvey).toHaveBeenCalled());

      // After creation, surveyId is set, which should trigger fetchSharedUsers
      await waitFor(() => {
        expect(mockOnFetchSharedUsers).toHaveBeenCalledWith('newSurvey789');
      });
    });
  });

  describe('Share/Unshare Interactions', () => {
    beforeEach(async () => {
      // Ensure a survey is loaded for these tests
      const surveyData = { id: 'shareTestSurvey', surveyJson: { title: 'Share Test' }, ownerUsername: 'testuser', sharedWithUsers: [] };
      mockOnGetSurvey.mockResolvedValue(surveyData);
      mockOnFetchSharedUsers.mockResolvedValue({ surveyId: 'shareTestSurvey', sharedWithUsers: [] }); // Initial state: no users shared

      renderComponent({ surveyIdFromParams: 'shareTestSurvey' });

      await waitFor(() => expect(mockOnGetSurvey).toHaveBeenCalledWith('shareTestSurvey'));
      await waitFor(() => expect(mockOnFetchSharedUsers).toHaveBeenCalledWith('shareTestSurvey'));
      // Ensure "Share Survey" section is visible
      expect(screen.getByText('Share Survey')).toBeInTheDocument();
    });

    it('calls onShareSurvey when share button is clicked with a username', async () => {
      const usernameInput = screen.getByPlaceholderText('Enter username to share with');
      const shareButton = screen.getByRole('button', { name: 'Share' });

      fireEvent.change(usernameInput, { target: { value: 'newUserToShare' } });
      fireEvent.click(shareButton);

      await waitFor(() => {
        expect(mockOnShareSurvey).toHaveBeenCalledWith('shareTestSurvey', 'newUserToShare');
      });

      // Check if list refreshes (onFetchSharedUsers is called again)
      await waitFor(() => {
          // It's called once initially, then again after sharing
          expect(mockOnFetchSharedUsers).toHaveBeenCalledTimes(2);
      });
    });

    it('calls onUnshareSurvey when unshare button is clicked for a shared user', async () => {
      // Mock that the survey is already shared with one user
      const initialSharedUsers = [{ id: 'userToUnshare1', username: 'userOne' }];
      mockOnFetchSharedUsers.mockResolvedValueOnce({ surveyId: 'shareTestSurvey', sharedWithUsers: initialSharedUsers });

      // Re-trigger fetch or re-render (simpler to re-render for test state)
      // For this, we'll rely on the fetch after share to update the list.
      // Let's simulate a share first, then unshare.

      // 1. Share with a user so they appear in the list
      mockOnShareSurvey.mockResolvedValueOnce({}); // Success for share
      mockOnFetchSharedUsers.mockResolvedValueOnce({ // After sharing, this user is returned
          surveyId: 'shareTestSurvey',
          sharedWithUsers: [{ id: 'userToUnshare1', username: 'userOne' }]
      });

      fireEvent.change(screen.getByPlaceholderText('Enter username to share with'), { target: { value: 'userOne' } });
      fireEvent.click(screen.getByRole('button', { name: 'Share' }));

      await waitFor(() => expect(mockOnShareSurvey).toHaveBeenCalledWith('shareTestSurvey', 'userOne'));
      await waitFor(() => expect(screen.getByText('userOne')).toBeInTheDocument()); // User appears in list

      // 2. Now unshare that user
      const unshareButtons = screen.getAllByRole('button', { name: 'Unshare' });
      expect(unshareButtons.length).toBe(1); // Should be one unshare button for 'userOne'
      fireEvent.click(unshareButtons[0]);

      await waitFor(() => {
        expect(mockOnUnshareSurvey).toHaveBeenCalledWith('shareTestSurvey', 'userToUnshare1');
      });

      // Check if list refreshes
       await waitFor(() => {
          // Called: initial, after share, after unshare
          expect(mockOnFetchSharedUsers).toHaveBeenCalledTimes(3);
      });
    });
  });

});
