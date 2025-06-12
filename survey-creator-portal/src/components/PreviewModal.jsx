import React from 'react';

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
};

function PreviewModal({ isOpen, onClose, questions }) {
  if (!isOpen) {
    return null;
  }

  return (
    <div style={modalStyle} onClick={onClose}>
      <div style={modalContentStyle} onClick={(e) => e.stopPropagation()}>
        <h2>Survey Preview</h2>
        {questions.map((question, index) => (
          <div key={question.id || index} style={{ marginBottom: '20px', borderBottom: '1px solid #eee', paddingBottom: '10px' }}>
            <h3>{index + 1}. {question.title}</h3>
            <p>{question.description}</p>
            <div>
              {question.answerType === 'text' && (
                <input type="text" disabled placeholder="Your answer here" style={{ width: '80%', padding: '8px' }} />
              )}
              {question.answerType === 'textarea' && (
                <textarea disabled placeholder="Your detailed answer here" style={{ width: '80%', padding: '8px', minHeight: '80px' }} />
              )}
              {(question.answerType === 'radio' || question.answerType === 'checkbox') && question.options && (
                <div>
                  {question.options.map((option, optIndex) => (
                    <div key={optIndex} style={{ margin: '5px 0' }}>
                      <input
                        type={question.answerType}
                        name={`question-${question.id}`} // Group radio buttons
                        id={`q${question.id}-opt${optIndex}`}
                        value={option}
                        disabled
                      />
                      <label htmlFor={`q${question.id}-opt${optIndex}`} style={{ marginLeft: '8px' }}>
                        {option}
                      </label>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        <button onClick={onClose} style={{ marginTop: '20px' }}>Close</button>
      </div>
    </div>
  );
}

export default PreviewModal;
