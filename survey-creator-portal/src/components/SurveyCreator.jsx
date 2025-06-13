// survey-creator-portal/src/components/SurveyCreator.jsx
import React, { useState, useEffect } from 'react';
// Material UI imports
import { Button, TextField, Select, MenuItem, FormControl, InputLabel, Box, Typography, Paper, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, Grid, Stack } from '@mui/material';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import DeleteIcon from '@mui/icons-material/Delete';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import { DndProvider, useDrag, useDrop } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { TouchBackend } from 'react-dnd-touch-backend'; // For touch support

// Service imports
import { getSurvey, createSurvey, updateSurvey } from '../services/surveyService.js';

// Table view component (assuming this is the one from the previous attempt)
// Ensure this component exists and is compatible or remove/update it.
// For now, we assume it might exist and try to pass compatible props.
// If 'टेबलमोडमेंदेखें' is not found, this will cause a runtime error.
// import { टेबलमोडमेंदेखें } from './टेबलमोडमेंदेखें';
// For now, let's comment it out to avoid potential import errors if the file doesn't exist or is not relevant.
// If it is needed, it should be created or uncommented.

const ItemTypes = {
  QUESTION: 'question',
};

const isTouchDevice = () => {
  if (typeof window !== 'undefined' && 'ontouchstart' in window) {
    return true;
  }
  return false;
};
const backend = isTouchDevice() ? TouchBackend : HTML5Backend;

const DraggableQuestion = ({ question, index, moveQuestion, handleUpdateQuestion, handleDeleteQuestion }) => {
  const ref = React.useRef(null);
  const [, drop] = useDrop({
    accept: ItemTypes.QUESTION,
    hover(item, monitor) {
      if (!ref.current) return;
      const dragIndex = item.index;
      const hoverIndex = index;
      if (dragIndex === hoverIndex) return;
      const hoverBoundingRect = ref.current?.getBoundingClientRect();
      const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;
      const clientOffset = monitor.getClientOffset();
      const hoverClientY = clientOffset.y - hoverBoundingRect.top;
      if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) return;
      if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) return;
      moveQuestion(dragIndex, hoverIndex);
      item.index = hoverIndex;
    },
  });

  const [{ isDragging }, drag, preview] = useDrag({
    type: ItemTypes.QUESTION,
    item: () => ({ id: question.id, index }),
    collect: (monitor) => ({ isDragging: monitor.isDragging() }),
  });

  drag(drop(ref));

  return (
    <Box ref={preview} sx={{ opacity: isDragging ? 0.5 : 1, mb: 2 }}>
      <Paper ref={ref} elevation={2} sx={{ p: 2, display: 'flex', alignItems: 'center', backgroundColor: isDragging ? 'action.hover' : 'background.paper' }}>
        <IconButton ref={drag} size="small" sx={{ cursor: 'grab', mr: 1 }} aria-label="drag question">
          <DragIndicatorIcon />
        </IconButton>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="Question Text"
              value={question.text || ''}
              onChange={(e) => handleUpdateQuestion(index, 'text', e.target.value)}
              variant="outlined"
              sx={{ mb: { xs: 1, sm: 0 } }}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth variant="outlined">
              <InputLabel>Question Type</InputLabel>
              <Select
                value={question.type || 'text'}
                onChange={(e) => handleUpdateQuestion(index, 'type', e.target.value)}
                label="Question Type"
              >
                <MenuItem value="text">Text</MenuItem>
                <MenuItem value="multiple-choice">Multiple Choice</MenuItem>
                <MenuItem value="rating">Rating (1-5)</MenuItem>
                <MenuItem value="boolean">Yes/No</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={2} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
            <IconButton onClick={() => handleDeleteQuestion(index)} color="error" aria-label="delete question">
              <DeleteIcon />
            </IconButton>
          </Grid>
          {question.type === 'multiple-choice' && (
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Options (comma-separated)"
                value={question.options ? question.options.join(',') : ''}
                onChange={(e) => handleUpdateQuestion(index, 'options', e.target.value.split(',').map(opt => opt.trim()))}
                variant="outlined"
                helperText="Enter options separated by commas"
              />
            </Grid>
          )}
        </Grid>
      </Paper>
    </Box>
  );
};

