import React, { useContext, useEffect, useState } from 'react';
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

function SurveyJsCreatorComponent({ json, options }) {
  
  const { token } = useContext(AuthContext);
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
        creator.JSON = { // Set default JSON for a new survey
            elements: [{
                name: "question1",
                type: "text",
                title: "New Survey - Edit Me"
            }],
        };
        // Reset metadata states for a new survey
        setSurveyId(null);
        setSurveyMode('public');
        setDataClassification('public');
        setStatus('drafted');
        setSharedUsersList([]); // Reset shared users list for a new survey
        // }
    }, [creator, token]); // Initial setup effect

    // Effect to fetch shared users when surveyId changes (e.g., after load or new save)
    useEffect(() => {
        if (surveyId) {
            fetchSharedUsers(surveyId);
        } else {
            setSharedUsersList([]); // Clear if no surveyId
        }
    }, [surveyId, token]); // Depends on surveyId and token (for fetchSharedUsers)
creator.saveSurveyFunc = (saveNo, callback) => {
        const surveyJsonToSave = creator.JSON; // surveyJson is the SurveyJS definition
        console.log("Attempting to save survey JSON:", surveyJsonToSave);

        const surveyData = {
            surveyJson: surveyJsonToSave, // This key should match the backend DTO field for SurveyJS JSON
            surveyMode: surveyMode,
            dataClassification: dataClassification,
            status: status
        };

        let requestMethod = 'POST';
        let apiUrl = '/api/surveys/';

        if (surveyId) {
            requestMethod = 'PUT';
            apiUrl = `/api/surveys/${surveyId}`;
        }

        console.log(`Saving survey. Method: ${requestMethod}, URL: ${apiUrl}, Data:`, surveyData);

        fetch(apiUrl, {
            method: requestMethod,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(surveyData)
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                response.text().then(text => {
                    console.error(`Error saving survey. Status: ${response.status}, Response: ${text}`);
                });
                throw new Error(`Save failed: ${response.statusText} (Status: ${response.status})`);
            }
        })
        .then(savedSurvey => {
            console.log("Survey saved successfully:", savedSurvey);
            setSurveyId(savedSurvey.id); // Update surveyId state with the ID from backend response
            // Optionally, update other states if backend modifies them (e.g. creationDate, modificationDate)
            // For instance, if the backend returns the full survey object including the JSON:
            // creator.JSON = savedSurvey.surveyJson;
            // ^ Be careful with this if surveyJson is very large or if you only want to update metadata

            // Also update state for metadata fields if the backend could have modified them
            // or if it's a new survey and these are now definitively set.
            setSurveyMode(savedSurvey.surveyMode);
            setDataClassification(savedSurvey.dataClassification);
            setStatus(savedSurvey.status);

            callback(saveNo, true);
        })
        .catch(error => {
            console.error("Error during survey save operation:", error);
            callback(saveNo, false);
        });
    };

  const fetchSharedUsers = async (currentSurveyId) => {
    if (!currentSurveyId) {
      setSharedUsersList([]);
      return;
    }
    if (!token) {
        console.error("No token available for fetching shared users.");
        alert("Authentication token not found. Please log in again.");
        return;
    }
    try {
      const response = await fetch(`/api/surveys/${currentSurveyId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch survey details to get shared users: ${response.statusText}`);
      }
      const surveyData = await response.json();
      // Assuming surveyData.sharedWithUsers is an array of objects like { id: 1, username: "user1", ...other User fields }
      // The backend Survey entity has a Set<User> sharedWithUsers.
      // The User model has id and username.
      // So, surveyData.sharedWithUsers should be compatible.
      setSharedUsersList(surveyData.sharedWithUsers ? surveyData.sharedWithUsers.map(u => ({id: u.id, username: u.username})) : []);
    } catch (error) {
      console.error("Error fetching shared users:", error);
      alert("Error fetching shared users list.");
      setSharedUsersList([]); // Clear list on error
    }
  };

  const handleShareSurvey = async () => {
    if (!surveyId || !shareWithUsername.trim()) {
      alert("Please select a survey and enter a username to share with.");
      return;
    }
    if (!token) {
      alert("Authentication token not found. Please log in again.");
      return;
    }

    let userIdToShare;
    try {
      // Step 1: Get User ID by Username
      const userResponse = await fetch(`/api/auth/users/by-username/${shareWithUsername.trim()}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!userResponse.ok) {
        if (userResponse.status === 404) {
          alert(`User "${shareWithUsername.trim()}" not found.`);
        } else {
          alert(`Error finding user: ${userResponse.statusText}`);
        }
        return;
      }
      const userData = await userResponse.json();
      userIdToShare = userData.id;

      // Prevent sharing with oneself if the current user is the one being shared with
      // This requires knowing the current user's ID or username.
      // For now, this check is omitted but can be added if context provides current user info.
      // Also, the backend already prevents sharing with the owner if that's the case.

    } catch (error) {
      console.error("Error fetching user by username:", error);
      alert("An error occurred while trying to find the user.");
      return;
    }

    if (!userIdToShare) {
        alert("Could not find user ID to share."); // Should be caught by previous checks
        return;
    }

    try {
      // Step 2: Share the Survey
      const shareResponse = await fetch(`/api/surveys/${surveyId}/share/${userIdToShare}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!shareResponse.ok) {
        // Try to get more details from backend error
        const errorData = await shareResponse.json().catch(() => null);
        if (shareResponse.status === 400 && errorData && errorData.message && errorData.message.includes("already shared")) {
             alert(`Survey is already shared with user ${shareWithUsername.trim()}.`);
        } else if (shareResponse.status === 400 && errorData && errorData.message && errorData.message.includes("cannot share with the owner")) {
            alert(`Cannot share the survey with its owner (${shareWithUsername.trim()}).`);
        }
        else {
            alert(`Error sharing survey: ${shareResponse.statusText}`);
        }
        return;
      }

      alert(`Survey shared successfully with ${shareWithUsername.trim()}!`);
      setShareWithUsername(''); // Clear input
      fetchSharedUsers(surveyId); // Refresh list
    } catch (error) {
      console.error("Error sharing survey:", error);
      alert("An error occurred while trying to share the survey.");
    }
  };

  const handleUnshareSurvey = async (userIdToUnshare) => {
    if (!surveyId || !userIdToUnshare) {
      alert("Survey ID or User ID to unshare is missing.");
      return;
    }
    if (!token) {
      alert("Authentication token not found. Please log in again.");
      return;
    }

    try {
      const response = await fetch(`/api/surveys/${surveyId}/unshare/${userIdToUnshare}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) {
        alert(`Error unsharing survey: ${response.statusText}`);
        return;
      }

      alert("User unshared successfully!");
      fetchSharedUsers(surveyId); // Refresh list
    } catch (error) {
      console.error("Error unsharing survey:", error);
      alert("An error occurred while trying to unshare the survey.");
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
