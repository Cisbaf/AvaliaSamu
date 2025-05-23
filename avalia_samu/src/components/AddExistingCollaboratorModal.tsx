'use client';

import React from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    List,
    ListItem,
    ListItemText,
    Button,
    DialogActions,
    CircularProgress,
    IconButton
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { GlobalCollaborator, MedicoRole, ShiftHours } from '@/types/project';

interface AddExistingCollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    collaborators: GlobalCollaborator[];
    onAdd: (collaboratorId: string, role: string, medicoRole?: MedicoRole, shiftHours?: ShiftHours) => Promise<void>;
    loading: boolean;
}

export default function AddExistingCollaboratorModal({
    open,
    onClose,
    collaborators,
    onAdd,
    loading
}: AddExistingCollaboratorModalProps) {
    console.log('Colaboradores disponíveis:', collaborators);

    const handleAddCollaborator = async (collab: GlobalCollaborator) => {
        if (collab.role === 'MEDICO') {
            await onAdd(collab.id!, collab.role, collab.medicoRole, collab.shiftHours);
        } else {
            await onAdd(collab.id!, collab.role);
        }
    };

    const isAddButtonDisabled = (collab: GlobalCollaborator) => {
        if (loading) return true;

        if (collab.role === 'MEDICO') {
            return !collab.medicoRole || !collab.shiftHours;
        }

        return false;
    };

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>Adicionar Colaborador Existente</DialogTitle>
            <DialogContent>
                {loading && <CircularProgress />}
                {!loading && collaborators.length === 0 && (
                    <p>Nenhum colaborador disponível para adicionar.</p>
                )}
                {!loading && collaborators.length > 0 && (
                    <List>
                        {collaborators.map(collab => (
                            <ListItem
                                key={collab.id}
                                secondaryAction={
                                    <IconButton
                                        edge="end"
                                        aria-label="add"
                                        onClick={() => handleAddCollaborator(collab)}
                                        disabled={isAddButtonDisabled(collab)}
                                    >
                                        <Add color="primary" />
                                    </IconButton>
                                }
                            >
                                <ListItemText
                                    primary={collab.nome}
                                    secondary={
                                        <>
                                            {collab.role}
                                            {collab.role === 'MEDICO' && collab.medicoRole && collab.shiftHours &&
                                                ` (${collab.medicoRole} - ${collab.shiftHours})`
                                            }
                                            {collab.role === 'MEDICO' && (!collab.medicoRole || !collab.shiftHours) &&
                                                ' (Faltam dados de papel médico ou turno)'
                                            }
                                        </>
                                    }
                                />
                            </ListItem>
                        ))}
                    </List>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color="primary">
                    Cancelar
                </Button>
            </DialogActions>
        </Dialog>
    );
}
