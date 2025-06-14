import React, { useState, useEffect, useCallback } from 'react';
import adminService from '../../services/adminService';
import ServiceForm from './ServiceForm'; // Import ServiceForm

// Basic styling (inline for simplicity, consider a CSS file)
const styles = {
    container: { padding: '20px' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px' },
    th: { border: '1px solid #ddd', padding: '8px', backgroundColor: '#f2f2f2', textAlign: 'left' },
    td: { border: '1px solid #ddd', padding: '8px' },
    button: { marginRight: '10px', padding: '5px 10px', cursor: 'pointer' },
    addButton: { padding: '10px 15px', cursor: 'pointer', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px', marginBottom: '20px' }
};

const ServiceList = () => {
    const [services, setServices] = useState([]);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [serviceToEdit, setServiceToEdit] = useState(null);

    // const navigate = useNavigate(); // For navigation with React Router

    const fetchServices = useCallback(async () => {
        setLoading(true);
        try {
            const response = await adminService.getAllServices();
            setServices(response.data);
            setError('');
        } catch (err) {
            setError('Failed to fetch services. Please try again later.');
            console.error("Error fetching services:", err);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchServices();
    }, [fetchServices]);

    const handleDelete = async (serviceId) => {
        if (window.confirm('Are you sure you want to delete this service? This might fail if it has associated teams.')) {
            try {
                await adminService.deleteService(serviceId);
                fetchServices(); // Refresh list
                setError('');
            } catch (err) {
                setError(`Failed to delete service. ${err.response?.data?.message || err.response?.data || err.message || 'Please try again.'}`);
                console.error("Error deleting service:", err);
            }
        }
    };

    const handleEdit = (service) => {
        setServiceToEdit(service);
        setShowForm(true);
    };

    const handleAddNew = () => {
        setServiceToEdit(null); // Clear any previous edit state
        setShowForm(true);
    };

    const handleFormSubmit = (savedService) => {
        setShowForm(false);
        setServiceToEdit(null);
        fetchServices(); // Refresh the list
        // Optionally, show a success message
    };

    const handleFormCancel = () => {
        setShowForm(false);
        setServiceToEdit(null);
    };

    if (loading) {
        return <div style={styles.container}>Loading services...</div>;
    }

    if (error) {
        return <div style={styles.container}><p style={{ color: 'red' }}>{error}</p></div>;
    }

    return (
        <div style={styles.container}>
            <h2>Services Management</h2>

            {showForm ? (
                <ServiceForm
                    serviceToEdit={serviceToEdit}
                    onFormSubmit={handleFormSubmit}
                    onCancel={handleFormCancel}
                />
            ) : (
                <>
                    <button onClick={handleAddNew} style={styles.addButton}>Add New Service</button>
                    {services.length === 0 && !loading && <p>No services found.</p>}
                    {services.length > 0 && (
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={styles.th}>ID</th>
                                    <th style={styles.th}>Name</th>
                                    <th style={styles.th}>Description</th>
                                    <th style={styles.th}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {services.map(service => (
                                    <tr key={service.id}>
                                        <td style={styles.td}>{service.id}</td>
                                        <td style={styles.td}>{service.name}</td>
                                        <td style={styles.td}>{service.description}</td>
                                        <td style={styles.td}>
                                            <button onClick={() => handleEdit(service)} style={styles.button}>Edit</button>
                                            <button onClick={() => handleDelete(service.id)} style={{...styles.button, backgroundColor: 'red', color: 'white'}}>Delete</button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </>
            )}
        </div>
    );
};

export default ServiceList;
