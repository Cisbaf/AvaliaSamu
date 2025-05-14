'use client';

import React, { useState, useEffect } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    MenuItem,
    Select,
    CircularProgress,
    InputLabel,
    FormControl,
    Alert
} from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import styles from './styles/Modal.module.css';
import { useProjectCollaborators } from '@/context/project/hooks/useProjectCollaborators';
import { CombinedCollaboratorData } from './CollaboratorsPanel';
import {
    MedicoRole,
    ShiftHours,
    UpdateProjectCollabDto,
    GlobalCollaborator
} from '@/types/project';
import {
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi
} from '@/lib/api';

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: CombinedCollaboratorData | GlobalCollaborator;
    projectId?: string;
}

type FormData = {
    nome: string;
    cpf: string;
    idCallRote: string;
    baseRole: string;
    medicoRole?: MedicoRole;
    shiftHours?: ShiftHours;
    durationSeconds?: number;
    quantity?: number;
    pausaMensalSeconds?: number;
};

export default function CollaboratorModal({
    open,
    onClose,
    onSuccess,
    initialData,
    projectId
}: CollaboratorModalProps) {
    const {
        addCollaboratorToProject,
        updateProjectCollaborator
    } = useProjectCollaborators().actions;

    const [formData, setFormData] = useState<FormData>({
        nome: '', cpf: '', idCallRote: '', baseRole: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (initialData) {
            const isMedico = initialData.role.startsWith('MEDICO');
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                baseRole: isMedico ? 'MEDICO' : initialData.role,
                medicoRole: isMedico ? (initialData as any).medicoRole : undefined,
                shiftHours: isMedico ? (initialData as any).shiftHours : undefined,
                durationSeconds: (initialData as any).durationSeconds,
                quantity: (initialData as any).quantity,
                pausaMensalSeconds: (initialData as any).pausaMensalSeconds
            });
        } else {
            setFormData({ nome: '', cpf: '', idCallRote: '', baseRole: '' });
        }
        setError('');
    }, [initialData]);

    const isEdit = Boolean(initialData && 'id' in initialData && initialData.id);

    const handleChange = (key: keyof FormData, value: any) => {
        setFormData(f => ({ ...f, [key]: value }));
    };

    const handleSubmit = async () => {
        setLoading(true);
        setError('');
        try {
            const finalRole = formData.baseRole === 'MEDICO' && formData.medicoRole && formData.shiftHours
                ? `MEDICO_${formData.medicoRole}_${formData.shiftHours}`
                : formData.baseRole;

            if (projectId) {
                if (isEdit) {
                    const dto: UpdateProjectCollabDto = {
                        role: finalRole,
                        durationSeconds: formData.durationSeconds,
                        quantity: formData.quantity,
                        pausaMensalSeconds: formData.pausaMensalSeconds
                    };
                    if (formData.medicoRole) dto.medicoRole = formData.medicoRole;
                    if (formData.shiftHours) dto.shiftHours = formData.shiftHours;

                    await updateProjectCollaborator(
                        projectId,
                        (initialData as CombinedCollaboratorData).id!,
                        dto
                    );
                } else {
                    await addCollaboratorToProject(
                        projectId,
                        {
                            id: (initialData as GlobalCollaborator)?.id || '',
                            role: finalRole,
                            durationSeconds: formData.durationSeconds,
                            quantity: formData.quantity,
                            pausaMensalSeconds: formData.pausaMensalSeconds,
                            parametros: {}
                        }
                    );
                }
            } else {
                const payload: Partial<GlobalCollaborator> = {
                    nome: formData.nome,
                    cpf: formData.cpf.replace(/\D/g, ''),
                    idCallRote: formData.idCallRote,
                    role: finalRole,
                    pontuacao: isEdit ? (initialData as GlobalCollaborator).pontuacao : 0,
                    isGlobal: true,
                    ...(formData.medicoRole && { medicoRole: formData.medicoRole }),
                    ...(formData.shiftHours && { shiftHours: formData.shiftHours })
                };
                if (isEdit) {
                    await updateGlobalCollaboratorApi(
                        (initialData as GlobalCollaborator).id!,
                        payload
                    );
                } else {
                    await createGlobalCollaboratorApi(payload as any);
                }
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || err.message || 'Erro ao salvar colaborador');
        } finally {
            setLoading(false);
        }
    };

    const isSubmitDisabled = loading
        || !formData.nome
        || !formData.cpf
        || !formData.idCallRote
        || !formData.baseRole
        || (formData.baseRole === 'MEDICO' && (!formData.medicoRole || !formData.shiftHours));

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
                <DialogTitle>{isEdit ? 'Editar Colaborador' : 'Novo Colaborador'}</DialogTitle>
                <DialogContent className={styles.modalContent}>
                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
                    <div className={styles.formGrid}>
                        <TextField label="Nome" fullWidth margin="dense"
                            value={formData.nome} onChange={e => handleChange('nome', e.target.value)} />
                        <TextField label="CPF" fullWidth margin="dense"
                            value={formData.cpf} onChange={e => handleChange('cpf', e.target.value)} />
                        <TextField label="ID Call Rote" fullWidth margin="dense"
                            value={formData.idCallRote} onChange={e => handleChange('idCallRote', e.target.value)} />
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Função</InputLabel>
                            <Select value={formData.baseRole} onChange={e => handleChange('baseRole', e.target.value as string)}>
                                <MenuItem value="TARM">TARM</MenuItem>
                                <MenuItem value="FROTA">FROTA</MenuItem>
                                <MenuItem value="MEDICO">MÉDICO</MenuItem>
                                <MenuItem value="MEDICO_SUPERVISOR">MÉDICO SUPERVISOR</MenuItem>
                            </Select>
                        </FormControl>
                        {formData.baseRole === 'MEDICO' && (
                            <>
                                <FormControl fullWidth margin="dense">
                                    <InputLabel>Papel Médico</InputLabel>
                                    <Select value={formData.medicoRole} onChange={e => handleChange('medicoRole', e.target.value as MedicoRole)}>
                                        {Object.values(MedicoRole).map(mr => <MenuItem key={mr} value={mr}>{mr}</MenuItem>)}
                                    </Select>
                                </FormControl>
                                <FormControl fullWidth margin="dense">
                                    <InputLabel>Turno</InputLabel>
                                    <Select value={formData.shiftHours} onChange={e => handleChange('shiftHours', e.target.value as ShiftHours)}>
                                        {Object.values(ShiftHours).map(sh => <MenuItem key={sh} value={sh}>{sh}</MenuItem>)}
                                    </Select>
                                </FormControl>
                            </>
                        )}
                        <TextField label="Duração (s)" type="number" fullWidth margin="dense"
                            value={formData.durationSeconds ?? ''} onChange={e => handleChange('durationSeconds', Number(e.target.value))} />
                        <TextField label="Quantidade" type="number" fullWidth margin="dense"
                            value={formData.quantity ?? ''} onChange={e => handleChange('quantity', Number(e.target.value))} />
                        <TextField label="Pausa Mensal (s)" type="number" fullWidth margin="dense"
                            value={formData.pausaMensalSeconds ?? ''}
                            onChange={e => handleChange('pausaMensalSeconds', Number(e.target.value))} />
                    </div>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose} disabled={loading}>Cancelar</Button>
                    <Button onClick={handleSubmit} variant="contained" disabled={isSubmitDisabled}>
                        {loading ? <CircularProgress size={24} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                    </Button>
                </DialogActions>
            </Dialog>
        </LocalizationProvider>
    );
}