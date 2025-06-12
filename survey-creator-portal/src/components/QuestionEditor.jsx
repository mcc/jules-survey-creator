import React, { useState, useEffect } from 'react';
import AnswerTypeSelector from './AnswerTypeSelector';

// Props: questionData, onUpdate
function QuestionEditor({ questionData, onUpdate }) {
  const [title, setTitle] = useState(questionData.title);
  const [description, setDescription] = useState(questionData.description);
  const [answerType, setAnswerType] = useState(questionData.answerType);
  const [options, setOptions] = useState(questionData.options);

  // When local state changes, call onUpdate with the full question data
  useEffect(() => {
    // Debounce or check for actual changes if performance becomes an issue
    onUpdate({
      id: questionData.id, // Keep the original ID
      title,
      description,
      answerType,
      options,
    });
  }, [title, description, answerType, options, questionData.id, onUpdate]);

  // useEffect for clearing options based on answerType
  useEffect(() => {
    if (answerType !== 'radio' && answerType !== 'checkbox') {
      setOptions([]);
    }
    // If switching from a non-options type to an options type,
    // and the initial options for this question were empty,
    // ensure options state is an array (it should be already by useState(questionData.options))
    // This effect primarily handles clearing. Populating initial options if needed when type changes
    // would be a different concern, potentially handled by initializing options based on questionData.answerType too.
  }, [answerType]);


  const addOption = () => {
    const newOptionText = window.prompt('Enter option text:');
    if (newOptionText) {
      setOptions([...options, newOptionText]);
    }
  };

  const removeOption = (indexToRemove) => {
    setOptions(options.filter((_, index) => index !== indexToRemove));
  };

  const updateOption = (indexToUpdate, newText) => {
    setOptions(
      options.map((option, index) =>
        index === indexToUpdate ? newText : option
      )
    );
  };


  return (
    <div style={{ border: '1px solid #ccc', margin: '10px', padding: '10px' }}>
      <div>
        <label htmlFor={`question-title-${questionData.id}`}>Title:</label>
        <input
          type="text"
          id={`question-title-${questionData.id}`}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>
      <div>
        <label htmlFor={`question-description-${questionData.id}`}>Description:</label>
        <textarea
          id={`question-description-${questionData.id}`}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <AnswerTypeSelector
        value={answerType}
        onChange={(newType) => {
            // If changing to a type that doesn't use options, clear them.
            if (newType !== 'radio' && newType !== 'checkbox') {
                setOptions([]);
            }
            setAnswerType(newType);
        }}
      />
      {(answerType === 'radio' || answerType === 'checkbox') && (
        <div>
          <h4>Options:</h4>
          <ul>
            {options.map((option, index) => (
              <li key={index}>
                <input
                  type="text"
                  value={option}
                  onChange={(e) => updateOption(index, e.target.value)}
                />
                <button onClick={() => removeOption(index)}>Remove</button>
              </li>
            ))}
          </ul>
          <button onClick={addOption}>Add Option</button>
        </div>
      )}
    </div>
  );
}

export default QuestionEditor;
