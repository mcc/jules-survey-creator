import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  CircularProgress,
  Typography,
  Divider,
  Chip,
  Box,
} from '@mui/material';
import ClearIcon from '@mui/icons-material/Clear';
import { fetchSharedUsers, shareSurvey, unshareSurvey } from '../../services/surveyService';
import { searchUsers } from '../../services/userService';

const ShareSurveyDialog = ({ open, onClose, surveyId }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [sharedUsers, setSharedUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(false); // Used for initial load and sharing actions
  const [isSearching, setIsSearching] = useState(false); // Used specifically for search operation
  const [error, setError] = useState(null);

  const loadSharedUsers = async () => {
    if (!surveyId) return;
    setIsLoading(true);
    setError(null);
    try {
      const surveyDetails = await fetchSharedUsers(surveyId);
      setSharedUsers(surveyDetails.sharedWithUsers || []);
    } catch (err) {
      console.error("Failed to fetch shared users:", err);
      setError("Failed to fetch shared users. Please try again.");
      setSharedUsers([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open && surveyId) {
      loadSharedUsers();
    } else if (!open) {
      setSharedUsers([]);
      setSearchQuery('');
      setSearchResults([]);
      setError(null);
    }
  }, [open, surveyId]); // Removed loadSharedUsers from dependencies as it's stable

  // Step 3: Search Input Handling (will be completed in the JSX part)
  // Step 4: Implement Search Execution (useEffect for searchQuery)
  useEffect(() => {
    if (searchQuery.trim() === '') {
      setSearchResults([]);
      return;
    }

    const timerId = setTimeout(async () => {
      setIsSearching(true);
      setError(null);
      try {
        const results = await searchUsers(searchQuery);
        const sharedUserIds = sharedUsers.map(user => user.id);
        const filteredResults = results.filter(user => !sharedUserIds.includes(user.id));
        setSearchResults(filteredResults);
      } catch (err) {
        console.error("Error searching users:", err);
        setError("Failed to search users. Please try again.");
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 500); // 500ms debounce

    return () => clearTimeout(timerId);
  }, [searchQuery, sharedUsers]); // Added sharedUsers dependency

  // Step 6: Handle Adding User (handleAddUser function)
  const handleAddUser = async (userToAdd) => {
    setIsLoading(true);
    setError(null);
    try {
      await shareSurvey(surveyId, userToAdd.username);
      setSearchQuery('');
      setSearchResults([]);
      await loadSharedUsers();
    } catch (err) {
      console.error(`Failed to share survey with ${userToAdd.username}:`, err);
      setError(`Failed to share survey with ${userToAdd.username}. ${err.message || ''}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemoveUser = async (userIdToRemove) => {
    setIsLoading(true);
    setError(null);
    try {
      await unshareSurvey(surveyId, userIdToRemove);
      await loadSharedUsers(); // Refresh the list
    } catch (err) {
      console.error(`Failed to remove user ${userIdToRemove} from share list:`, err);
      setError(`Failed to remove user. ${err.message || ''}`);
      // loadSharedUsers might be called here or in finally depending on desired behavior on error
    } finally {
      setIsLoading(false); // loadSharedUsers also sets this, but good to be explicit
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Share Survey</DialogTitle>
      <DialogContent>
        {/* Search Input */}
        <TextField
          autoFocus
          margin="dense"
          id="search-user"
          label="Search User"
          type="text"
          fullWidth
          variant="standard"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          disabled={isLoading}
        />

        {/* Error Display */}
        {error && (
          <Typography color="error" sx={{ mt: 1 }}>
            {error}
          </Typography>
        )}

        {/* Search Results */}
        {isSearching && <CircularProgress size={24} sx={{ display: 'block', margin: '10px auto' }} />}
        {!isSearching && searchResults.length > 0 && (
          <>
            <Typography sx={{ mt: 2, mb: 1 }}>Search Results:</Typography>
            <List dense sx={{ border: '1px solid #ccc', borderRadius: '4px', maxHeight: 150, overflow: 'auto' }}>
              {searchResults.map((user) => (
                <ListItemButton key={user.id} onClick={() => handleAddUser(user)} disabled={isLoading}>
                  <ListItemText primary={user.username} secondary={user.email || user.name} />
                </ListItemButton>
              ))}
            </List>
          </>
        )}
        {!isSearching && searchQuery.trim() !== '' && searchResults.length === 0 && (
          <Typography sx={{ mt: 2 }}>No new users found.</Typography>
        )}

        <Divider sx={{ my: 2 }} />

        {/* Shared Users List */}
        <Typography sx={{ mt: 2, mb: 1 }}>Shared With:</Typography>
        {isLoading && sharedUsers.length === 0 && !isSearching && (
          <CircularProgress size={24} sx={{ display: 'block', margin: '10px auto' }} />
        )}
        {!isLoading && sharedUsers.length > 0 && (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1, p: 1, border: '1px solid #ccc', borderRadius: '4px', maxHeight: 150, overflow: 'auto' }}>
            {sharedUsers.map((user) => (
              <Chip
                key={user.id}
                label={user.username}
                onDelete={() => handleRemoveUser(user.id)}
                deleteIcon={<ClearIcon />}
                disabled={isLoading}
              />
            ))}
          </Box>
        )}
        {!isLoading && sharedUsers.length === 0 && !isSearching && (
          <Typography sx={{ mt: 1 }}>Not shared with any users yet.</Typography>
        )}

      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isLoading}>Cancel</Button>
        {/* The main "Share" button might be repurposed or removed if sharing is done per user */}
        {/* For now, let's assume the original "Share" button is more of a "Done" or "Close" button */}
        <Button onClick={onClose} disabled={isLoading}>Done</Button>
      </DialogActions>
    </Dialog>
  );
};

export default ShareSurveyDialog;
