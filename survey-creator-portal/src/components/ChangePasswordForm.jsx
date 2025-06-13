import React, { useState, useEffect } from 'react';
import { TextField, Button, Box, Typography, Alert, List, ListItem, ListItemIcon, ListItemText } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { changeCurrentUserPassword } from '../services/userService'; // Ensure path is correct

// Password Policy Definitions
const MIN_LENGTH = 8;
const REGEX_UPPERCASE = /[A-Z]/;
const REGEX_LOWERCASE = /[a-z]/;
const REGEX_NUMBER = /[0-9]/;
const REGEX_SPECIAL_CHAR = /[!@#$%^&*()_+\-=\[\]{};':",./<>?]/; // Mirrored from backend

const policyRuleText = {
  minLength: `At least ${MIN_LENGTH} characters`,
  uppercase: 'At least one uppercase letter',
  lowercase: 'At least one lowercase letter',
  number: 'At least one number',
  specialChar: 'At least one special character',
};

const ChangePasswordForm = () => {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [passwordPolicyChecks, setPasswordPolicyChecks] = useState({
    minLength: false,
    uppercase: false,
    lowercase: false,
    number: false,
    specialChar: false,
  });

  const validateNewPasswordPolicy = (password) => {
    setPasswordPolicyChecks({
      minLength: password.length >= MIN_LENGTH,
      uppercase: REGEX_UPPERCASE.test(password),
      lowercase: REGEX_LOWERCASE.test(password),
      number: REGEX_NUMBER.test(password),
      specialChar: REGEX_SPECIAL_CHAR.test(password),
    });
  };

  const handleNewPasswordChange = (e) => {
    const currentNewPassword = e.target.value;
    setNewPassword(currentNewPassword);
    validateNewPasswordPolicy(currentNewPassword);
  };

  // Clear success and error messages when passwords change
  useEffect(() => {
    if (oldPassword || newPassword || confirmPassword) {
      setError('');
      setSuccess('');
    }
  }, [oldPassword, newPassword, confirmPassword]);

  const allPoliciesMet = Object.values(passwordPolicyChecks).every(Boolean);
  const passwordsMatch = newPassword === confirmPassword;
  const requiredFieldsFilled = oldPassword && newPassword && confirmPassword;

  const isSubmitDisabled =
    loading ||
    !allPoliciesMet ||
    !passwordsMatch ||
    !requiredFieldsFilled;

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(''); // Clear previous errors
    setSuccess(''); // Clear previous success messages

    // Redundant checks due to button disable logic, but good for safety if form submitted via other means (e.g. enter key)
    if (!requiredFieldsFilled) {
      setError('All fields are required.');
      return;
    }
    if (!passwordsMatch) {
      setError('New passwords do not match.');
      return;
    }
    if (!allPoliciesMet) {
      setError('Password does not meet all policy requirements.');
      return;
    }

    setLoading(true);
    try {
      const response = await changeCurrentUserPassword({ oldPassword, newPassword });
      setSuccess(response.message || 'Password changed successfully!');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      // Reset policy checks for the now empty new password field
      validateNewPasswordPolicy('');
    } catch (err) {
      const errorMessage = err?.message || err?.error || 'Failed to change password. Please try again.';
      setError(errorMessage);
      console.error("Password change error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
        maxWidth: '450px', // Adjust as needed for checklist
        p: 2,
        border: '1px solid #ccc',
        borderRadius: '4px'
      }}
    >
      <Typography variant="h6" component="h3" sx={{ mb: 1 }}>
        Change Password
      </Typography>
      {error && <Alert severity="error" sx={{ mb:1}}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb:1}}>{success}</Alert>}
      <TextField
        label="Current Password"
        type="password"
        value={oldPassword}
        onChange={(e) => setOldPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <TextField
        label="New Password"
        type="password"
        value={newPassword}
        onChange={handleNewPasswordChange}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      {newPassword && ( // Only show checklist if newPassword is not empty
        <Box sx={{ mt: 0, mb: 1 }}>
          <Typography variant="caption" component="div" sx={{ mb: 0.5 }}>Password Policy:</Typography>
          <List dense disablePadding>
            {Object.entries(passwordPolicyChecks).map(([rule, isMet]) => (
              <ListItem key={rule} dense disableGutters sx={{py: 0.1}}>
                <ListItemIcon sx={{ minWidth: '28px' }}>
                  {isMet ? <CheckCircleIcon color="success" sx={{ fontSize: '1.1rem' }} /> : <CancelIcon color="error" sx={{ fontSize: '1.1rem' }} />}
                </ListItemIcon>
                <ListItemText
                  primary={policyRuleText[rule]}
                  primaryTypographyProps={{ variant: 'caption', color: isMet ? 'success.main' : 'error.main' }}
                />
              </ListItem>
            ))}
          </List>
        </Box>
      )}
      <TextField
        label="Confirm New Password"
        type="password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
        error={newPassword !== confirmPassword && confirmPassword !== ''}
        helperText={newPassword !== confirmPassword && confirmPassword !== '' ? "Passwords do not match" : ""}
      />
      <Button
        type="submit"
        variant="contained"
        color="primary"
        disabled={isSubmitDisabled}
        sx={{ mt: 1 }}
      >
        {loading ? 'Changing...' : 'Change Password'}
      </Button>
    </Box>
  );
};

export default ChangePasswordForm;
=======
import React, { useState } from 'react';
import { TextField, Button, Box, Typography, Alert } from '@mui/material';
import { changeCurrentUserPassword } from '../services/userService'; // Ensure path is correct

const ChangePasswordForm = () => {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (!oldPassword || !newPassword || !confirmPassword) {
      setError('All fields are required.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.');
      return;
    }

    // Optional: Add client-side complexity rules for newPassword here if desired
    // For example:
    // if (newPassword.length < 8) {
    //   setError('New password must be at least 8 characters long.');
    //   return;
    // }

    setLoading(true);
    try {
      const response = await changeCurrentUserPassword({ oldPassword, newPassword });
      setSuccess(response.message || 'Password changed successfully!'); // Assuming backend returns a message field
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const errorMessage = err?.message || err?.error || 'Failed to change password. Please try again.';
      setError(errorMessage);
      console.error("Password change error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
        maxWidth: '400px', // Adjust as needed
        p: 2,
        border: '1px solid #ccc', // Optional: for visual separation
        borderRadius: '4px'     // Optional
      }}
    >
      <Typography variant="h6" component="h3">
        Change Password
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}
      <TextField
        label="Current Password"
        type="password"
        value={oldPassword}
        onChange={(e) => setOldPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <TextField
        label="New Password"
        type="password"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <TextField
        label="Confirm New Password"
        type="password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <Button
        type="submit"
        variant="contained"
        color="primary"
        disabled={loading}
        sx={{ mt: 1 }}
      >
        {loading ? 'Changing...' : 'Change Password'}
      </Button>
    </Box>
  );
};

export default ChangePasswordForm;
