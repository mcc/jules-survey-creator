import React, { useState, useEffect } from 'react';
import './styles.css';
import QuestionEditor from './QuestionEditor'; // Ensure this is imported
import JsonViewModal from './JsonViewModal'; // Import JsonViewModal
import PreviewModal from './PreviewModal'; // Import PreviewModal
import QuestionTableView from './QuestionTableView'; // Import QuestionTableView
import { saveSurveyToLocalStorage, loadSurveyFromLocalStorage } from '../utils/localStorage';

function SurveyCreator() {
  const [questions, setQuestions] = useState(() => {
    const loadedQuestions = loadSurveyFromLocalStorage();
    return loadedQuestions || [];
  });
  const [isJsonModalOpen, setIsJsonModalOpen] = useState(false); // State for Json modal
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false); // State for Preview modal
  const [isTableViewOpen, setIsTableViewOpen] = useState(false); // State for TableView

  useEffect(() => {
    saveSurveyToLocalStorage(questions);
  }, [questions]);

  const addQuestion = () => {
    const newQuestion = {
      id: Date.now(), // Simple unique ID
      title: '',
      description: '',
      answerType: 'text',
      options: [],
    };
    setQuestions([...questions, newQuestion]);
  };

  const handleUpdateQuestion = (index, updatedQuestionData) => {
    const newQuestions = questions.map((q, i) =>
      i === index ? updatedQuestionData : q
    );
    setQuestions(newQuestions);
  };

  return (
    <div>
      <h1>Survey Creator</h1>
      <button onClick={addQuestion}>Add New Question</button>
      <button onClick={() => setIsJsonModalOpen(true)}>View JSON</button>
      <button onClick={() => setIsPreviewModalOpen(true)}>Preview Survey</button> {/* Button to open Preview modal */}
      <button onClick={() => setIsTableViewOpen(!isTableViewOpen)}>
        {isTableViewOpen ? 'Hide Table' : 'View Table'}
      </button>
      {isTableViewOpen && <QuestionTableView questions={questions} />}
      {questions.map((question, index) => (
        <QuestionEditor
          key={question.id}
          questionData={question}
          onUpdate={(updatedData) => handleUpdateQuestion(index, updatedData)}
          // onDelete={() => handleDeleteQuestion(index)} // For later
        />
      ))}
      <JsonViewModal
        isOpen={isJsonModalOpen}
        onClose={() => setIsJsonModalOpen(false)}
        questions={questions}
      />
      <PreviewModal
        isOpen={isPreviewModalOpen}
        onClose={() => setIsPreviewModalOpen(false)}
        questions={questions}
      />
    </div>
  );
}

export default SurveyCreator;
