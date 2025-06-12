import React, { useState, useEffect } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  IconButton,
  Typography,
  Box,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import UserForm from './UserForm'; // Import UserForm

const initialUsers = [
  { id: 1, name: 'John Doe', email: 'john.doe@example.com', role: 'Admin' },
  { id: 2, name: 'Jane Smith', email: 'jane.smith@example.com', role: 'Editor' },
  { id: 3, name: 'Peter Jones', email: 'peter.jones@example.com', role: 'Viewer' },
];

const UserList = () => {
  const [users, setUsers] = useState(initialUsers);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null); // For future edit functionality

  const handleOpenCreateForm = () => {
    setEditingUser(null); // Clear any editing state
    setIsFormOpen(true);
  };

  const handleOpenEditForm = (user) => {
    setEditingUser(user);
    setIsFormOpen(true);
  };

  const handleFormClose = () => {
    setIsFormOpen(false);
    setEditingUser(null);
  };

  const handleFormSubmit = (formData) => {
    if (editingUser) {
      // Logic for updating an existing user (for future implementation)
      console.log('Updating user:', { ...formData, id: editingUser.id });
      setUsers(users.map(user => user.id === editingUser.id ? { ...formData, id: editingUser.id } : user));
    } else {
      // Logic for creating a new user
      const newUser = { ...formData, id: users.length > 0 ? Math.max(...users.map(u => u.id)) + 1 : 1 };
      console.log('Creating new user:', newUser);
      setUsers([...users, newUser]);
    }
    handleFormClose(); // Also closes the form
  };

  const handleEditUser = (userId) => {
    // Placeholder for edit user functionality - now handled by handleOpenEditForm
    const userToEdit = users.find(user => user.id === userId);
    if (userToEdit) {
      handleOpenEditForm(userToEdit);
    }
    console.log(`Edit user with id: ${userId}`);
  };

  const handleDeleteUser = (userId) => {
    // Placeholder for delete user functionality
    console.log(`Delete user with id: ${userId}`);
    setUsers(users.filter(user => user.id !== userId));
  };

  return (
    <Paper sx={{ p: 2, margin: 'auto', maxWidth: 1000, flexGrow: 1 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" component="h2">
          User Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenCreateForm} // Changed to open form
        >
          Create User
        </Button>
      </Box>
      <TableContainer>
        <Table sx={{ minWidth: 650 }} aria-label="simple table">
          <TableHead>
            <TableRow sx={{ backgroundColor: (theme) => theme.palette.grey[200] }}>
              <TableCell>ID</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow
                key={user.id}
                sx={{ '&:last-child td, &:last-child th': { border: 0 }, '&:hover': { backgroundColor: (theme) => theme.palette.action.hover } }}
              >
                <TableCell component="th" scope="row">
                  {user.id}
                </TableCell>
                <TableCell>{user.name}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.role}</TableCell>
                <TableCell align="center">
                  <IconButton aria-label="edit" size="small" onClick={() => handleEditUser(user.id)} sx={{ mr: 0.5 }}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton aria-label="delete" size="small" onClick={() => handleDeleteUser(user.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {users.length === 0 && (
        <Typography sx={{ textAlign: 'center', mt: 3 }}>
          No users found.
        </Typography>
      )}
      <UserForm
        open={isFormOpen}
        onClose={handleFormClose}
        onSubmit={handleFormSubmit}
        initialData={editingUser} // Pass null for create, user data for edit
      />
    </Paper>
  );
};

export default UserList;
