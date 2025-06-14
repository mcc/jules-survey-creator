import React from 'react';
import QuestionTableView from './QuestionTableView'; // Import QuestionTableView

// Re-using modal styles (ideally, these would be in a shared CSS file)
const modalStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100%',
  height: '100%',
  backgroundColor: 'rgba(0, 0, 0, 0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 1000,
};

const modalContentStyle = {
  backgroundColor: '#fff',
  padding: '20px',
  borderRadius: '5px',
  maxHeight: '80vh',
  overflowY: 'auto',
  minWidth: '60%', // Adjusted for potentially more content
  maxWidth: '90%', // Ensure modal is not too wide
};

function PreviewModal({ isOpen, onClose, surveyJson }) { // Changed props
  if (!isOpen) {
    return null;
  }

  // Extract questions from surveyJson
  // SurveyJS stores questions in pages[0].elements
  // Handle cases where surveyJson might be null or pages/elements are missing
  const questions = surveyJson?.pages?.[0]?.elements || [];

  return (
    <div style={modalStyle} onClick={onClose}>
      <div style={modalContentStyle} onClick={(e) => e.stopPropagation()}>
        <h2>Survey Questions Preview</h2>
        {questions.length > 0 ? (
          <QuestionTableView questions={questions} />
        ) : (
          <p>This survey currently has no questions to display.</p>
        )}
        <button onClick={onClose} style={{ marginTop: '20px', padding: '10px 20px', cursor: 'pointer' }}>
          Close
        </button>
      </div>
    </div>
  );
}

export default PreviewModal;
