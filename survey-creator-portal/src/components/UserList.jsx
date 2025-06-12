import React, { useState, useEffect } from 'react';
import { getUsers } from '../services/userService'; // Adjust path as needed
import { useNavigate } from 'react-router-dom';
import {
  Button, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Typography, Box, Alert, CircularProgress
} from '@mui/material';

const UserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true);
        const data = await getUsers();
        setUsers(data);
        setError(null);
      } catch (err) {
        setError(err.message || 'Failed to fetch users');
        setUsers([]); // Clear users on error
      } finally {
        setLoading(false);
      }
    };
    fetchUsers();
  }, []);

  const handleCreateUser = () => {
    navigate('/admin/users/new'); // Or your preferred route for user creation
  };

  if (loading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>;
  }

  if (error) {
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
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }} aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Active</TableCell>
              <TableCell>Roles</TableCell>
              {/* Add TableCell for Actions later */}
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
                  {/* Assuming roles is an array of objects with 'name' or an array of strings */}
                  {/* Add TableCell for Action Buttons later */}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={5} align="center">
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
