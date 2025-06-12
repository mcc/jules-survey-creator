import React from 'react';

// Basic modal styles (can be moved to styles.css later)
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
  zIndex: 1000, // Ensure it's on top
};

const modalContentStyle = {
  backgroundColor: '#fff',
  padding: '20px',
  borderRadius: '5px',
  maxHeight: '80vh', // Max height
  overflowY: 'auto', // Scrollable
  minWidth: '50%', // Minimum width
};

function JsonViewModal({ isOpen, onClose, questions }) {
  if (!isOpen) {
    return null;
  }

  return (
    <div style={modalStyle} onClick={onClose}> {/* Optional: close on backdrop click */}
      <div style={modalContentStyle} onClick={(e) => e.stopPropagation()}> {/* Prevent closing when clicking content */}
        <h2>Survey JSON</h2>
        <pre>
          <code>{JSON.stringify(questions, null, 2)}</code>
        </pre>
        <button onClick={onClose}>Close</button>
      </div>
    </div>
  );
}

export default JsonViewModal;
