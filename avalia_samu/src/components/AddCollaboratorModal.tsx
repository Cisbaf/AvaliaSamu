'use client';

import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, TextField, MenuItem, Select, CircularProgress
} from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import styles from './styles/Modal.module.css';
import { GlobalCollaborator } from '@/types/project';
import {
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi,
    addCollaboratorToProjectApi
} from '@/lib/api';
import { MedicoRole, ShiftHours } from '@/types/project';

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: {
        id?: string;
        nome: string;
        cpf: string;
        idCallRote: string;
        role: string;
        medicoRole?: MedicoRole;
        shiftHours?: ShiftHours;
    };
    projectId?: string;
}

type FormData = {
    nome: string;
    cpf: string;
    idCallRote: string;
    role: string;
    medicoRole: MedicoRole | '';
    shiftHours: ShiftHours | '';
};

export default function CollaboratorModal({
    open, onClose, onSuccess, initialData, projectId
}: CollaboratorModalProps) {
    const [formData, setFormData] = useState<FormData>({
        nome: '',
        cpf: '',
        idCallRote: '',
        role: '',
        medicoRole: '',
        shiftHours: '',
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
                medicoRole: initialData.medicoRole ?? '',
                shiftHours: initialData.shiftHours ?? '',
            });
        } else {
            setFormData({
                nome: '',
                cpf: '',
                idCallRote: '',
                role: '',
                medicoRole: '',
                shiftHours: '',
            });
        }
    }, [initialData]);

    const isEdit = Boolean(initialData?.id);

    const handleSubmit = async () => {
        setLoading(true);
        setError('');
        try {
            const payload: Omit<GlobalCollaborator, 'id'> = {
                nome: formData.nome,
                cpf: formData.cpf.replace(/\D/g, ''),
                idCallRote: formData.idCallRote,
                role: formData.role,
                pontuacao: 0,
                isGlobal: true,
                // só envio esses campos quando for médico
                ...(formData.role.startsWith('MEDICO') && {
                    medicoRole: formData.medicoRole,
                    shiftHours: formData.shiftHours,
                }),
            };

            let saved: GlobalCollaborator;
            if (isEdit) {
                await updateGlobalCollaboratorApi(initialData!.id!, payload);
                saved = { ...initialData!, ...payload, id: initialData!.id! };
            } else {
                saved = await createGlobalCollaboratorApi(payload);
            }

            if (projectId) {
                await addCollaboratorToProjectApi(
                    projectId,
                    saved.id!,
                    saved.role
                );
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || 'Erro ao salvar colaborador');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
                <DialogTitle>
                    {isEdit ? 'Editar Colaborador' : 'Novo Colaborador'}
                </DialogTitle>
                <DialogContent className={styles.modalContent}>
                    {error && <div className={styles.errorMessage}>{error}</div>}
                    <div className={styles.formGrid}>
                        <TextField
                            label="Nome completo"
                            value={formData.nome}
                            onChange={e => setFormData(f => ({ ...f, nome: e.target.value }))}
                            fullWidth required
                        />
                        <TextField
                            label="CPF"
                            value={formData.cpf}
                            onChange={e => setFormData(f => ({ ...f, cpf: e.target.value }))}
                            fullWidth required
                        />
                        <TextField
                            label="ID Call Rote"
                            value={formData.idCallRote}
                            onChange={e => setFormData(f => ({ ...f, idCallRote: e.target.value }))}
                            fullWidth required
                        />
                        <Select
                            value={formData.role}
                            onChange={e => setFormData(f => ({
                                ...f,
                                role: e.target.value,
                                // quando mudar de role não-médico, limpar valores de médico
                                ...(!e.target.value.startsWith('MEDICO')
                                    ? { medicoRole: '', shiftHours: '' }
                                    : {}
                                )
                            }))}
                            displayEmpty fullWidth required
                        >
                            <MenuItem value="" disabled>Selecione a função</MenuItem>
                            <MenuItem value="TARM">Tarm</MenuItem>
                            <MenuItem value="FROTA">Frota</MenuItem>
                            <MenuItem value="MEDICO">Médico</MenuItem>
                            <MenuItem value="MEDICO_SUPERVISOR">Médico Supervisor</MenuItem>
                        </Select>
                        {formData.role.startsWith('MEDICO') && (
                            <>
                                <Select
                                    value={formData.medicoRole}
                                    onChange={e => setFormData(f => ({ ...f, medicoRole: e.target.value as MedicoRole }))}
                                    displayEmpty fullWidth required
                                >
                                    <MenuItem value="" disabled>Selecione o papel médico</MenuItem>
                                    {Object.values(MedicoRole).map(mr => (
                                        <MenuItem key={mr} value={mr}>{mr}</MenuItem>
                                    ))}
                                </Select>
                                <Select
                                    value={formData.shiftHours}
                                    onChange={e => setFormData(f => ({ ...f, shiftHours: e.target.value as ShiftHours }))}
                                    displayEmpty fullWidth required
                                >
                                    <MenuItem value="" disabled>Selecione o turno</MenuItem>
                                    {Object.values(ShiftHours).map(sh => (
                                        <MenuItem key={sh} value={sh}>{sh}</MenuItem>
                                    ))}
                                </Select>
                            </>
                        )}
                    </div>
                </DialogContent>
                <DialogActions className={styles.modalActions}>
                    <Button onClick={onClose} disabled={loading}>Cancelar</Button>
                    <Button
                        onClick={handleSubmit}
                        variant="contained"
                        disabled={
                            loading ||
                            !formData.nome ||
                            !formData.cpf ||
                            !formData.idCallRote ||
                            !formData.role ||
                            (formData.role.startsWith('MEDICO') &&
                                (!formData.medicoRole || !formData.shiftHours))
                        }
                    >
                        {loading ? <CircularProgress size={24} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                    </Button>
                </DialogActions>
            </Dialog>
        </LocalizationProvider>
    );
}
