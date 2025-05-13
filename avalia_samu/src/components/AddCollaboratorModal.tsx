'use client';

import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, TextField, MenuItem, Select, CircularProgress, InputLabel, FormControl
} from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import styles from './styles/Modal.module.css';
import { CombinedCollaboratorData } from './CollaboratorsPanel';
import {
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi
} from '@/lib/api';
import { MedicoRole, ShiftHours } from '@/types/project';

type FormData = {
    nome: string;
    cpf: string;
    idCallRote: string;
    baseRole: string;
    medicoRole: MedicoRole | '';
    shiftHours: ShiftHours | '';
};

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: CombinedCollaboratorData;
    projectId?: string;
}

export default function CollaboratorModal({
    open, onClose, onSuccess, initialData, projectId
}: CollaboratorModalProps) {
    const [formData, setFormData] = useState<FormData>({
        nome: '',
        cpf: '',
        idCallRote: '',
        baseRole: '',
        medicoRole: '',
        shiftHours: '',
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (initialData) {
            const isMedico = initialData.role === 'MEDICO' || initialData.role.startsWith('MEDICO_');
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                baseRole: isMedico ? 'MEDICO' : initialData.role,
                medicoRole: isMedico && initialData.medicoRole ? initialData.medicoRole : '',
                shiftHours: isMedico && initialData.shiftHours ? initialData.shiftHours : '',
            });
        } else {
            setFormData({
                nome: '',
                cpf: '',
                idCallRote: '',
                baseRole: '',
                medicoRole: '',
                shiftHours: '',
            });
        }
    }, [initialData]);

    const isEdit = Boolean(initialData?.id);

    const handleBaseRoleChange = (e: any) => {
        const baseRole = e.target.value as string;
        setFormData(f => ({
            ...f,
            baseRole,
            medicoRole: baseRole === 'MEDICO' ? f.medicoRole : '',
            shiftHours: baseRole === 'MEDICO' ? f.shiftHours : ''
        }));
    };
    const handleMedicoRoleChange = (e: any) => {
        setFormData(f => ({ ...f, medicoRole: e.target.value as MedicoRole }));
    };
    const handleShiftHoursChange = (e: any) => {
        setFormData(f => ({ ...f, shiftHours: e.target.value as ShiftHours }));
    };

    const handleSubmit = async () => {
        setLoading(true);
        setError('');

        const finalRole = formData.baseRole === 'MEDICO'
            ? `MEDICO_${formData.medicoRole}_${formData.shiftHours}`
            : formData.baseRole;

        const payload = {
            nome: formData.nome,
            cpf: formData.cpf.replace(/\D/g, ''),
            idCallRote: formData.idCallRote,
            role: finalRole,
            pontuacao: isEdit ? initialData!.pontuacao : 0,
            isGlobal: true as true,
            medicoRole: formData.medicoRole as MedicoRole,
            shiftHours: formData.shiftHours as ShiftHours,
        };

        try {
            let collaboratorId = initialData?.id!;
            if (!isEdit) {
                const newC = await createGlobalCollaboratorApi(payload);
                collaboratorId = newC.id!;
            } else {
                await updateGlobalCollaboratorApi(collaboratorId, payload);
            }

            if (projectId) {
                if (isEdit) {
                    await updateProjectCollaboratorApi(
                        projectId,
                        collaboratorId,
                        finalRole,
                        initialData?.durationSeconds,
                        initialData?.quantity,
                        initialData?.pausaMensalSeconds,
                    );
                } else {
                    await addCollaboratorToProjectApi(
                        projectId,
                        collaboratorId,
                        finalRole,
                        undefined, undefined, undefined, {}
                    );
                }
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || err.message || 'Erro ao salvar');
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
                        <FormControl fullWidth required>
                            <InputLabel id="base-role-label">Função</InputLabel>
                            <Select
                                labelId="base-role-label"
                                value={formData.baseRole}
                                onChange={handleBaseRoleChange}
                                label="Função"
                            >
                                <MenuItem value="TARM">TARM</MenuItem>
                                <MenuItem value="FROTA">FROTA</MenuItem>
                                <MenuItem value="MEDICO">MÉDICO</MenuItem>
                                <MenuItem value="MEDICO_SUPERVISOR">MÉDICO SUPERVISOR</MenuItem>
                            </Select>
                        </FormControl>
                        {formData.baseRole === 'MEDICO' && (
                            <>
                                <FormControl fullWidth required>
                                    <InputLabel id="medico-role-label">Papel Médico</InputLabel>
                                    <Select
                                        labelId="medico-role-label"
                                        value={formData.medicoRole}
                                        onChange={handleMedicoRoleChange}
                                        label="Papel Médico"
                                    >
                                        {Object.values(MedicoRole).map(mr => (
                                            <MenuItem key={mr} value={mr}>{mr}</MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                                <FormControl fullWidth required>
                                    <InputLabel id="shift-hours-label">Turno</InputLabel>
                                    <Select
                                        labelId="shift-hours-label"
                                        value={formData.shiftHours}
                                        onChange={handleShiftHoursChange}
                                        label="Turno"
                                    >
                                        {Object.values(ShiftHours).map(sh => (
                                            <MenuItem key={sh} value={sh}>{sh}</MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                            </>
                        )}
                    </div>
                </DialogContent>
                <DialogActions className={styles.modalActions}>
                    <Button onClick={onClose} disabled={loading}>Cancelar</Button>
                    <Button onClick={handleSubmit} variant="contained" disabled={isSubmitDisabled}>
                        {loading ? <CircularProgress size={24} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                    </Button>
                </DialogActions>
            </Dialog>
        </LocalizationProvider>
    );
}
