import React from 'react';
import './QuestionTableView.css';

// Helper function to determine data type from SurveyJS question type
function getDataType(surveyJsQuestionType, question) {
  switch (surveyJsQuestionType) {
    case 'text':
    case 'comment':
    case 'dropdown': // Single selection from a list
    case 'radiogroup': // Single selection
    case 'html': // Stores HTML content as a string
      return 'string';
    case 'checkbox': // Multiple selections
    case 'tagbox': // Multiple selections, typically stored as an array of strings
      return 'array';
    case 'boolean':
      return 'boolean';
    case 'rating': // Typically numerical
      return 'number';
    case 'expression': // Result type can vary, often number or string. Defaulting to 'calculated'
      return 'calculated';
    case 'file':
      return 'file'; // Or 'object' if storing metadata
    case 'matrix': // Single choice per row, multiple rows. Often results in an object where keys are row values.
    case 'matrixdropdown': // Multiple choices per row, multiple rows. Results in an object or array of objects.
    case 'panel': // Represents a container, data is an object of its elements
      return 'object';
    case 'matrixdynamic': // Array of objects, as rows are dynamic
    case 'paneldynamic': // Array of objects
      return 'array of objects';
    case 'imagepicker':
      // Imagepicker can be single or multiple selection.
      // Check question.multiSelect to determine if it's an array or string.
      return question && question.multiSelect ? 'array' : 'string';
    default:
      return 'N/A'; // Default for unmapped types
  }
}

function QuestionTableView({ questions }) {
  if (!questions || questions.length === 0) {
    return <p>No questions to display.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Question Title</th>
          <th>Variable Name</th>
          <th>Value Type (SurveyJS)</th>
          <th>Data Type (Inferred)</th>
          <th>Options</th>
        </tr>
      </thead>
      <tbody>
        {questions.map((question) => (
          <tr key={question.name || question.id}> {/* Use question.name as key if available, fallback to id */}
            <td>{question.title || question.name}</td> {/* Display name if title is missing */}
            <td>{question.name}</td>
            <td>{question.type}</td> {/* This is the SurveyJS question type */}
            <td>{getDataType(question.type, question)}</td> {/* Inferred data type */}
            <td>
              {question.choices && question.choices.length > 0
                ? question.choices.map(choice => choice.text || choice.value || choice).join(', ') // Handle choices from SurveyJS
                : 'N/A'}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default QuestionTableView;
