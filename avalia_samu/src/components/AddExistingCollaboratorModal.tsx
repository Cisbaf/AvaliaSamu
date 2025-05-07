// AddExistingCollaboratorModal.tsx
'use client';

import React, { useState } from 'react';
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
import { GlobalCollaborator } from '@/types/project';
import { useGlobalCollaborators } from '@/context/project/hooks/useGlobalCollaborators';

interface AddExistingCollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    collaborators: GlobalCollaborator[];
    onAdd: (collaboratorId: string, role: string) => Promise<void>;
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
                                        onClick={() => onAdd(collab.id!, collab.role)}
                                        disabled={loading}
                                    >
                                        <Add color="primary" />
                                    </IconButton>
                                }
                            >
                                <ListItemText
                                    primary={collab.nome}
                                    secondary={collab.role}
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