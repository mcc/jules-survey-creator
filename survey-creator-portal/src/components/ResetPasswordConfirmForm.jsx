import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TextField, Button, Box, Typography, Alert, List, ListItem, ListItemIcon, ListItemText, CircularProgress } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { confirmPasswordReset } from '../services/userService';

// Password Policy Definitions (should mirror backend and ChangePasswordForm.jsx)
const MIN_LENGTH = 8;
const REGEX_UPPERCASE = /[A-Z]/;
const REGEX_LOWERCASE = /[a-z]/;
const REGEX_NUMBER = /[0-9]/;
const REGEX_SPECIAL_CHAR = /[!@#$%^&*()_+\-=\[\]{};':",./<>?]/;

const policyRuleText = {
  minLength: `At least ${MIN_LENGTH} characters`,
  uppercase: 'At least one uppercase letter',
  lowercase: 'At least one lowercase letter',
  number: 'At least one number',
  specialChar: 'At least one special character',
};

const ResetPasswordConfirmForm = () => {
  const { token } = useParams();
  const navigate = useNavigate();
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

  useEffect(() => {
    if (!token) {
      setError('Password reset token is missing. Please use the link from your email.');
    }
  }, [token]);

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
    if (error) setError(''); // Clear error on new input
    if (success) setSuccess(''); // Clear success message on new input
  };

  const handleConfirmPasswordChange = (e) => {
    setConfirmPassword(e.target.value);
    if (error) setError('');
  };

  const allPoliciesMet = Object.values(passwordPolicyChecks).every(Boolean);
  const passwordsMatch = newPassword === confirmPassword;
  const requiredFieldsFilled = newPassword && confirmPassword;

  const isSubmitDisabled =
    loading ||
    !token ||
    !allPoliciesMet ||
    !passwordsMatch ||
    !requiredFieldsFilled;

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (!token) {
      setError('Password reset token is missing. Cannot proceed.');
      return;
    }
    if (!requiredFieldsFilled) {
      setError('Both password fields are required.');
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
      const response = await confirmPasswordReset(token, newPassword);
      setSuccess(response.message || 'Your password has been reset successfully! You can now login.');
      setNewPassword('');
      setConfirmPassword('');
      validateNewPasswordPolicy(''); // Reset policy checks
      // Consider redirecting to login after a delay
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err) {
      const errorMessage = err?.message || err?.error || 'Failed to reset password. The token might be invalid or expired.';
      setError(errorMessage);
      console.error("Password reset confirmation error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      sx={{ mt: 4, p: 2 }}
    >
      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
          width: '100%',
          maxWidth: '450px',
          p: { xs: 2, sm: 3 },
          border: '1px solid #ccc',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        }}
      >
        <Typography variant="h5" component="h1" sx={{ textAlign: 'center', mb: 2 }}>
          Reset Your Password
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 1, width: '100%' }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 1, width: '100%' }}>{success}</Alert>}

        <TextField
          label="New Password"
          type="password"
          value={newPassword}
          onChange={handleNewPasswordChange}
          variant="outlined"
          fullWidth
          required
          disabled={loading || !!success}
        />
        {newPassword && (
          <Box sx={{ mt: 0, mb: 1 }}>
            <Typography variant="caption" component="div" sx={{ mb: 0.5 }}>Password Policy:</Typography>
            <List dense disablePadding>
              {Object.entries(passwordPolicyChecks).map(([rule, isMet]) => (
                <ListItem key={rule} dense disableGutters sx={{ py: 0.1 }}>
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
          onChange={handleConfirmPasswordChange}
          variant="outlined"
          fullWidth
          required
          disabled={loading || !!success}
          error={newPassword !== confirmPassword && confirmPassword !== ''}
          helperText={newPassword !== confirmPassword && confirmPassword !== '' ? "Passwords do not match" : ""}
        />
        <Button
          type="submit"
          variant="contained"
          color="primary"
          disabled={isSubmitDisabled || !!success} // Disable if successful to prevent resubmit
          sx={{ mt: 1, py: 1.5 }}
          fullWidth
        >
          {loading ? <CircularProgress size={24} color="inherit" /> : 'Set New Password'}
        </Button>
      </Box>
    </Box>
  );
};

export default ResetPasswordConfirmForm;
