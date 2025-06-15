import React, { useContext, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
// import { getSurvey } from '../services/surveyService.js'; // Removed as per refactor
import { SurveyCreatorModel  } from 'survey-creator-core';
import { SurveyCreatorComponent, SurveyCreator } from 'survey-creator-react';
import { AuthContext } from '../contexts/AuthContext';
import PreviewModal from './PreviewModal';
import QuestionTableView from './QuestionTableView';
// components/SurveyCreator.tsx
import "survey-core/survey-core.css";
import "survey-creator-core/survey-creator-core.css";

const creatorOptions = {
    autoSaveEnabled: true,
    collapseOnDrag: true,
    showLogicTab: true,
    showTranslationTab: true

};

const defaultJson = {
    elements: [{
        name: "Question1",
        type: "text",
        title: "Please enter your name:",
        isRequired: true
    }]
};


const defaultCreatorOptions = {
  autoSaveEnabled: false,
  collapseOnDrag: true
};

function SurveyJsCreatorComponent({ json, options, onGetSurvey, onCreateSurvey, onUpdateSurvey, onFetchSharedUsers, onShareSurvey, onUnshareSurvey }) {
  
  const { user } = useContext(AuthContext); // Removed token, user might still be needed for ownership check
  const { surveyId: surveyIdFromParams } = useParams();
  const [surveyId, setSurveyId] = useState(null);
  const [surveyMode, setSurveyMode] = useState('public');
  const [dataClassification, setDataClassification] = useState('public');
  const [status, setStatus] = useState('drafted');
  const [shareWithUsername, setShareWithUsername] = useState('');
  const [sharedUsersList, setSharedUsersList] = useState([]);
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  let [creator, setCreator] = useState();

  if (!creator) {
    creator = new SurveyCreator(options || defaultCreatorOptions);
    setCreator(creator);
  }

    useEffect(() => {
        const loadSurvey = async () => {
            if (surveyIdFromParams && user && onGetSurvey) { // Ensure user and onGetSurvey are available
                try {
                    const fetchedSurvey = await onGetSurvey(surveyIdFromParams); // Use onGetSurvey prop

                    // Ownership check using username:
                    // Assumes backend will provide ownerUsername in fetchedSurvey.
                    // Logged-in user's username is in user.sub (from JWT's subject claim).
                    if (!fetchedSurvey.owner || fetchedSurvey.owner.username !== user.sub) { // ***** USER OWNERSHIP CHECK *****
                        alert("Error: You are not authorized to edit this survey, or the survey does not exist, or owner information is missing.");
                        // Reset to new survey state
                        if (creator) {
                             creator.JSON = defaultJson; // Or your preferred default JSON for new surveys
                        }
                        setSurveyId(null);
                        setSurveyMode('public');
                        setDataClassification('public');
                        setStatus('drafted');
                        setSharedUsersList([]);
                        return;
                    }

                    // If authorized, load the survey
                    creator.JSON = fetchedSurvey.surveyJson;
                    setSurveyId(fetchedSurvey.id); // Update state surveyId with the loaded survey's ID
                    setSurveyMode(fetchedSurvey.surveyMode);
                    setDataClassification(fetchedSurvey.dataClassification);
                    setStatus(fetchedSurvey.status);
                    // Shared users will be fetched by the existing useEffect dependent on `surveyId` state
                } catch (error) {
                    console.error("Error fetching survey:", error);
                    // Check if the error is due to 404 Not Found or 403 Forbidden specifically
                    if (error.response && (error.response.status === 404 || error.response.status === 403)) {
                        alert("Error: Survey not found or you do not have permission to access it.");
                    } else {
                        alert("Failed to load survey. Please try again.");
                    }
                    // Reset to new survey state in case of error
                    creator.JSON = defaultJson;
                    setSurveyId(null);
                    setSurveyMode('public');
                    setDataClassification('public');
                    setStatus('drafted');
                    setSharedUsersList([]);
                }
            } else if (!surveyIdFromParams) {
                // No surveyId in params, so it's a new survey
                creator.JSON = defaultJson; // Your default JSON for new surveys
                setSurveyId(null);
                setSurveyMode('public');
                setDataClassification('public');
                setStatus('drafted');
                setSharedUsersList([]);
            }
        };

        if (creator) { // Ensure creator instance exists
            loadSurvey();
        }
        // This effect should run when surveyIdFromParams changes, or when creator/user/onGetSurvey are initialized.
    }, [creator, surveyIdFromParams, user, onGetSurvey]);

    // Effect to fetch shared users when surveyId changes (e.g., after load or new save)
    useEffect(() => {
        if (surveyId && onFetchSharedUsers) {
            fetchSharedUsers(surveyId);
        } else {
            setSharedUsersList([]); // Clear if no surveyId
        }
    }, [surveyId, onFetchSharedUsers]); // Depends on surveyId and onFetchSharedUsers
    creator.saveSurveyFunc = (saveNo, callback) => {
        // Inside creator.saveSurveyFunc
        const currentSurveyJsonAsObject = creator.JSON;
        console.log("Attempting to save survey JSON (object):", currentSurveyJsonAsObject);

        const surveyData = {
            title: currentSurveyJsonAsObject.title || (currentSurveyJsonAsObject.pages && currentSurveyJsonAsObject.pages[0] && currentSurveyJsonAsObject.pages[0].title) || 'Untitled Survey', // Extract title
            description: currentSurveyJsonAsObject.description || (currentSurveyJsonAsObject.pages && currentSurveyJsonAsObject.pages[0] && currentSurveyJsonAsObject.pages[0].description) || '', // Extract description
            surveyJson: (currentSurveyJsonAsObject), // Stringify the SurveyJS JSON object
            surveyMode: surveyMode, // from local state
            dataClassification: dataClassification, // from local state
            status: status // from local state
        };

        // The rest of the logic (calling onUpdateSurvey or onCreateSurvey) remains the same,
        // but now 'surveyData' has a stringified 'surveyJson' and includes title/description.
        if (surveyId) {
            if (onUpdateSurvey) {
                onUpdateSurvey(surveyId, surveyData) // surveyData now contains stringified surveyJson
                    .then(savedSurvey => {
                        // ... (existing success logic)
                        // Update local state from savedSurvey, which is SurveyDTO
                        setSurveyId(savedSurvey.id);
                        setSurveyMode(savedSurvey.surveyMode);
                        setDataClassification(savedSurvey.dataClassification);
                        setStatus(savedSurvey.status);
                        // Optionally reload creator.JSON if backend could modify it, e.g.
                        if (savedSurvey.surveyJson) creator.JSON = JSON.parse(savedSurvey.surveyJson);
                        callback(saveNo, true);
                    })
                    // ... (existing error logic)
                    .catch(error => {
                        console.error("Error during survey update operation:", error);
                        callback(saveNo, false);
                    });
            } else {
                console.error("onUpdateSurvey function not provided.");
                callback(saveNo, false);
            }
        } else {
            if (onCreateSurvey) {
                onCreateSurvey(surveyData) // surveyData now contains stringified surveyJson
                    .then(savedSurvey => {
                        // ... (existing success logic)
                         setSurveyId(savedSurvey.id);
                         setSurveyMode(savedSurvey.surveyMode);
                         setDataClassification(savedSurvey.dataClassification);
                         setStatus(savedSurvey.status);
                        // Optionally reload creator.JSON, e.g.
                        if (savedSurvey.surveyJson) creator.JSON = JSON.parse(savedSurvey.surveyJson);
                         callback(saveNo, true);
                    })
                    // ... (existing error logic)
                    .catch(error => {
                        console.error("Error during survey create operation:", error);
                        callback(saveNo, false);
                    });
            } else {
                console.error("onCreateSurvey function not provided.");
                callback(saveNo, false);
            }
        }
    };

  const fetchSharedUsers = async (currentSurveyId) => {
    if (!currentSurveyId || !onFetchSharedUsers) {
      setSharedUsersList([]);
      if (!onFetchSharedUsers) console.error("onFetchSharedUsers function not provided.");
      return;
    }
    try {
      const surveyData = await onFetchSharedUsers(currentSurveyId);
      setSharedUsersList(surveyData.sharedWithUsers ? surveyData.sharedWithUsers.map(u => ({id: u.id, username: u.username})) : []);
    } catch (error) {
      console.error("Error fetching shared users:", error);
      alert("Error fetching shared users list.");
      setSharedUsersList([]);
    }
  };

  const handleShareSurvey = async () => {
    if (!surveyId || !shareWithUsername.trim() || !onShareSurvey) {
      alert("Please select a survey, enter a username, and ensure share functionality is available.");
      if (!onShareSurvey) console.error("onShareSurvey function not provided.");
      return;
    }

    try {
      await onShareSurvey(surveyId, shareWithUsername.trim());
      alert(`Survey shared successfully with ${shareWithUsername.trim()}!`);
      setShareWithUsername(''); // Clear input
      fetchSharedUsers(surveyId); // Refresh list
    } catch (error) {
      console.error("Error sharing survey:", error);
      // Assuming error object might have a message from the backend or a generic one
      const errorMessage = error.response?.data?.message || error.message || "An error occurred while trying to share the survey.";
      if (errorMessage.includes("already shared")) {
           alert(`Survey is already shared with user ${shareWithUsername.trim()}.`);
      } else if (errorMessage.includes("cannot share with the owner")) {
          alert(`Cannot share the survey with its owner (${shareWithUsername.trim()}).`);
      } else if (errorMessage.includes("not found")) {
          alert(`User "${shareWithUsername.trim()}" not found.`);
      }
      else {
          alert(errorMessage);
      }
    }
  };

  const handleUnshareSurvey = async (userIdToUnshare) => {
    if (!surveyId || !userIdToUnshare || !onUnshareSurvey) {
      alert("Survey ID, User ID to unshare is missing, or unshare functionality is unavailable.");
      if(!onUnshareSurvey) console.error("onUnshareSurvey function not provided.");
      return;
    }

    try {
      await onUnshareSurvey(surveyId, userIdToUnshare);
      alert("User unshared successfully!");
      fetchSharedUsers(surveyId); // Refresh list
    } catch (error) {
      console.error("Error unsharing survey:", error);
      alert(error.message || "An error occurred while trying to unshare the survey.");
    }
  };

  return (<div>
            <div style={{ padding: '10px', backgroundColor: '#f0f0f0', marginBottom: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3>Survey Settings</h3>
                <div> {/* Container for buttons */}
                    <button onClick={() => setShowPreviewModal(true)} style={{ marginRight: '10px' }}>
                        Preview Survey
                    </button>
                    {/* Existing settings can be grouped or styled as needed */}
                </div>
            </div>
            <div style={{ padding: '10px', backgroundColor: '#f0f0f0', marginBottom: '10px' }}>
                <label htmlFor="surveyIdDisplay" style={{ marginRight: '10px' }}>Survey ID: </label>
                <span id="surveyIdDisplay" style={{ marginRight: '20px', fontWeight:'bold' }}>{surveyId || 'New Survey'}</span>
                <br/>
                <label htmlFor="surveyMode" style={{ marginRight: '10px' }}>Mode:</label>
                <select id="surveyMode" value={surveyMode} onChange={(e) => setSurveyMode(e.target.value)} style={{ marginRight: '20px' }}>
                    <option value="public">Public</option>
                    <option value="access_code">Access Code</option>
                    <option value="personal_access_code">Personal Access Code</option>
                </select>

                <label htmlFor="dataClassification" style={{ marginRight: '10px' }}>Data Classification:</label>
                <select id="dataClassification" value={dataClassification} onChange={(e) => setDataClassification(e.target.value)} style={{ marginRight: '20px' }}>
                    <option value="public">Public</option>
                    <option value="restricted">Restricted</option>
                    <option value="confidential">Confidential</option>
                </select>

                <label htmlFor="status" style={{ marginRight: '10px' }}>Status:</label>
                <select id="status" value={status} onChange={(e) => setStatus(e.target.value)}>
                    <option value="drafted">Drafted</option>
                    <option value="published">Published</option>
                    <option value="expired">Expired</option>
                </select>
            </div>
            
      {/* Sharing Section */}
      {surveyId && ( // Only show sharing UI if a survey is loaded/saved
        <div style={{ padding: '10px', backgroundColor: '#e0e0e0', marginBottom: '10px' }}>
          <h3>Share Survey</h3>
          <div>
            <input
              type="text"
              placeholder="Enter username to share with"
              value={shareWithUsername}
              onChange={(e) => setShareWithUsername(e.target.value)}
              style={{ marginRight: '10px' }}
            />
            <button
              onClick={handleShareSurvey}
              disabled={!surveyId || !shareWithUsername.trim()}
            >
              Share
            </button>
          </div>
          <div style={{ marginTop: '10px' }}>
            <h4>Shared With:</h4>
            {sharedUsersList.length > 0 ? (
              <ul>
                {sharedUsersList.map(user => (
                  <li key={user.id}>
                    {user.username}
                    <button
                      onClick={() => handleUnshareSurvey(user.id)}
                      style={{ marginLeft: '10px' }}
                    >
                      Unshare
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p>Not shared with any users.</p>
            )}
          </div>
        </div>
      )}

    <div style={{ height: "100vh", width: "100%" }}>
      <SurveyCreatorComponent creator={creator} />
    </div>
    {showPreviewModal && (
        <PreviewModal
            isOpen={showPreviewModal}
            onClose={() => setShowPreviewModal(false)}
            surveyJson={creator.JSON}
        />
    )}
    </div>
  );
}


export default SurveyJsCreatorComponent;
