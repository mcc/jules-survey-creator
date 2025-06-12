import React from 'react';

const answerTypes = [
  { value: 'text', label: 'Single Line Text' },
  { value: 'textarea', label: 'Multi-line Text' },
  { value: 'radio', label: 'Multiple Choice (Single Select)' },
  { value: 'checkbox', label: 'Multiple Choice (Multiple Select)' },
];

function AnswerTypeSelector({ value, onChange }) { // Destructure props
  return (
    <div>
      <label htmlFor="answer-type-select">Answer Type:</label>
      <select
        id="answer-type-select"
        value={value} // Use prop for value
        onChange={(e) => onChange(e.target.value)} // Call prop onChange
      >
        {answerTypes.map((type) => (
          <option key={type.value} value={type.value}>
            {type.label}
          </option>
        ))}
      </select>
    </div>
  );
}

export default AnswerTypeSelector;
