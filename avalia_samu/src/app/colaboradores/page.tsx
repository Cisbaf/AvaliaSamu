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
} from '@mui/material';
import styles from '@/components/styles/CollaboratorsPanel.module.css';
import EditIcon from '@mui/icons-material/Edit';
import CollaboratorModal from '@/components/modal/AddCollaboratorModal';
import api, { deleteGlobalCollaboratorApi } from '@/lib/api';
import { GlobalCollaborator, Collaborator } from "@/types/project"
import { useProjects } from '@/context/ProjectContext';
import { Delete } from '@mui/icons-material';
import AddIcon from '@mui/icons-material/Add';
import ConfirmationDialog from '@/components/modal/ConfirmationModal';

export default function CollaboratorsPage() {
    const [collaborators, setCollaborators] = useState<GlobalCollaborator[]>([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedCollaborator, setSelectedCollaborator] = useState<GlobalCollaborator | undefined>();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('all');
    const [deleteConfirmationOpen, setDeleteConfirmationOpen] = useState(false);
    const [collaboratorToDelete, setCollaboratorToDelete] = useState<string | null>(null);

    const { } = useProjects();

    const memoizedRoles = useMemo(() => {
        const roles = collaborators
            .map(c => c.role)
            .filter(role => typeof role === 'string' && role !== 'MEDICO');

        const medicoRoles = collaborators
            .map(c => c.medicoRole)
            .filter(medicoRole => typeof medicoRole === 'string');

        return [...new Set([...roles, ...medicoRoles])];
    }, [collaborators]);



    const loadCollaborators = async () => {
        try {
            setLoading(true);
            const response = await api.get('/collaborator');
            setCollaborators(response.data as GlobalCollaborator[]);
            setError(null);
        } catch (error: any) {
            console.error('Failed to load collaborators:', error);
            setError(error.response?.data?.message || 'Falha ao carregar colaboradores');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCollaborators();
    }, []);

    const handleDeleteClick = useCallback((id: string) => {
        setCollaboratorToDelete(id);
        setDeleteConfirmationOpen(true);
    }, []);

    const handleConfirmDelete = useCallback(async () => {
        if (!collaboratorToDelete) return;

        try {
            setLoading(true);
            setError(null);
            await deleteGlobalCollaboratorApi(collaboratorToDelete);
            await loadCollaborators();
        } catch (error: any) {
            console.error('Failed to delete collaborator:', error);
            setError(error.response?.data?.message || 'Falha ao excluir colaborador');
        } finally {
            setLoading(false);
            setDeleteConfirmationOpen(false);
            setCollaboratorToDelete(null);
        }
    }, [collaboratorToDelete, deleteGlobalCollaboratorApi, loadCollaborators]);

    const handleCloseDeleteDialog = useCallback(() => {
        setDeleteConfirmationOpen(false);
        setCollaboratorToDelete(null);
    }, []);

    const filteredCollaborators = useMemo(() => {
        return collaborators.filter(collaborator => {
            const nameMatch = typeof collaborator.nome === 'string' && collaborator.nome.toLowerCase().includes(searchTerm.toLowerCase());
            const roleMatch = filterRole === 'all' || (typeof collaborator.role === 'string' && collaborator.role === filterRole || typeof collaborator.medicoRole === 'string' && collaborator.medicoRole === filterRole);
            const passesSearch = searchTerm === '' || nameMatch;
            return passesSearch && roleMatch;
        });
    }, [collaborators, searchTerm, filterRole]);

    return (
        <div style={{ margin: '15px' }}>
            {error && (
                <Alert severity="error" className="mb-4">
                    {error}
                </Alert>
            )}

            {loading && !error && collaborators.length === 0 && (
                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '20px' }}>
                    <CircularProgress />
                </div>
            )}

            {!loading && !error && (
                <>
                    <div className={styles.filters}>
                        <TextField
                            placeholder="Pesquisar por nome"
                            variant="standard"
                            size="small"
                            sx={{ mr: 2, width: '300px' }}
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                        <Button
                            variant="contained"
                            onClick={() => {
                                setSelectedCollaborator(undefined);
                                setModalOpen(true)
                            }}
                            className={styles.addButton}
                            startIcon={<AddIcon />}
                            sx={{ mr: 2 }}
                        >
                            Novo Colaborador
                        </Button>

                        <Select
                            value={filterRole}
                            className={styles.roleSelect}
                            onChange={(e) => setFilterRole(e.target.value)}
                            size="small"
                            displayEmpty
                            sx={{ minWidth: 150 }}
                        >
                            <MenuItem value="all">Todas as funções</MenuItem>
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
                                    <TableCell sx={{ fontWeight: 'bold' }}>ID CallRote</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>CPF</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>Função</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>Ações</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
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
                                        <TableRow key={collaborator.id}>
                                            <TableCell>{collaborator.nome}</TableCell>
                                            <TableCell>{collaborator.idCallRote}</TableCell>
                                            <TableCell>
                                                {collaborator.cpf ? collaborator.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4') : ''}
                                            </TableCell>
                                            <TableCell>
                                                {(collaborator.role ?? '') + (collaborator.medicoRole ? ` (${collaborator.medicoRole} - ${collaborator.shiftHours})` : '')}
                                            </TableCell>
                                            <TableCell>
                                                <IconButton
                                                    onClick={() => {
                                                        setSelectedCollaborator(collaborator);
                                                        setModalOpen(true);
                                                    }}
                                                    aria-label={`editar ${collaborator.nome}`}
                                                    disabled={loading}
                                                >
                                                    <EditIcon color="primary" />
                                                </IconButton>
                                                <IconButton
                                                    onClick={() => handleDeleteClick(collaborator.id!)}
                                                    aria-label={`excluir ${collaborator.nome}`}
                                                    disabled={loading}
                                                >
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

            <CollaboratorModal
                open={modalOpen}
                onClose={() => {
                    setModalOpen(false);
                    setSelectedCollaborator(undefined);
                }}
                onSuccess={loadCollaborators}
                initialData={selectedCollaborator}
            />

            <ConfirmationDialog
                open={deleteConfirmationOpen}
                onClose={handleCloseDeleteDialog}
                onConfirm={handleConfirmDelete}
                title="Confirmar Exclusão"
                message="Você tem certeza que deseja remover este colaborador?"
                confirmText="Remover"
            />
        </div>
    );
}