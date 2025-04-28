'use client';

import { useState, useEffect } from 'react';
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
    Alert
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import CollaboratorModal from '@/components/AddCollaboratorModal';
import api from '@/lib/api';
import { Collaborator } from "@/types/project"

export default function CollaboratorsPage() {
    const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedCollaborator, setSelectedCollaborator] = useState<Collaborator | undefined>();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

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
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">Gerenciar Colaboradores</h1>

            </div>

            {loading && <CircularProgress />}

            {error && (
                <Alert severity="error" className="mb-4">
                    {error}
                </Alert>
            )}

            {!loading && !error && (
                <TableContainer component={Paper}>
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
                onClose={() => {
                    setModalOpen(false);
                    setSelectedCollaborator(undefined);
                }}
                onSave={handleSave}
                onSuccess={() => {
                    console.log('Operation successful');
                }}
                initialData={selectedCollaborator}
            />
        </div>
    );
}