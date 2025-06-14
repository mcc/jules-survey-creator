import React, { useState, useEffect } from 'react';
import adminService from '../../services/adminService';

// Basic styling (inline for simplicity, consider a CSS file)
const styles = {
    form: { display: 'flex', flexDirection: 'column', gap: '15px', maxWidth: '500px', margin: '20px auto', padding: '20px', border: '1px solid #ccc', borderRadius: '8px' },
    label: { fontWeight: 'bold' },
    input: { padding: '10px', border: '1px solid #ddd', borderRadius: '4px' },
    textarea: { padding: '10px', border: '1px solid #ddd', borderRadius: '4px', minHeight: '80px' },
    button: { padding: '10px 15px', cursor: 'pointer', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '4px' },
    error: { color: 'red', marginTop: '5px' }
};

const ServiceForm = ({ serviceToEdit, onFormSubmit, onCancel }) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const isEditing = Boolean(serviceToEdit);

    useEffect(() => {
        if (isEditing && serviceToEdit) {
            setName(serviceToEdit.name || '');
            setDescription(serviceToEdit.description || '');
        } else {
            setName('');
            setDescription('');
        }
    }, [serviceToEdit, isEditing]);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError('');
        setLoading(true);

        if (!name.trim()) {
            setError('Service name is required.');
            setLoading(false);
            return;
        }

        const serviceData = { name, description };

        try {
            let response;
            if (isEditing) {
                response = await adminService.updateService(serviceToEdit.id, serviceData);
            } else {
                response = await adminService.createService(serviceData);
            }
            onFormSubmit(response.data); // Pass the saved/updated service data back
        } catch (err) {
            const errorMsg = err.response?.data?.message || err.response?.data || err.message || 'Failed to save service.';
            setError(errorMsg);
            console.error("Error saving service:", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} style={styles.form}>
            <h3>{isEditing ? 'Edit Service' : 'Create New Service'}</h3>
            <div>
                <label htmlFor="serviceName" style={styles.label}>Service Name:</label>
                <input
                    id="serviceName"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    style={styles.input}
                    disabled={loading}
                />
            </div>
            <div>
                <label htmlFor="serviceDescription" style={styles.label}>Description:</label>
                <textarea
                    id="serviceDescription"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    style={styles.textarea}
                    disabled={loading}
                />
            </div>
            {error && <p style={styles.error}>{error}</p>}
            <div>
                <button type="submit" disabled={loading} style={styles.button}>
                    {loading ? (isEditing ? 'Saving...' : 'Creating...') : (isEditing ? 'Save Changes' : 'Create Service')}
                </button>
                {onCancel && (
                     <button type="button" onClick={onCancel} style={{...styles.button, backgroundColor: '#6c757d', marginLeft: '10px'}} disabled={loading}>
                        Cancel
                    </button>
                )}
            </div>
        </form>
    );
};

export default ServiceForm;
