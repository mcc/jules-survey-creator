import React, { useState, useEffect, useCallback } from 'react';
import adminService from '../../services/adminService';

// Basic styling
const styles = {
    form: { display: 'flex', flexDirection: 'column', gap: '15px', maxWidth: '500px', margin: '20px auto', padding: '20px', border: '1px solid #ccc', borderRadius: '8px' },
    label: { fontWeight: 'bold' },
    input: { padding: '10px', border: '1px solid #ddd', borderRadius: '4px' },
    textarea: { padding: '10px', border: '1px solid #ddd', borderRadius: '4px', minHeight: '80px' },
    select: { padding: '10px', border: '1px solid #ddd', borderRadius: '4px' },
    button: { padding: '10px 15px', cursor: 'pointer', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '4px' },
    error: { color: 'red', marginTop: '5px' }
};

const TeamForm = ({ teamToEdit, onFormSubmit, onCancel }) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [selectedServiceId, setSelectedServiceId] = useState('');
    const [services, setServices] = useState([]);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [loadingServices, setLoadingServices] = useState(true);

    const isEditing = Boolean(teamToEdit);

    const fetchServices = useCallback(async () => {
        setLoadingServices(true);
        try {
            const response = await adminService.getAllServices();
            setServices(response.data);
            if (response.data.length > 0 && !isEditing) {
                // setSelectedServiceId(response.data[0].id); // Default to first service if creating
            }
        } catch (err) {
            console.error("Error fetching services for dropdown:", err);
            setError('Failed to load services for selection.'); // Show error related to services loading
        } finally {
            setLoadingServices(false);
        }
    }, [isEditing]); // Removed setSelectedServiceId from dependency array to avoid re-triggering

    useEffect(() => {
        fetchServices();
    }, [fetchServices]);

    useEffect(() => {
        if (isEditing && teamToEdit) {
            setName(teamToEdit.name || '');
            setDescription(teamToEdit.description || '');
            setSelectedServiceId(teamToEdit.serviceId || '');
        } else {
            // Reset form for new entry
            setName('');
            setDescription('');
            // Keep selectedServiceId as is, or set to default if services are loaded
             if (services.length > 0) {
                // setSelectedServiceId(services[0].id); // Optionally default to first service
             } else {
                setSelectedServiceId('');
             }
        }
    }, [teamToEdit, isEditing, services]); // Added services to deps for default selection logic

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError('');
        setLoading(true);

        if (!name.trim()) {
            setError('Team name is required.');
            setLoading(false);
            return;
        }
        if (!selectedServiceId) {
            setError('A service must be selected for the team.');
            setLoading(false);
            return;
        }

        const teamData = { name, description, serviceId: selectedServiceId };

        try {
            let response;
            if (isEditing) {
                response = await adminService.updateTeam(teamToEdit.id, teamData);
            } else {
                response = await adminService.createTeam(teamData);
            }
            onFormSubmit(response.data);
        } catch (err) {
            const errorMsg = err.response?.data?.message || err.response?.data || err.message || 'Failed to save team.';
            setError(errorMsg);
            console.error("Error saving team:", err);
        } finally {
            setLoading(false);
        }
    };

    if (loadingServices) {
        return <div style={styles.form}>Loading services for form...</div>;
    }

    return (
        <form onSubmit={handleSubmit} style={styles.form}>
            <h3>{isEditing ? 'Edit Team' : 'Create New Team'}</h3>
            <div>
                <label htmlFor="teamName" style={styles.label}>Team Name:</label>
                <input
                    id="teamName"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    style={styles.input}
                    disabled={loading}
                />
            </div>
            <div>
                <label htmlFor="teamDescription" style={styles.label}>Description:</label>
                <textarea
                    id="teamDescription"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    style={styles.textarea}
                    disabled={loading}
                />
            </div>
            <div>
                <label htmlFor="teamService" style={styles.label}>Service:</label>
                <select
                    id="teamService"
                    value={selectedServiceId}
                    onChange={(e) => setSelectedServiceId(e.target.value)}
                    style={styles.select}
                    disabled={loading || loadingServices}
                >
                    <option value="">Select a Service</option>
                    {services.map(service => (
                        <option key={service.id} value={service.id}>
                            {service.name}
                        </option>
                    ))}
                </select>
            </div>
            {error && <p style={styles.error}>{error}</p>}
            <div>
                <button type="submit" disabled={loading || loadingServices} style={styles.button}>
                    {loading ? (isEditing ? 'Saving...' : 'Creating...') : (isEditing ? 'Save Changes' : 'Create Team')}
                </button>
                {onCancel && (
                     <button type="button" onClick={onCancel} style={{...styles.button, backgroundColor: '#6c757d', marginLeft: '10px'}} disabled={loading || loadingServices}>
                        Cancel
                    </button>
                )}
            </div>
        </form>
    );
};

export default TeamForm;
