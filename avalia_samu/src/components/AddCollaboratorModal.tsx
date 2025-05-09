'use client';

import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, TextField, MenuItem, Select, CircularProgress, InputLabel, FormControl
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

type FormData = {
    nome: string;
    cpf: string;
    idCallRote: string;
    baseRole: string;
    medicoRole: MedicoRole | '';
    shiftHours: ShiftHours | '';
};

const parseInitialRole = (roleString: string) => {
    let baseRole = roleString;
    let medicoRole: MedicoRole | '' = '';
    let shiftHours: ShiftHours | '' = '';

    const parts = roleString.split('_');
    if (parts[0] === 'MEDICO') {
        baseRole = 'MEDICO';
        const parsedMedicoRole = parts[1];
        if (parsedMedicoRole && Object.values(MedicoRole).includes(parsedMedicoRole as MedicoRole)) {
            medicoRole = parsedMedicoRole as MedicoRole;
        }
        const parsedShiftHours = parts[2];
        if (parsedShiftHours && Object.values(ShiftHours).includes(parsedShiftHours as ShiftHours)) {
            shiftHours = parsedShiftHours as ShiftHours;
        }
    } else if (roleString === 'MEDICO_SUPERVISOR') {
        baseRole = 'MEDICO_SUPERVISOR';
    }

    return {
        baseRole,
        medicoRole,
        shiftHours,
    };
};

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: GlobalCollaborator;
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
            const parsedRoles = parseInitialRole(initialData.role);
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                baseRole: parsedRoles.baseRole,
                medicoRole: parsedRoles.medicoRole,
                shiftHours: parsedRoles.shiftHours,
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

    const handleBaseRoleChange = (event: any) => {
        const newBaseRole = event.target.value as string;
        setFormData(prev => {
            const updatedFormData = {
                ...prev,
                baseRole: newBaseRole,
            };
            if (newBaseRole !== 'MEDICO') {
                updatedFormData.medicoRole = '';
                updatedFormData.shiftHours = '';
            }
            return updatedFormData;
        });
    };

    const handleMedicoRoleChange = (event: any) => {
        setFormData(prev => ({ ...prev, medicoRole: event.target.value as MedicoRole }));
    };

    const handleShiftHoursChange = (event: any) => {
        setFormData(prev => ({ ...prev, shiftHours: event.target.value as ShiftHours }));
    };

    const handleSubmit = async () => {
        setLoading(true);
        setError('');
        try {
            const finalRole = formData.baseRole === 'MEDICO'
                ? `MEDICO_${formData.medicoRole}_${formData.shiftHours}`
                : formData.baseRole;

            type ApiPayload = Omit<GlobalCollaborator, 'id'> & { isGlobal: true };

            const payload: ApiPayload = {
                nome: formData.nome,
                cpf: formData.cpf.replace(/\D/g, ''),
                idCallRote: formData.idCallRote,
                role: finalRole,
                pontuacao: isEdit ? initialData!.pontuacao : 0,
                isGlobal: true,
            };

            let savedCollaborator: GlobalCollaborator;

            if (isEdit) {
                await updateGlobalCollaboratorApi(initialData!.id!, payload);
                savedCollaborator = {
                    id: initialData!.id!,
                    nome: payload.nome,
                    cpf: payload.cpf,
                    idCallRote: payload.idCallRote,
                    role: payload.role,
                    pontuacao: initialData!.pontuacao,
                    isGlobal: initialData!.isGlobal,
                };
            } else {
                savedCollaborator = await createGlobalCollaboratorApi(payload);
            }

            if (projectId) {
                await addCollaboratorToProjectApi(
                    projectId,
                    savedCollaborator.id!,
                    savedCollaborator.role
                );
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            const apiErrorMessage = err.response?.data?.message || err.message || 'Erro ao salvar colaborador';
            setError(apiErrorMessage);
            console.error("API Error:", err.response?.data || err);
        } finally {
            setLoading(false);
        }
    };

    const isSubmitDisabled = loading ||
        !formData.nome ||
        !formData.cpf ||
        !formData.idCallRote ||
        !formData.baseRole ||
        (formData.baseRole === 'MEDICO' &&
            (!formData.medicoRole || !formData.shiftHours));

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

                        <FormControl fullWidth required>
                            <InputLabel id="base-role-label">Função</InputLabel>
                            <Select
                                labelId="base-role-label"
                                value={formData.baseRole}
                                onChange={handleBaseRoleChange}
                                label="Função"
                                displayEmpty
                            >
                                <MenuItem value="TARM">Tarm</MenuItem>
                                <MenuItem value="FROTA">Frota</MenuItem>
                                <MenuItem value="MEDICO">Médico</MenuItem>
                                <MenuItem value="MEDICO_SUPERVISOR">Médico Supervisor</MenuItem>
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
                                        displayEmpty
                                    >
                                        {Object.values(MedicoRole).map((mr: MedicoRole) => (
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
                                        displayEmpty
                                    >
                                        {Object.values(ShiftHours).map((sh: ShiftHours) => (
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
                    <Button
                        onClick={handleSubmit}
                        variant="contained"
                        disabled={isSubmitDisabled}
                    >
                        {loading ? <CircularProgress size={24} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                    </Button>
                </DialogActions>
            </Dialog>
        </LocalizationProvider>
    );
}