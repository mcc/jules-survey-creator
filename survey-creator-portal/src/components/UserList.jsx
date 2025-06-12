import React, { useState, useEffect, useCallback } from 'react';
import { getUsers, resetPassword, setUserStatus } from '../services/userService'; // Adjust path as needed
import { useNavigate } from 'react-router-dom';
import {
  Button, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Typography, Box, Alert, CircularProgress
} from '@mui/material';

const UserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null); // For general page load errors
  const [actionFeedback, setActionFeedback] = useState({ error: null, success: null });
  const navigate = useNavigate();

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getUsers();
      setUsers(data);
      // setError(null); // Clear general error on successful fetch
    } catch (err) {
      setError(err.message || 'Failed to fetch users');
      setUsers([]); // Clear users on error
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleCreateUser = () => {
    navigate('/admin/users/new');
  };

  const handleEdit = (userId) => {
    navigate(`/admin/users/${userId}/edit`);
  };

  const handleResetPassword = async (username) => {
    setActionFeedback({ error: null, success: null });
    if (window.confirm(`Are you sure you want to reset password for ${username}?`)) {
      try {
        // Consider a more specific loading state if page loading is too broad
        setLoading(true);
        const response = await resetPassword(username);
        // Assuming the backend returns a simple message string or an object with a message field
        const message = typeof response === 'string' ? response : response?.message;
        setActionFeedback({ success: message || `Password reset successfully for ${username}. A new password has been logged by the backend.` });
      } catch (err) {
        setActionFeedback({ error: err.message || `Failed to reset password for ${username}.` });
      } finally {
        setLoading(false);
      }
    }
  };

  const handleToggleActive = async (userId, isActive) => {
    setActionFeedback({ error: null, success: null });
    const action = isActive ? 'inactivate' : 'activate';
    if (window.confirm(`Are you sure you want to ${action} user ID ${userId}?`)) {
      try {
        // Consider a more specific loading state
        setLoading(true);
        await setUserStatus(userId, !isActive);
        setActionFeedback({ success: `User ${action}d successfully.` });
        fetchUsers(); // Refresh the user list
      } catch (err) {
        setActionFeedback({ error: err.message || `Failed to ${action} user.` });
      } finally {
        setLoading(false);
      }
    }
  };

  // Initial loading state for the whole page
  if (loading && users.length === 0 && !error && !actionFeedback.error && !actionFeedback.success) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>;
  }

  if (error && users.length === 0) { // Show general error only if no users are displayed
    return <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>;
  }

  return (
    <Paper sx={{ margin: 2, padding: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" component="h1">
          User Management
        </Typography>
        <Button variant="contained" color="primary" onClick={handleCreateUser}>
          Create User
        </Button>
      </Box>

      {/* Action Feedback Alerts */}
      {actionFeedback.error &&
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionFeedback({ error: null, success: null })}>
          {actionFeedback.error}
        </Alert>
      }
      {actionFeedback.success &&
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setActionFeedback({ error: null, success: null })}>
          {actionFeedback.success}
        </Alert>
      }

      {loading && <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}><CircularProgress size={30} /></Box>}


      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }} aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Active</TableCell>
              <TableCell>Roles</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.length > 0 ? (
              users.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.id}</TableCell>
                  <TableCell>{user.username}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>{user.isActive ? 'Yes' : 'No'}</TableCell>
                  <TableCell>{user.roles.map(role => role.name || role).join(', ')}</TableCell>
                  <TableCell>
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => handleEdit(user.id)}
                      sx={{ mr: 1 }}
                    >
                      Edit
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      color="secondary"
                      onClick={() => handleResetPassword(user.username)}
                      sx={{ mr: 1 }}
                    >
                      Reset Password
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      color={user.isActive ? 'error' : 'success'}
                      onClick={() => handleToggleActive(user.id, user.isActive)}
                    >
                      {user.isActive ? 'Inactivate' : 'Activate'}
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={6} align="center"> {/* Adjusted colSpan to 6 */}
                  No users found.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
};

export default UserList;
