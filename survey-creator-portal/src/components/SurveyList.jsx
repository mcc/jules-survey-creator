import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSurveysByUser } from '../services/surveyService.js';
import { Button, List, ListItem, ListItemText, Typography, Box, Paper, IconButton } from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import PublishIcon from '@mui/icons-material/Publish';
import UnpublishedIcon from '@mui/icons-material/Unpublished';

const SurveyList = () => {
  const navigate = useNavigate();
  const [surveys, setSurveys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Mock userId - in a real app, this would come from an auth context
  const userId = 'user1';

  useEffect(() => {
    const fetchSurveys = async () => {
      try {
        setLoading(true);
        const userSurveys = await getSurveysByUser(userId);
        setSurveys(userSurveys);
        setError(null);
      } catch (err) {
        console.error("Error fetching surveys:", err);
        setError("Failed to load surveys.");
        setSurveys([]);
      } finally {
        setLoading(false);
      }
    };

    fetchSurveys();
  }, [userId]);

  return (
    <Paper elevation={3} sx={{ p: 3, m: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4" component="h1">
          My Surveys
        </Typography>
        <Button variant="contained" color="primary" onClick={() => navigate('/survey-creator')}>
          Create New Survey
        </Button>
      </Box>

      {loading && <Typography>Loading surveys...</Typography>}
      {error && <Typography color="error">{error}</Typography>}
      {!loading && !error && surveys.length === 0 && <Typography>No surveys found.</Typography>}

      {!loading && !error && surveys.length > 0 && (
        <List>
          {surveys.map((survey) => (
            <ListItem
              key={survey.id}
              divider
              secondaryAction={
                <>
                  <IconButton edge="end" aria-label="edit" sx={{ mr: 1 }} onClick={() => navigate(`/survey-creator/${survey.id}`)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton edge="end" aria-label={survey.status === 'draft' ? 'publish' : 'unpublish'} sx={{ mr: 1 }}>
                    {survey.status === 'draft' ? <PublishIcon /> : <UnpublishedIcon />}
                  </IconButton>
                  <IconButton edge="end" aria-label="delete">
                    <DeleteIcon />
                  </IconButton>
                </>
              }
            >
              <ListItemText
                primary={survey.title}
                secondary={`Status: ${survey.status}`}
              />
            </ListItem>
          ))}
        </List>
      )}
    </Paper>
  );
};

export default SurveyList;
