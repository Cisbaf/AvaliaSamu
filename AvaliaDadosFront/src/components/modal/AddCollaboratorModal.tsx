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
    FormControlLabel,
    FormLabel,
    Radio,
    RadioGroup,
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
    updateGlobalCollaboratorApi,
    fetchGlobalCollaboratorsByCpfApi
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
    // WorkPeriod.H24 = colaborador 24h (somente Supervisor). Médico escolhe DIURNO ou NOTURNO.
    workPeriod?: WorkPeriod;
    // Só médico: o que ele recebe (planilha e API) é dividido por 2. Vazio = não.
    plantaoDuplo?: boolean;
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
    // Médicos já cadastrados com este CPF (busca feita ao sair do campo CPF).
    const [cadastrosDoCpf, setCadastrosDoCpf] = useState<GlobalCollaborator[]>([]);

    useEffect(() => {
        if (initialData) {
            const isMedico = initialData.role.toUpperCase().startsWith('MEDICO');
            const medicoRole = isMedico ? (initialData as any).medicoRole : undefined;
            // Médico é sempre 12h: o turno não é escolhido, o período (Diurno/Noturno) é.
            // Regulador é 12h; Líder é 24h.
            const shiftHours = isMedico
                ? (medicoRole === MedicoRole.LIDER ? ShiftHours.H24 : ShiftHours.H12)
                : undefined;
            const is24h = is24hCollaborator(initialData.role, medicoRole, shiftHours);
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                baseRole: isMedico ? 'MEDICO' : initialData.role,
                medicoRole,
                shiftHours,
                workPeriod: is24h ? WorkPeriod.H24 : (initialData.workPeriod || WorkPeriod.DIURNO),
                plantaoDuplo: Boolean((initialData as any).plantaoDuplo),
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

    // Ao sair do campo CPF: se for um médico já cadastrado, puxa nome, ID Call Rote e função.
    // O período (diurno/noturno) continua sendo escolhido pelo usuário.
    const buscarPorCpf = async (cpfDigitado: string) => {
        if (isEdit) return;
        const digitos = cpfDigitado.replace(/\D/g, '');
        if (digitos.length !== 11) {
            setCadastrosDoCpf([]);
            return;
        }
        try {
            const { data } = await fetchGlobalCollaboratorsByCpfApi(digitos);
            const medicos = (data || []).filter(c => (c.role || '').toUpperCase().startsWith('MEDICO'));
            setCadastrosDoCpf(medicos);
            if (medicos.length > 0) {
                const base = medicos[0] as any;
                setFormData(prev => ({
                    ...prev,
                    nome: prev.nome || base.nome,
                    idCallRote: prev.idCallRote || base.idCallRote,
                    baseRole: 'MEDICO',
                    medicoRole: base.medicoRole ?? prev.medicoRole,
                    shiftHours: (base.medicoRole ?? prev.medicoRole) === MedicoRole.LIDER ? ShiftHours.H24 : ShiftHours.H12,
                }));
            }
        } catch {
            // Sem resultado ou erro de rede: segue como cadastro manual.
            setCadastrosDoCpf([]);
        }
    };

    const periodosJaCadastrados = cadastrosDoCpf.map(c => (c as any).workPeriod as WorkPeriod);
    const periodoJaCadastrado = !isEdit
        && formData.baseRole === 'MEDICO'
        && periodosJaCadastrados.includes(formData.workPeriod as WorkPeriod);

    const handleChange = (key: keyof FormData, value: any) => {
        setFormData(prev => {
            const next = { ...prev, [key]: value };
            // O turno do médico é fixado e não aparece no formulário: Regulador 12h, Líder 24h.
            if (next.baseRole === 'MEDICO' && (key === 'baseRole' || key === 'medicoRole')) {
                next.shiftHours = next.medicoRole === MedicoRole.LIDER ? ShiftHours.H24 : ShiftHours.H12;
            }
            // Plantão duplo só vale para Regulador: ao trocar para outro papel, desmarca.
            if (key === 'medicoRole' && value !== MedicoRole.REGULADOR) {
                next.plantaoDuplo = false;
            }
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
                        plantaoDuplo: formData.medicoRole === MedicoRole.REGULADOR && Boolean(formData.plantaoDuplo),
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
        || (formData.baseRole === 'MEDICO' && (!formData.medicoRole || !formData.shiftHours))
        || periodoJaCadastrado;

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
                <DialogTitle>{isEdit ? 'Editar Colaborador' : 'Novo Colaborador'}</DialogTitle>
                <DialogContent className={styles.modalContent}>
                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
                    {cadastrosDoCpf.length > 0 && !isEdit && (
                        <Alert severity={periodoJaCadastrado ? 'warning' : 'info'} sx={{ mb: 2 }}>
                            {periodoJaCadastrado
                                ? `Este médico já está cadastrado no período ${formData.workPeriod === WorkPeriod.NOTURNO ? 'Noturno' : 'Diurno'}. Escolha o outro período.`
                                : `Médico já cadastrado (${periodosJaCadastrados.map(p => p === WorkPeriod.NOTURNO ? 'Noturno' : 'Diurno').join(', ')}). Escolha o período deste cadastro.`}
                        </Alert>
                    )}
                    <div className={styles.formGrid}>
                        <TextField label="Nome" fullWidth margin="dense"
                            value={formData.nome} onChange={e => handleChange('nome', e.target.value)}
                            variant='outlined'
                        />
                        <TextField label="CPF" fullWidth margin="dense"
                            value={formData.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')} onChange={e => handleChange('cpf', e.target.value)}
                            onBlur={() => buscarPorCpf(formData.cpf)}
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
                                {/* Plantão duplo só existe para Regulador; Líder tem parâmetros próprios. */}
                                {!projectId && formData.medicoRole === MedicoRole.REGULADOR && (
                                    <FormControl margin="dense">
                                        <FormLabel id="plantao-duplo-label">Plantão duplo</FormLabel>
                                        <RadioGroup
                                            row
                                            aria-labelledby="plantao-duplo-label"
                                            value={formData.plantaoDuplo ? 'sim' : 'nao'}
                                            onChange={e => handleChange('plantaoDuplo', e.target.value === 'sim')}
                                        >
                                            <FormControlLabel value="nao" control={<Radio />} label="Não" />
                                            <FormControlLabel value="sim" control={<Radio />} label="Sim (valores divididos por 2)" />
                                        </RadioGroup>
                                    </FormControl>
                                )}
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
