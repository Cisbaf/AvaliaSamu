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
    CircularProgress,
    InputLabel,
    FormControl,
    Alert
} from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import styles from "../styles/Modal.module.css"
import { useProjectCollaborators } from '@/context/project/hooks/useProjectCollaborators';
import { CombinedCollaboratorData } from '../CollaboratorsPanel';
import {
    MedicoRole,
    ShiftHours,
    WorkPeriod,
    UpdateProjectCollabDto,
    GlobalCollaborator
} from '@/types/project';
import {
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi
} from '@/lib/api';
import { is24hCollaborator } from '../utils';

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
    // WorkPeriod.H24 = colaborador 24h (Supervisor, ou Médico Líder/Regulador com turno H24).
    workPeriod?: WorkPeriod;
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
        nome: '', cpf: '', idCallRote: '', baseRole: '',
        workPeriod: WorkPeriod.DIURNO,
        medicoRole: undefined, shiftHours: undefined,
        durationSeconds: undefined, quantity: undefined, pausaMensalSeconds: undefined
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (initialData) {
            const isMedico = initialData.role.toUpperCase().startsWith('MEDICO');
            const medicoRole = isMedico ? (initialData as any).medicoRole : undefined;
            const shiftHours = isMedico ? (initialData as any).shiftHours : undefined;
            const is24h = is24hCollaborator(initialData.role, medicoRole, shiftHours);
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                baseRole: isMedico ? 'MEDICO' : initialData.role,
                medicoRole,
                shiftHours,
                workPeriod: is24h ? WorkPeriod.H24 : (initialData.workPeriod || WorkPeriod.DIURNO),
                durationSeconds: (initialData as any).durationSeconds,
                quantity: (initialData as any).quantity,
                pausaMensalSeconds: (initialData as any).pausaMensalSeconds
            });
        } else {
            setFormData({ nome: '', cpf: '', idCallRote: '', baseRole: '', workPeriod: WorkPeriod.DIURNO });
        }
        setError('');
    }, [initialData]);

    const isEdit = Boolean(initialData && 'id' in initialData && initialData.id);

    const is24h = is24hCollaborator(formData.baseRole, formData.medicoRole, formData.shiftHours);

    const handleChange = (key: keyof FormData, value: any) => {
        setFormData(prev => {
            const next = { ...prev, [key]: value };
            const willBe24h = is24hCollaborator(next.baseRole, next.medicoRole, next.shiftHours);
            if (willBe24h) {
                next.workPeriod = WorkPeriod.H24;
            } else if (!next.workPeriod || next.workPeriod === WorkPeriod.H24) {
                next.workPeriod = WorkPeriod.DIURNO;
            }
            return next;
        });
    };

    const handleClose = async () => {
        onClose();
        setFormData({
            nome: '', cpf: '', idCallRote: '', baseRole: '',
            workPeriod: WorkPeriod.DIURNO,
            medicoRole: undefined, shiftHours: undefined,
            durationSeconds: undefined, quantity: undefined, pausaMensalSeconds: undefined
        })
    }


    const handleSubmit = async () => {
        setLoading(true);
        setError('');
        try {
            const finalRole = formData.baseRole;

            if (projectId) {
                if (isEdit) {
                    const baseDto = {
                        nome: formData.nome,
                        role: finalRole,
                        durationSeconds: formData.durationSeconds,
                        quantity: formData.quantity,
                        pausaMensalSeconds: formData.pausaMensalSeconds,
                        pontuacao: (initialData as CombinedCollaboratorData)?.pontuacao ?? 0,
                        idCallRote: formData.idCallRote,
                        workPeriod: formData.workPeriod,
                    };

                    const medicoFields = finalRole === 'MEDICO' ? {
                        medicoRole: formData.medicoRole,
                        shiftHours: formData.shiftHours,
                    } : {};

                    const dto: UpdateProjectCollabDto = {
                        ...baseDto,
                        ...medicoFields,
                    };

                    await updateProjectCollaborator(
                        projectId,
                        (initialData as CombinedCollaboratorData).id!,
                        dto
                    );
                } else {
                    const payloadForAdd = {
                        id: (initialData as GlobalCollaborator)?.id || '',
                        role: finalRole,
                        durationSeconds: formData.durationSeconds,
                        quantity: formData.quantity,
                        pausaMensalSeconds: formData.pausaMensalSeconds,
                        idCallRote: formData.idCallRote,
                        workPeriod: formData.workPeriod,
                        ...(finalRole === 'MEDICO' && {
                            medicoRole: formData.medicoRole,
                            shiftHours: formData.shiftHours,
                        }),
                        parametros: {}
                    };


                    await addCollaboratorToProject(projectId, payloadForAdd);
                }
            } else {
                const payload: Partial<GlobalCollaborator> = {
                    nome: formData.nome,
                    cpf: formData.cpf.replace(/\D/g, ''),
                    idCallRote: formData.idCallRote,
                    role: finalRole,
                    pontuacao: isEdit ? (initialData as GlobalCollaborator).pontuacao : 0,
                    isGlobal: true,
                    workPeriod: formData.workPeriod,
                    ...(finalRole === 'MEDICO' && {
                        medicoRole: formData.medicoRole,
                        shiftHours: formData.shiftHours,
                    }),
                };

                if (isEdit) {
                    await updateGlobalCollaboratorApi(
                        (initialData as GlobalCollaborator).id!,
                        payload as any
                    );
                } else {
                    await createGlobalCollaboratorApi(payload as any);
                }
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            const errorMessage =
                err.response?.data?.message ||
                err.message ||
                'Erro ao salvar colaborador';
            setError(errorMessage);
        } finally {
            setLoading(false);
            setFormData({
                nome: '', cpf: '', idCallRote: '', baseRole: '',
                workPeriod: WorkPeriod.DIURNO,
                medicoRole: undefined, shiftHours: undefined,
                durationSeconds: undefined, quantity: undefined, pausaMensalSeconds: undefined
            })
        }
    };

    const isSubmitDisabled = loading
        || !formData.nome
        || !formData.cpf
        || !formData.idCallRote
        || !formData.baseRole
        || !formData.workPeriod
        || (formData.baseRole === 'MEDICO' && (!formData.medicoRole || !formData.shiftHours));

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
                <DialogTitle>{isEdit ? 'Editar Colaborador' : 'Novo Colaborador'}</DialogTitle>
                <DialogContent className={styles.modalContent}>
                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
                    <div className={styles.formGrid}>
                        <TextField label="Nome" fullWidth margin="dense"
                            value={formData.nome} onChange={e => handleChange('nome', e.target.value)}
                            variant='outlined'
                        />
                        <TextField label="CPF" fullWidth margin="dense"
                            value={formData.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')} onChange={e => handleChange('cpf', e.target.value)}
                            variant={isEdit && projectId ? 'filled' : 'outlined'}
                            slotProps={{
                                input: {
                                    readOnly: isEdit && projectId ? true : false,
                                }
                            }} />
                        <TextField label="ID Call Rote" fullWidth margin="dense"
                            variant='outlined'
                            value={formData.idCallRote.replace(/(\d{1})(\d{11})/, '$1-$2')} onChange={e => handleChange('idCallRote', e.target.value)}
                            slotProps={{
                                input: {
                                }
                            }} />
                        <FormControl fullWidth margin="dense">
                            <InputLabel id="base-role-label">Função Base</InputLabel>
                            <Select
                                labelId="base-role-label"
                                label="Função Base"
                                value={formData.baseRole}
                                onChange={e => handleChange('baseRole', e.target.value)}
                            >
                                <MenuItem value="" disabled>Selecione uma função</MenuItem>
                                <MenuItem value="TARM">TARM</MenuItem>
                                <MenuItem value="FROTA">FROTA</MenuItem>
                                <MenuItem value="MEDICO">MÉDICO</MenuItem>
                                <MenuItem value="SUPERVISOR">SUPERVISOR</MenuItem>
                            </Select>
                        </FormControl>
                        {is24h ? (
                            <TextField
                                label="Período"
                                fullWidth
                                margin="dense"
                                value="24h — não se aplica Diurno/Noturno"
                                variant="filled"
                                slotProps={{ input: { readOnly: true } }}
                            />
                        ) : (
                            <FormControl fullWidth margin="dense">
                                <InputLabel id="work-period-label">Período</InputLabel>
                                <Select
                                    labelId="work-period-label"
                                    label="Período"
                                    value={formData.workPeriod ?? ''}
                                    onChange={e => handleChange('workPeriod', e.target.value as WorkPeriod)}
                                >
                                    <MenuItem value={WorkPeriod.DIURNO}>Diurno</MenuItem>
                                    <MenuItem value={WorkPeriod.NOTURNO}>Noturno</MenuItem>
                                </Select>
                            </FormControl>
                        )}
                        {formData.baseRole === 'MEDICO' && (
                            <>
                                <FormControl fullWidth margin="dense">
                                    <InputLabel id="medico-role-label">Papel Médico</InputLabel>
                                    <Select
                                        labelId="medico-role-label"
                                        label="Papel Médico"
                                        value={formData.medicoRole ?? ''}
                                        onChange={e => handleChange('medicoRole', e.target.value as MedicoRole)}
                                    >
                                        <MenuItem value="" disabled>Selecione um papel</MenuItem>
                                        {Object.values(MedicoRole).map(mr => (
                                            <MenuItem key={mr} value={mr}>{mr}</MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                                <FormControl fullWidth margin="dense">
                                    <InputLabel id="shift-hours-label">Turno</InputLabel>
                                    <Select
                                        labelId="shift-hours-label"
                                        label="Turno"
                                        value={formData.shiftHours ?? ''}
                                        onChange={e => handleChange('shiftHours', e.target.value as ShiftHours)}
                                    >
                                        <MenuItem value="" disabled>Selecione um turno</MenuItem>
                                        {Object.values(ShiftHours).map(sh => (
                                            <MenuItem key={sh} value={sh}>{sh}</MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                            </>
                        )}

                    </div>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleClose} disabled={loading}>Cancelar</Button>
                    <Button onClick={handleSubmit} variant="contained" disabled={isSubmitDisabled}>
                        {loading ? <CircularProgress size={24} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                    </Button>
                </DialogActions>
            </Dialog>
        </LocalizationProvider>
    );
}
