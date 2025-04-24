// components/CollaboratorModal.tsx
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

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSave: (data: { name: string; function: string }) => void;
    initialData?: { name: string; function: string };
}

export default function CollaboratorModal({
    open,
    onClose,
    onSave,
    initialData
}: CollaboratorModalProps) {
    const [name, setName] = useState('');
    const [role, setRole] = useState('');

    useEffect(() => {
        if (initialData) {
            setName(initialData.name);
            setRole(initialData.function);
        } else {
            setName('');
            setRole('');
        }
    }, [initialData]);

    const handleSubmit = () => {
        if (name && role) {
            onSave({ name, function: role });
            setName('');
            setRole('');
            onClose();
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
                <Button onClick={onClose}>Cancelar</Button>
                <Button
                    onClick={handleSubmit}
                    variant="contained"
                    disabled={!name || !role}
                >
                    {initialData ? 'Salvar' : 'Cadastrar'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}