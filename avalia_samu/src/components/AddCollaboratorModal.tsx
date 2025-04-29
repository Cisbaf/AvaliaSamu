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
    Select,
    CircularProgress
} from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import styles from "./styles/Modal.module.css";
import api from '@/lib/api';
import { Collaborator } from "@/types/project"


interface CollaboratorModalProps {
    open: boolean;
    onSave: (data: Collaborator) => Promise<void>; onClose: () => void;
    onSuccess: () => void;
    initialData?: {
        id?: number;
        nome: string;
        cpf: string;
        idCallRote: string;
        role: string;

    };
}

export default function CollaboratorModal({ open, onClose, initialData, onSuccess }: CollaboratorModalProps) {
    const [formData, setFormData] = useState({
        nome: '',
        cpf: '',
        idCallRote: '',
        role: '',

    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (initialData) {
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                role: initialData.role,

            });
        }
    }, [initialData]);

    const handleSubmit = async () => {
        setLoading(true);
        setError('');

        try {
            const payload = {
                ...formData,
                cpf: formData.cpf.replace(/\D/g, ''), // Remove non-numeric characters
            };

            const response = initialData?.id
                ? await api.put(`/collaborator/${initialData.id}`, payload)
                : await api.post('/collaborator', payload);

            if (response.status === 200) {
                onSuccess();
                onClose();
            }
        } catch (err) {
            setError('Erro ao salvar colaborador. Verifique os dados e tente novamente.');
            console.error('API Error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
                <DialogTitle>
                    {initialData ? 'Editar Colaborador' : 'Novo Colaborador'}
                </DialogTitle>

                <DialogContent className={styles.modalContent}>
                    {error && <div className={styles.errorMessage}>{error}</div>}

                    <div className={styles.formGrid}>
                        <TextField
                            label="Nome completo"
                            value={formData.nome}
                            onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                            fullWidth
                            margin="normal"
                            required
                        />

                        <TextField
                            label="CPF"
                            value={formData.cpf}
                            onChange={(e) => setFormData({ ...formData, cpf: e.target.value })}
                            fullWidth
                            margin="normal"
                            inputProps={{ pattern: '[0-9]{11}' }}
                            required
                        />

                        <TextField
                            label="ID Call Rote"
                            value={formData.idCallRote}
                            onChange={(e) => setFormData({ ...formData, idCallRote: e.target.value })}
                            fullWidth
                            margin="normal"
                        />



                        <Select
                            value={formData.role}
                            onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                            fullWidth
                            displayEmpty
                            required
                            className={styles.roleSelect}
                        >
                            <MenuItem value="" disabled>Selecione a função</MenuItem>
                            <MenuItem value="TARM">Tarm</MenuItem>
                            <MenuItem value="FROTA">Frota</MenuItem>
                            <MenuItem value="MEDICO">Médico</MenuItem>
                            <MenuItem value="MEDICO_SUPERVISOR">Médico Supervisor</MenuItem>
                        </Select>


                    </div>
                </DialogContent>

                <DialogActions className={styles.modalActions}>
                    <Button onClick={onClose} disabled={loading}>
                        Cancelar
                    </Button>

                    <Button
                        onClick={handleSubmit}
                        className={loading ? styles.loadingButton : ''}
                        variant="contained"
                        color="primary"
                        disabled={loading || !formData.nome || !formData.cpf || !formData.role}
                    >
                        {loading ? (
                            <CircularProgress size={24} color="inherit" />
                        ) : initialData ? (
                            'Salvar Alterações'
                        ) : (
                            'Cadastrar'
                        )}
                    </Button>
                </DialogActions>
            </Dialog>
        </LocalizationProvider>
    );
}