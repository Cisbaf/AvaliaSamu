'use client';

import { useState, useEffect } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    MenuItem,
    Select
} from '@mui/material';
import styles from "./styles/Modal.module.css"
import { useProjects } from '../context/ProjectContext';

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSave: (data: { _id?: string; name: string; function: string }) => void; // Adicionar _id
    initialData?: {
        _id?: string; // Usar _id ao invés de id
        name: string;
        function: string;
        mode?: string; // Added the 'mode' property
    };
}

export default function CollaboratorModal({
    open,
    onClose,
    initialData
}: CollaboratorModalProps) {
    const {
        actions: { createGlobalCollaborator, updateGlobalCollaborator }
    } = useProjects();
    const [name, setName] = useState('');
    const [collaboratorId, setCollaboratorId] = useState('');
    const [role, setRole] = useState('');

    useEffect(() => {
        if (initialData) {
            setName(initialData.name);
            setRole(initialData.function);
            // Se precisar do _id para alguma lógica interna
            if (initialData._id) setCollaboratorId(initialData._id);
        } else {
            setName('');
            setRole('');
            setCollaboratorId('');
        }
    }, [initialData]);

    const handleSubmit = async () => {
        if (name && role) {
            try {
                if (initialData?._id) {
                    await updateGlobalCollaborator(initialData._id, { name, function: role });
                } else {
                    await createGlobalCollaborator({
                        name,
                        function: role,
                        points: 0,
                        isGlobal: true
                    });
                }
                onClose();
            } catch (error) {
                console.error('Erro ao salvar colaborador:', error);
            }
        }
    };

    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitle>{initialData ? 'Editar Colaborador' : 'Novo Colaborador'}</DialogTitle>
            <DialogContent className={styles.modalContent}>
                <TextField
                    autoFocus
                    margin="dense"
                    label="Nome completo"
                    fullWidth
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    sx={{ mb: 2 }}
                />
                <Select
                    fullWidth
                    value={role}
                    className={styles.select}
                    onChange={(e) => setRole(e.target.value)}
                    displayEmpty
                    inputProps={{ 'aria-label': 'Selecione a função' }}
                >
                    <MenuItem value="" disabled>
                        Selecione a função
                    </MenuItem>
                    <MenuItem value="TARM">TARM</MenuItem>
                    <MenuItem value="Frota">Frota</MenuItem>
                    <MenuItem value="MEDICO">MÉDICO</MenuItem>
                    <MenuItem value="MEDICO SUPERVISOR">MÉDICO SUPERVISOR</MenuItem>
                </Select>
            </DialogContent>
            <DialogActions className={styles.modalActions}>
                <Button onClick={() => { onClose(); setName(""); setRole(""); }}>Cancelar</Button>
                <Button
                    onClick={() => { handleSubmit(); setName(""); setRole(""); }}
                    variant="contained"
                    disabled={!name || !role}
                >
                    {initialData ? 'Salvar' : 'Cadastrar'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}


