import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  List,
} from '@mui/material';

const ShareSurveyDialog = ({ open, onClose }) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Share Survey</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          id="search-user"
          label="Search User"
          type="text"
          fullWidth
          variant="standard"
        />
        <List sx={{ mt: 2 }}>
          {/* User list items will be rendered here */}
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={onClose}>Share</Button>
      </DialogActions>
    </Dialog>
  );
};

export default ShareSurveyDialog;
