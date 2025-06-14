import React, { useState, useEffect, useCallback } from 'react';
import adminService from '../../services/adminService';
import TeamForm from './TeamForm'; // Import TeamForm

// Basic styling (inline for simplicity, consider a CSS file)
const styles = {
    container: { padding: '20px' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px' },
    th: { border: '1px solid #ddd', padding: '8px', backgroundColor: '#f2f2f2', textAlign: 'left' },
    td: { border: '1px solid #ddd', padding: '8px' },
    button: { marginRight: '10px', padding: '5px 10px', cursor: 'pointer' },
    addButton: { padding: '10px 15px', cursor: 'pointer', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px', marginBottom: '20px' }
    // Add style for filter dropdown if implemented
};

const TeamList = () => {
    const [teams, setTeams] = useState([]);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [teamToEdit, setTeamToEdit] = useState(null);
    // const [services, setServices] = useState([]); // For service filter dropdown - TeamForm handles its own service fetching
    // const [selectedServiceFilter, setSelectedServiceFilter] = useState(''); // For service filter

    const fetchTeams = useCallback(async () => {
        setLoading(true);
        try {
            // Later, this could be adminService.getTeamsByService(selectedServiceFilter) if a filter is active
            const response = await adminService.getAllTeams();
            setTeams(response.data);
            setError('');
        } catch (err) {
            setError('Failed to fetch teams. Please try again later.');
            console.error("Error fetching teams:", err);
        } finally {
            setLoading(false);
        }
    }, []); // Add selectedServiceFilter to dependency array if using filter

    // Fetch services for filter dropdown (optional for now)
    // useEffect(() => {
    //     const fetchServicesForFilter = async () => {
    //         try {
    //             const response = await adminService.getAllServices();
    //             setServices(response.data);
    //         } catch (err) {
    //             console.error("Error fetching services for filter:", err);
    //         }
    //     };
    //     fetchServicesForFilter();
    // }, []);

    useEffect(() => {
        fetchTeams();
    }, [fetchTeams]);

    const handleDelete = async (teamId) => {
        if (window.confirm('Are you sure you want to delete this team? This may affect user assignments.')) {
            try {
                await adminService.deleteTeam(teamId);
                fetchTeams(); // Refresh list
                setError('');
            } catch (err) {
                setError(`Failed to delete team. ${err.response?.data?.message || err.response?.data || err.message || 'Please try again.'}`);
                console.error("Error deleting team:", err);
            }
        }
    };

    const handleEdit = (team) => {
        setTeamToEdit(team);
        setShowForm(true);
    };

    const handleAddNew = () => {
        setTeamToEdit(null); // Clear any previous edit state
        setShowForm(true);
    };

    const handleFormSubmit = (savedTeam) => {
        setShowForm(false);
        setTeamToEdit(null);
        fetchTeams(); // Refresh the list
        // Optionally, show a success message like: alert(`${savedTeam.name} has been saved successfully!`);
    };

    const handleFormCancel = () => {
        setShowForm(false);
        setTeamToEdit(null);
    };

    if (loading) {
        return <div style={styles.container}>Loading teams...</div>;
    }

    if (error) {
        return <div style={styles.container}><p style={{ color: 'red' }}>{error}</p></div>;
    }

    return (
        <div style={styles.container}>
            <h2>Teams Management</h2>

            {/* Optional: Service Filter Dropdown could be added here later */}

            {showForm ? (
                <TeamForm
                    teamToEdit={teamToEdit}
                    onFormSubmit={handleFormSubmit}
                    onCancel={handleFormCancel}
                />
            ) : (
                <>
                    <button onClick={handleAddNew} style={styles.addButton}>Add New Team</button>
                    {teams.length === 0 && !loading && <p>No teams found.</p>}
                    {teams.length > 0 && (
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={styles.th}>ID</th>
                                    <th style={styles.th}>Name</th>
                                    <th style={styles.th}>Description</th>
                                    <th style={styles.th}>Service Name</th>
                                    <th style={styles.th}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {teams.map(team => (
                                    <tr key={team.id}>
                                        <td style={styles.td}>{team.id}</td>
                                        <td style={styles.td}>{team.name}</td>
                                        <td style={styles.td}>{team.description}</td>
                                        <td style={styles.td}>{team.serviceName || 'N/A'}</td>
                                        <td style={styles.td}>
                                            <button onClick={() => handleEdit(team)} style={styles.button}>Edit</button>
                                            <button onClick={() => handleDelete(team.id)} style={{...styles.button, backgroundColor: 'red', color: 'white'}}>Delete</button>
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

export default TeamList;
