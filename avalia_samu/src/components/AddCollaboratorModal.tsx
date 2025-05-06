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
import { Collaborator } from "@/types/project"

interface CollaboratorModalProps {
    open: boolean;
    onSave: (data: Collaborator) => Promise<void>;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: {
        id?: string;
        nome: string;
        cpf: string;
        idCallRote: string;
        role: string;
    };
    loading: boolean;

}

export default function CollaboratorModal({ open, onSave, onClose, onSuccess, initialData }: CollaboratorModalProps) {
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

    const setBlank = () => {
        setFormData({
            nome: '',
            cpf: '',
            idCallRote: '',
            role: '',
        });
    }
    const isEdit = Boolean(initialData?.id);


    const handleSubmit = async () => {
        setLoading(true);
        setError('');

        try {
            const dataToSave: Collaborator = {
                nome: formData.nome,
                cpf: formData.cpf.replace(/\D/g, ''),
                idCallRote: formData.idCallRote,
                role: formData.role,
                pontuacao: 0,
                isGlobal: true,
                ...(isEdit && { id: initialData?.id })
            };

            console.log('Data to save:', dataToSave);

            await onSave(dataToSave);
            onSuccess();
            setBlank();
            onClose();
        } catch (err) {
            setError('Erro ao salvar colaborador. Verifique os dados e tente novamente.');
            console.error('onSave Error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Dialog
                open={open}
                onClose={onClose}
                maxWidth="md"
                fullWidth
                role="dialog"
                aria-labelledby="dialog-title"
                aria-modal="true"
            >
                <DialogTitle id="dialog-title">
                    {initialData ? 'Editar Colaborador' : 'Novo Colaborador'}
                </DialogTitle>

                <DialogContent className={styles.modalContent}>
                    {error && <div className={styles.errorMessage} role="alert">{error}</div>}

                    <div className={styles.formGrid}>
                        <TextField
                            label="Nome completo"
                            value={formData.nome}
                            onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                            fullWidth
                            margin="normal"
                            required
                            inputProps={{ 'aria-required': 'true' }}
                        />

                        <TextField
                            label="CPF"
                            value={formData.cpf}
                            onChange={(e) => setFormData({ ...formData, cpf: e.target.value })}
                            fullWidth
                            margin="normal"
                            inputProps={{
                                pattern: '[0-9]{11}',
                                'aria-label': 'CPF do colaborador'
                            }}
                            required
                        />

                        <TextField
                            label="ID Call Rote"
                            value={formData.idCallRote}
                            onChange={(e) => setFormData({ ...formData, idCallRote: e.target.value })}
                            fullWidth
                            margin="normal"
                            inputProps={{ 'aria-label': 'ID Call Rote' }}
                        />

                        <Select
                            value={formData.role}
                            onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                            fullWidth
                            displayEmpty
                            required
                            className={styles.roleSelect}
                            inputProps={{
                                'aria-label': 'Selecione a função',
                                'aria-required': 'true'
                            }}
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
                    <Button
                        onClick={onClose}
                        disabled={loading}
                        aria-label="Cancelar"
                    >
                        Cancelar
                    </Button>

                    <Button
                        onClick={handleSubmit}
                        className={loading ? styles.loadingButton : ''}
                        variant="contained"
                        color="primary"
                        disabled={loading || !formData.nome || !formData.cpf || !formData.role}
                        aria-label={initialData ? 'Salvar alterações' : 'Cadastrar'}
                    >
                        {loading ? (
                            <CircularProgress size={24} color="inherit" aria-label="Processando" />
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