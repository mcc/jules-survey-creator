export const saveSurveyToLocalStorage = (questions) => {
  try {
    const serializedState = JSON.stringify(questions);
    localStorage.setItem('surveyQuestions', serializedState);
  } catch (e) {
    console.error("Could not save survey to local storage", e);
  }
};

export const loadSurveyFromLocalStorage = () => {
  try {
    const serializedState = localStorage.getItem('surveyQuestions');
    if (serializedState === null) {
      return undefined; // Or an empty array, depending on desired default
    }
    return JSON.parse(serializedState);
  } catch (e) {
    console.error("Could not load survey from local storage", e);
    return undefined; // Or an empty array
  }
};
