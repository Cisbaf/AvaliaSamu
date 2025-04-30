'use client';

import { useState, useEffect, useMemo } from 'react';
import {
    Button,
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
    MenuItem
} from '@mui/material';
import styles from '@/components/styles/CollaboratorsPanel.module.css';

import EditIcon from '@mui/icons-material/Edit';
import CollaboratorModal from '@/components/AddCollaboratorModal';
import api from '@/lib/api';
import { Collaborator } from "@/types/project"
import { useProjects } from '@/context/ProjectContext';

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
        } catch (error) {
            setError('Falha ao carregar colaboradores');
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

            if (selectedCollaborator) {
                await api.put(`/collaborator/${data.id}`, data);
            } else {
                await api.post('/collaborator', data);
            }

            await loadCollaborators();
            setModalOpen(false);
        } catch (error) {
            setError(selectedCollaborator
                ? 'Falha ao atualizar colaborador'
                : 'Falha ao criar novo colaborador');
        }
    };

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

                        <Select
                            value={filterRole}
                            className={styles.roleSelect}
                            onChange={(e) => setFilterRole(e.target.value)}
                            size="small"

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
                            {collaborators.map((collaborator) => (
                                <TableRow key={collaborator.id}>
                                    <TableCell>{collaborator.nome}</TableCell>
                                    <TableCell>
                                        {collaborator.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')}
                                    </TableCell>
                                    <TableCell>{collaborator.role}</TableCell>
                                    <TableCell>{collaborator.pontuacao}</TableCell>
                                    <TableCell>
                                        <Button
                                            startIcon={<EditIcon />}
                                            onClick={() => {
                                                setSelectedCollaborator(collaborator);
                                                setModalOpen(true);
                                            }}
                                        >
                                            Editar
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}

            <CollaboratorModal
                open={modalOpen}
                loading={loading}
                onClose={() => {
                    setModalOpen(false);
                    setSelectedCollaborator(undefined);
                    loadCollaborators();
                }}
                onSave={handleSave}
                onSuccess={() => {
                    console.log('Operation successful');
                }}
                initialData={selectedCollaborator ? {
                    ...selectedCollaborator,
                    id: selectedCollaborator.id ? selectedCollaborator.id : undefined,
                    idCallRote: selectedCollaborator.idCallRote || ''
                } : undefined}
            />
        </div>
    );
}