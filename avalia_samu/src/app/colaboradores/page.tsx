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
import styles from '@/components/styles/CollaboratorsPanel.module.css';

import EditIcon from '@mui/icons-material/Edit';
import CollaboratorModal from '@/components/AddCollaboratorModal';
import api, { deleteGlobalCollaboratorApi } from '@/lib/api';
import { Collaborator } from "@/types/project"
import { useProjects } from '@/context/ProjectContext';
import { Delete } from '@mui/icons-material';
import AddIcon from '@mui/icons-material/Add';


export default function CollaboratorsPage() {
    const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedCollaborator, setSelectedCollaborator] = useState<Collaborator | undefined>();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('all');

    const {
        globalCollaborators,
        actions: {

        }
    } = useProjects();


    const memoizedRoles = useMemo(() =>
        [...new Set(globalCollaborators.map(c => c.role))],
        [globalCollaborators]
    );


    const loadCollaborators = async () => {
        try {
            setLoading(true);
            const response = await api.get('/collaborator');
            setCollaborators(response.data as Collaborator[]);
            setError(null);
        } catch (error: any) {
            setError(error.response?.data?.message || 'Falha ao carregar colaboradores');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCollaborators();
    }, []);

    const handleSave = async (data: Collaborator) => {
        try {
            setError(null);
            setLoading(true);
            if (selectedCollaborator) {
                await api.put(`/collaborator/${data.id}`, data);

            } else {
                await api.post('/collaborator', data);

            }
            await loadCollaborators(); // Reload collaborators after save
            setModalOpen(false);
            setSelectedCollaborator(undefined); // Reset selected collaborator
        } catch (error: any) {
            setError(error.response?.data?.message || (selectedCollaborator
                ? 'Falha ao atualizar colaborador'
                : 'Falha ao criar novo colaborador'));
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = useCallback(async (id: string) => {
        try {
            await deleteGlobalCollaboratorApi(id);
            await loadCollaborators();
        } catch (error: any) {
            setError(error.response?.data?.message || 'Failed to delete collaborator');
        }

    }, [deleteGlobalCollaboratorApi, loadCollaborators]);

    const filteredCollaborators = useMemo(() => {
        return collaborators.filter(collaborator => {
            const nameMatch = collaborator.nome;
            const roleMatch = filterRole === 'all' || collaborator.role === filterRole;
            return nameMatch && roleMatch;
        });
    }, [collaborators, searchTerm, filterRole]);

    return (
        <div className="p-6 max-w-6xl mx-auto">


            {loading && <CircularProgress />}

            {error && (
                <Alert severity="error" className="mb-4">
                    {error}
                </Alert>
            )}

            {!loading && !error && (
                <TableContainer component={Paper}>
                    <div className={styles.filters} >
                        <TextField
                            placeholder="Pesquisar por nome"
                            variant="outlined"
                            size="small"
                            fullWidth
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                        <Button
                            variant="contained"
                            onClick={() => {
                                setSelectedCollaborator(undefined); // Ensure new collaborator form
                                setModalOpen(true)
                            }}
                            className={styles.addButton}><AddIcon /></Button>

                        <Select
                            value={filterRole}
                            className={styles.roleSelect}
                            onChange={(e) => setFilterRole(e.target.value)}
                            size="small"
                            style={{ marginRight: '15px' }}
                        >
                            <MenuItem value="all">Todas as funções</MenuItem>
                            {memoizedRoles.map(role => (
                                <MenuItem key={role} value={role}>{role}</MenuItem>
                            ))}
                        </Select>

                    </div>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Nome</TableCell>
                                <TableCell>CPF</TableCell>
                                <TableCell>Função</TableCell>
                                <TableCell>Pontuação</TableCell>
                                <TableCell>Ações</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {filteredCollaborators.map((collaborator) => (
                                <TableRow key={collaborator.id}>
                                    <TableCell>{collaborator.nome}</TableCell>
                                    <TableCell>
                                        {collaborator.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')}
                                    </TableCell>
                                    <TableCell>{collaborator.role}</TableCell>
                                    <TableCell>{collaborator.pontuacao}</TableCell>
                                    <TableCell>
                                        <IconButton
                                            onClick={() => {
                                                setSelectedCollaborator(collaborator);
                                                setModalOpen(true);
                                            }}
                                        >
                                            <EditIcon color="primary" />
                                        </IconButton>
                                        <IconButton onClick={() => handleDelete(collaborator.id!)}>
                                            <Delete color="error" />
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}


            <CollaboratorModal
                open={modalOpen}
                onClose={() => {
                    setModalOpen(false);
                    setSelectedCollaborator(undefined);
                }}
                onSuccess={() => {
                    loadCollaborators();
                    console.log('Operation successful');
                }}
                initialData={selectedCollaborator}
            />
        </div>
    );
}

