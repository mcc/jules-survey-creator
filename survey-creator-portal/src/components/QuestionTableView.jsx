import React from 'react';
import './QuestionTableView.css';

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
          <th>Value Type</th>
          <th>Options</th>
        </tr>
      </thead>
      <tbody>
        {questions.map((question) => (
          <tr key={question.id}>
            <td>{question.title}</td>
            <td>{question.name || `question_${question.id}`}</td> {/* Assuming variable name might be 'name' or generated */}
            <td>{question.answerType}</td>
            <td>
              {question.options && question.options.length > 0
                ? question.options.map(option => option.text || option).join(', ')
                : 'N/A'}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default QuestionTableView;