// Assuming surveyId might be passed via props, e.g., <SurveyCreator surveyIdFromParent="some-id" />
const SurveyCreator = ({ surveyIdFromParent = null }) => {
  const [surveyTitle, setSurveyTitle] = useState('');
  const [surveyDescription, setSurveyDescription] = useState('');
  const [surveyQuestions, setSurveyQuestions] = useState([]);
  const [surveyId, setSurveyId] = useState(surveyIdFromParent);
  const [surveyStatus, setSurveyStatus] = useState('draft');

  const [previewOpen, setPreviewOpen] = useState(false);
  const [jsonViewOpen, setJsonViewOpen] = useState(false);
  const [tableViewOpen, setTableViewOpen] = useState(false); // State for TableView
  const [loading, setLoading] = useState(false);
  const [initialLoadDone, setInitialLoadDone] = useState(false);


  useEffect(() => {
    const loadSurveyDetails = async (id_to_load) => {
      if (!id_to_load) {
        setSurveyTitle('');
        setSurveyDescription('');
        setSurveyQuestions([{ id: `new-${Date.now()}`, text: 'New Question', type: 'text', options: [] }]);
        setSurveyStatus('draft');
        setSurveyId(null);
        setInitialLoadDone(true);
        return;
      }
      setLoading(true);
      try {
        console.log(`Loading survey with ID: ${id_to_load}`);
        const survey = await getSurvey(id_to_load);
        setSurveyId(survey.id);
        setSurveyTitle(survey.title);
        setSurveyDescription(survey.description);
        setSurveyQuestions(survey.questions.map(q => ({
            id: q.id || `new-${Date.now()}-${Math.random()}`,
            text: q.text || '',
            type: q.type || 'text',
            options: q.options || (q.type === 'multiple-choice' ? ['Option 1'] : [])
        })));
        setSurveyStatus(survey.status);
      } catch (error) {
        console.error("Failed to load survey:", error);
        setSurveyId(null); // Reset ID if survey not found or error
        setSurveyTitle('');
        setSurveyDescription('');
        setSurveyQuestions([{ id: `new-${Date.now()}`, text: 'New Question', type: 'text', options: [] }]);
        setSurveyStatus('draft');
      } finally {
        setLoading(false);
        setInitialLoadDone(true);
      }
    };

    loadSurveyDetails(surveyId);

  }, [surveyId]); // Effect runs when surveyId state changes

  const addQuestion = () => {
    setSurveyQuestions([
      ...surveyQuestions,
      { id: `new-${Date.now()}`, text: 'New Question', type: 'text', options: [] }
    ]);
  };

  const handleUpdateQuestion = (index, field, value) => {
    const newQuestions = [...surveyQuestions];
    const questionToUpdate = { ...newQuestions[index] };
    questionToUpdate[field] = value;

    if (field === 'type' && value === 'multiple-choice' && (!questionToUpdate.options || questionToUpdate.options.length === 0)) {
      questionToUpdate.options = ['Option 1'];
    } else if (field === 'type' && value !== 'multiple-choice') {
      questionToUpdate.options = [];
    }
    newQuestions[index] = questionToUpdate;
    setSurveyQuestions(newQuestions);
  };

  const handleDeleteQuestion = (index) => {
    const newQuestions = surveyQuestions.filter((_, i) => i !== index);
    setSurveyQuestions(newQuestions);
  };

  const moveQuestion = (dragIndex, hoverIndex) => {
    const draggedQuestion = surveyQuestions[dragIndex];
    const newQuestions = [...surveyQuestions];
    newQuestions.splice(dragIndex, 1);
    newQuestions.splice(hoverIndex, 0, draggedQuestion);
    setSurveyQuestions(newQuestions);
  };

  const handleSaveSurvey = async (statusToSet) => {
    setLoading(true);
    const finalStatus = statusToSet || surveyStatus;
    const questionsForApi = surveyQuestions.map(q => ({
        id: String(q.id).startsWith('new-') ? undefined : q.id,
        text: q.text,
        type: q.type,
        options: q.options && q.options.length > 0 ? q.options.filter(opt => opt.trim() !== '') : undefined,
    }));

    const surveyData = {
      title: surveyTitle,
      description: surveyDescription,
      questions: questionsForApi,
      status: finalStatus,
      userId: 'user1' // Mock user ID, replace with actual user ID from context/auth
    };

    try {
      let savedSurvey;
      if (surveyId && !String(surveyId).startsWith("new-")) {
        savedSurvey = await updateSurvey(surveyId, { ...surveyData, id: surveyId });
        console.log('Survey updated:', savedSurvey);
      } else {
        savedSurvey = await createSurvey(surveyData);
        console.log('Survey created:', savedSurvey);
        setSurveyId(savedSurvey.id); // Update state with new ID from backend
      }
      // Update state with potentially modified data from backend (e.g. confirmed ID, status)
      setSurveyTitle(savedSurvey.title);
      setSurveyDescription(savedSurvey.description);
      setSurveyStatus(savedSurvey.status);
      setSurveyQuestions(savedSurvey.questions.map(q => ({ // Re-map questions from response
            id: q.id,
            text: q.text,
            type: q.type,
            options: q.options || (q.type === 'multiple-choice' ? [] : [])
      })));
      // alert(`Survey ${surveyId ? 'updated' : 'created'} successfully! Status: ${savedSurvey.status}`);
    } catch (error) {
      console.error('Failed to save survey:', error);
      // alert('Failed to save survey. Check console for details.');
    } finally {
      setLoading(false);
    }
  };

  if (!initialLoadDone) {
      return <Typography sx={{p:3}}>Loading survey details...</Typography>;
  }

  return (
    <DndProvider backend={backend}>
      <Paper elevation={3} sx={{ p: { xs: 2, sm: 3 }, m: { xs: 1, sm: 2 } }}>
        <Typography variant="h4" component="h1" gutterBottom sx={{ textAlign: 'center', mb: 1 }}>
          {surveyId && !String(surveyId).startsWith("new-") ? 'Edit Survey' : 'Create New Survey'}
        </Typography>
        {(surveyId && !String(surveyId).startsWith("new-")) &&
         <Typography variant="body2" color="textSecondary" sx={{ textAlign: 'center', mb: 3 }}>
            ID: {surveyId} | Status: <span style={{ fontWeight: surveyStatus === 'published' ? 'bold' : 'normal'}}>{surveyStatus}</span>
         </Typography>
        }

        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Survey Title"
              value={surveyTitle}
              onChange={(e) => setSurveyTitle(e.target.value)}
              variant="outlined"
              disabled={loading}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Survey Description"
              value={surveyDescription}
              onChange={(e) => setSurveyDescription(e.target.value)}
              variant="outlined"
              multiline
              rows={2}
              disabled={loading}
            />
          </Grid>
        </Grid>

        <Typography variant="h5" component="h2" gutterBottom sx={{ mb: 2 }}>
          Questions
        </Typography>

        {surveyQuestions.map((question, index) => (
          <DraggableQuestion
            key={question.id.toString()}
            index={index}
            question={question}
            moveQuestion={moveQuestion}
            handleUpdateQuestion={handleUpdateQuestion}
            handleDeleteQuestion={handleDeleteQuestion}
          />
        ))}

        <Button
          variant="contained"
          startIcon={<AddCircleOutlineIcon />}
          onClick={addQuestion}
          sx={{ mt: 2, mb: 3 }}
          disabled={loading}
        >
          Add New Question
        </Button>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="center" sx={{ mt: 3, mb: 2, flexWrap: 'wrap' }}>
          <Button variant="outlined" onClick={() => handleSaveSurvey('draft')} disabled={loading}>
            {surveyId && !String(surveyId).startsWith("new-") ? 'Save Draft Changes' : 'Save as Draft'}
          </Button>
          <Button variant="contained" color="primary" onClick={() => handleSaveSurvey('published')} disabled={loading}>
            {surveyStatus === 'published' ? 'Update Published Survey' : 'Save and Publish'}
          </Button>
        </Stack>

        <Box sx={{ mt: 3, display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 1.5, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Button variant="outlined" onClick={() => setJsonViewOpen(true)} disabled={loading || surveyQuestions.length === 0}>View JSON</Button>
          <Button variant="outlined" onClick={() => setPreviewOpen(true)} disabled={loading || surveyQuestions.length === 0}>Preview Survey</Button>
          {/* <Button variant="outlined" onClick={() => setTableViewOpen(true)} disabled={loading || surveyQuestions.length === 0}>View Table</Button> */}
        </Box>

        <Dialog open={jsonViewOpen} onClose={() => setJsonViewOpen(false)} fullWidth maxWidth="md">
          <DialogTitle>Survey JSON</DialogTitle>
          <DialogContent>
            <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '60vh', overflowY: 'auto' }}>
              {JSON.stringify({ id: surveyId, title: surveyTitle, description: surveyDescription, questions: surveyQuestions, status: surveyStatus }, null, 2)}
            </pre>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setJsonViewOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>

        <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>{surveyTitle || "Survey Preview"}</DialogTitle>
          <DialogContent>
            {surveyDescription && <Typography variant="body1" sx={{mb: 2}}>{surveyDescription}</Typography>}
            {surveyQuestions.map((q, i) => (
              <Box key={q.id ? q.id.toString() : i} sx={{ mb: 2, p:1, borderBottom: '1px solid #eee' }}>
                <Typography variant="subtitle1" component="div">{i + 1}. {q.text || "[No question text]"}</Typography>
                {q.type === 'text' && <TextField fullWidth variant="standard" placeholder="Your answer" margin="dense"/>}
                {q.type === 'multiple-choice' && q.options && (
                  <FormControl component="fieldset" sx={{mt:1}}>
                    {q.options.map((opt, idx) => (
                      <Typography key={idx} component="div" sx={{ml:2}}>- {String(opt).trim()}</Typography>
                    ))}
                  </FormControl>
                )}
                {q.type === 'rating' && <TextField type="number" inputProps={{ min: 1, max: 5 }} variant="standard" placeholder="Rating (1-5)" margin="dense"/>}
                {q.type === 'boolean' && <Box sx={{mt:1}}><Button size="small" variant="outlined" sx={{mr:1}}>Yes</Button><Button size="small" variant="outlined">No</Button></Box>}
              </Box>
            ))}
            {(!surveyQuestions || surveyQuestions.length === 0) && <Typography>This survey has no questions yet.</Typography>}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setPreviewOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>

        {/* Table View Dialog - Commented out as 'टेबलमोडमेंदेखें' import is also commented out.
         <Dialog open={tableViewOpen} onClose={() => setTableViewOpen(false)} fullWidth maxWidth="lg">
          <DialogTitle>Survey Questions Table</DialogTitle>
          <DialogContent>
            <टेबलमोडमेंदेखें questions={surveyQuestions.map(q => ({...q, title: q.text, answerType: q.type}))} />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setTableViewOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>
        */}
      </Paper>
    </DndProvider>
  );
};

export default SurveyCreator;
