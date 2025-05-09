'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    CircularProgress,
    Alert,
    TextField,
    Select,
    MenuItem,
    IconButton,
    Button,
    SvgIcon
} from '@mui/material';
import styles from '@/components/styles/CollaboratorsPanel.module.css'; // Assuming shared styles

import EditIcon from '@mui/icons-material/Edit';
// Ensure this modal component expects GlobalCollaborator for initialData
import CollaboratorModal from '@/components/AddCollaboratorModal';
import api, { deleteGlobalCollaboratorApi } from '@/lib/api';
// Import GlobalCollaborator and potentially MedicoRole/ShiftHours if needed for roles list
import { GlobalCollaborator, Collaborator } from "@/types/project" // Keep Collaborator if it's a distinct project type
import { useProjects } from '@/context/ProjectContext';
import { Delete } from '@mui/icons-material';
import AddIcon from '@mui/icons-material/Add';


export default function CollaboratorsPage() {
    // State should hold GlobalCollaborator objects as this page manages the global list
    const [collaborators, setCollaborators] = useState<GlobalCollaborator[]>([]);
    const [modalOpen, setModalOpen] = useState(false);
    // Selected collaborator for editing should also be GlobalCollaborator
    const [selectedCollaborator, setSelectedCollaborator] = useState<GlobalCollaborator | undefined>();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('all');

    const {
        // You are using globalCollaborators from context, let's rely on the local fetch for the table data for consistency on this page
        // globalCollaborators,
        // actions: { ... } // Context actions related to projects might not be needed here
    } = useProjects();

    // Use the collaborators fetched by this component to derive roles for the filter
    const memoizedRoles = useMemo(() => {
        // Ensure roles are strings before creating a Set
        return [...new Set(collaborators.map(c => c.role).filter(role => typeof role === 'string'))];
    }, [collaborators]);


    const loadCollaborators = async () => {
        try {
            setLoading(true);
            const response = await api.get('/collaborator');
            // Cast the response data to GlobalCollaborator[]
            setCollaborators(response.data as GlobalCollaborator[]);
            setError(null);
        } catch (error: any) {
            console.error('Failed to load collaborators:', error); // Log the error detail
            setError(error.response?.data?.message || 'Falha ao carregar colaboradores');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCollaborators();
    }, []); // Empty dependency array means this runs once on mount

    // handleSave receives data from the modal, which is typed as GlobalCollaborator
    const handleSave = async (data: GlobalCollaborator) => {
        try {
            setError(null);
            setLoading(true); // Indicate loading for the save operation
            if (selectedCollaborator) {
                // Ensure the data sent for update conforms to the API's expected input
                // The modal's onSuccess should already provide data with isGlobal: true and pontuacao
                const payload: Omit<GlobalCollaborator, 'id'> & { isGlobal: true } = {
                    ...data, // Spread the data received from the modal
                    isGlobal: true, // Explicitly ensure isGlobal is true as required by the endpoint
                };
                // If the modal's data already has the correct pontuacao, you might not need to override
                // payload.pontuacao = data.pontuacao; // Or initialData!.pontuacao if not editable in modal

                await api.put(`/collaborator/${data.id}`, payload);

            } else {
                // Ensure the data sent for creation conforms to the API's expected input
                const payload: Omit<GlobalCollaborator, 'id'> & { isGlobal: true } = {
                    ...data, // Spread the data received from the modal
                    pontuacao: data.pontuacao ?? 0, // Default pontuacao if not set in modal data
                    isGlobal: true, // Explicitly ensure isGlobal is true
                };
                await api.post('/collaborator', payload);
            }
            await loadCollaborators(); // Reload collaborators after save
            setModalOpen(false); // Close modal
            setSelectedCollaborator(undefined); // Reset selected collaborator state
        } catch (error: any) {
            console.error('Save operation failed:', error); // Log error detail
            setError(error.response?.data?.message || (selectedCollaborator
                ? 'Falha ao atualizar colaborador'
                : 'Falha ao criar novo colaborador'));
        } finally {
            setLoading(false); // Unset loading state
        }
    };

    const handleDelete = useCallback(async (id: string) => {
        // Add confirmation prompt here if desired
        if (!id) {
            console.error("Cannot delete collaborator without an ID");
            return;
        }
        try {
            setLoading(true); // Indicate loading for delete
            setError(null); // Clear previous errors
            await deleteGlobalCollaboratorApi(id);
            await loadCollaborators(); // Reload collaborators after delete
        } catch (error: any) {
            console.error('Failed to delete collaborator:', error); // Log error detail
            setError(error.response?.data?.message || 'Falha ao excluir colaborador');
        } finally {
            setLoading(false); // Unset loading state
        }

    }, [deleteGlobalCollaboratorApi, loadCollaborators]);

    const filteredCollaborators = useMemo(() => {
        return collaborators.filter(collaborator => {
            // Check if nome exists and is a string before calling toLowerCase
            const nameMatch = typeof collaborator.nome === 'string' && collaborator.nome.toLowerCase().includes(searchTerm.toLowerCase());

            // Check if role exists and is a string before checking equality
            const roleMatch = filterRole === 'all' || (typeof collaborator.role === 'string' && collaborator.role === filterRole);

            // The name filter needs to search against the searchTerm, not just check truthiness
            const passesSearch = searchTerm === '' || nameMatch;


            return passesSearch && roleMatch;
        });
    }, [collaborators, searchTerm, filterRole]); // searchTerm is now a dependency

    return (
        <div className="p-6 max-w-6xl mx-auto">
            {/* Use a single loading indicator if actions are modal-based, or specific ones for table load/delete */}
            {/* {loading && <CircularProgress />} */} {/* This might hide everything */}

            {error && (
                <Alert severity="error" className="mb-4">
                    {error}
                </Alert>
            )}

            {/* Show loading spinner over the table or centrally while fetching */}
            {loading && !error && collaborators.length === 0 && (
                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '20px' }}>
                    <CircularProgress />
                </div>
            )}


            {!loading && !error && ( // Only show table container when not initially loading and no global error
                <>
                    <div className={styles.filters} >
                        <TextField
                            placeholder="Pesquisar por nome"
                            variant="outlined"
                            size="small"
                            // fullWidth // Might not want fullWidth if other filters/buttons are next to it
                            sx={{ mr: 2, width: '300px' }} // Add margin right and width
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                        {/* Add New Global Collaborator Button */}
                        <Button
                            variant="contained"
                            onClick={() => {
                                setSelectedCollaborator(undefined); // Ensure new collaborator form
                                setModalOpen(true)
                            }}
                            className={styles.addButton} // Ensure this class applies appropriate styling/margin
                            startIcon={<AddIcon />}
                            sx={{ mr: 2 }} // Add margin right
                        >
                            Novo Colaborador
                        </Button>

                        <Select
                            value={filterRole}
                            className={styles.roleSelect} // Ensure this class handles width/margin
                            onChange={(e) => setFilterRole(e.target.value)}
                            size="small"
                            displayEmpty // Show empty value
                            sx={{ minWidth: 150 }} // Give the select a minimum width
                        >
                            <MenuItem value="all">Todas as funções</MenuItem>
                            {/* Map over roles derived from fetched global collaborators */}
                            {memoizedRoles.map(role => (
                                <MenuItem key={role} value={role}>{role}</MenuItem>
                            ))}
                        </Select>

                    </div>
                    <TableContainer component={Paper}>
                        <Table stickyHeader aria-label="global collaborators table">
                            <TableHead>
                                <TableRow>
                                    <TableCell sx={{ fontWeight: 'bold' }}>Nome</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>CPF</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>Função</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>Pontuação</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>Ações</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {/* Show spinner row if loading state applies to table updates */}
                                {loading && collaborators.length > 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={5} align="center">
                                            <CircularProgress size={30} />
                                        </TableCell>
                                    </TableRow>
                                ) : filteredCollaborators.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={5} align="center">
                                            Nenhum colaborador encontrado.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    filteredCollaborators.map((collaborator) => (
                                        // Use the global collaborator ID as the key
                                        <TableRow key={collaborator.id}>
                                            <TableCell>{collaborator.nome}</TableCell>
                                            <TableCell>
                                                {/* Add null check before replace if CPF might be null/undefined */}
                                                {collaborator.cpf ? collaborator.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4') : ''}
                                            </TableCell>
                                            <TableCell>{collaborator.role}</TableCell>
                                            <TableCell>{collaborator.pontuacao}</TableCell>
                                            <TableCell>
                                                {/* Edit Button */}
                                                <IconButton
                                                    onClick={() => {
                                                        setSelectedCollaborator(collaborator); // Set the selected global collaborator
                                                        setModalOpen(true); // Open the modal
                                                    }}
                                                    aria-label={`editar ${collaborator.nome}`}
                                                    disabled={loading} // Disable actions while saving/deleting
                                                >
                                                    <EditIcon color="primary" />
                                                </IconButton>
                                                {/* Delete Button */}
                                                <IconButton
                                                    onClick={() => handleDelete(collaborator.id!)} // Pass the global collaborator ID
                                                    aria-label={`excluir ${collaborator.nome}`}
                                                    disabled={loading} // Disable actions while saving/deleting
                                                >
                                                    {/* Show spinner on delete button if loading */}
                                                    {loading && selectedCollaborator?.id === collaborator.id ? <CircularProgress size={20} color="error" /> : <Delete color="error" />}
                                                </IconButton>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </>
            )}


            {/* Collaborator Modal (used for both Add New and Edit) */}
            <CollaboratorModal
                open={modalOpen}
                onClose={() => {
                    setModalOpen(false);
                    setSelectedCollaborator(undefined); // Clear selected collaborator on close
                }}
                onSuccess={loadCollaborators} // Reload collaborators after successful save/create
                initialData={selectedCollaborator} // Pass the selected global collaborator data for editing (undefined for new)
            // projectId is not needed here as this modal manages GLOBAL collaborators
            // projectId={undefined} // Explicitly pass undefined or just omit
            />
        </div>
    );
}