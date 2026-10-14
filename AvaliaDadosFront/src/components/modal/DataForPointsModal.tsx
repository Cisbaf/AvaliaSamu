import { useProjectCollaborators } from '@/context/project/hooks/useProjectCollaborators';
import { GlobalCollaborator, MedicoRole, ShiftHours, UpdateProjectCollabDto } from '@/types/project';
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Grid // Importe o Grid
    ,
    TextField
} from '@mui/material';
import { useEffect, useState, type ChangeEvent } from 'react';
import { CombinedCollaboratorData } from '../CollaboratorsPanel';
import styles from "../styles/Modal.module.css";
import { formatSecondsToTime, parseTimeInputToSeconds } from '../utils';

interface DataForPointsModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: CombinedCollaboratorData | GlobalCollaborator;
    projectId?: string;
}

type FormData = {
    durationSeconds: number;
    criticos: number,
    removidos: number;
    removidosLider?: number;
    pausaMensalSeconds: number;
    saidaVtr: number;
    points: number;
};

// Roles permitidos para ver os campos específicos
type Roles = 'TARM' | 'FROTA' | 'MEDICO';

export default function DataForPointsModal({
    open,
    onClose,
    onSuccess,
    initialData,
    projectId
}: DataForPointsModalProps) {
    const { actions: { updateProjectCollaborator } } = useProjectCollaborators();

    const [formData, setFormData] = useState<FormData>({
        durationSeconds: 0,
        criticos: 0,
        removidos: 0,
        removidosLider: 0,
        pausaMensalSeconds: 0,
        saidaVtr: 0,
        points: 0,
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // --- LÓGICA DE VISIBILIDADE ---
    const role = initialData?.role as Roles;
    const isFrota = role === 'FROTA';
    const isLider = initialData?.medicoRole === MedicoRole.LIDER;

    // Variável para checar se o role é um dos relevantes
    const showSpecificFields = role && ['TARM', 'FROTA', 'MEDICO'].includes(role);


    useEffect(() => {
        if (initialData) {
            setFormData({
                durationSeconds: initialData.durationSeconds ?? (initialData as any).duration ?? 0,
                criticos: (initialData as any).criticos ?? 0,
                removidos: initialData.removidos ?? 0,
                removidosLider: isLider ? (initialData as any).removidosLider ?? 0 : undefined,
                pausaMensalSeconds: initialData.pausaMensalSeconds ?? (initialData as any).pausaMensal ?? 0,
                saidaVtr: initialData.saidaVtr ?? 0,
                points: initialData.pontuacao ?? 0,
            });
        }
    }, [initialData]);

    // Campos de tempo são texto HH:MM:SS (aceitam mais de 24h). O rascunho guarda o que o
    // usuário digita e só vira segundos ao sair do campo.
    const [timeDrafts, setTimeDrafts] = useState<Record<string, string>>({});

    const handleChangeTime = (field: 'durationSeconds' | 'pausaMensalSeconds' | 'saidaVtr' | 'criticos') =>
        (e: ChangeEvent<HTMLInputElement>) => {
            const raw = e.target.value;
            setTimeDrafts(prev => ({ ...prev, [field]: raw }));
        };

    const handleBlurTime = (field: 'durationSeconds' | 'pausaMensalSeconds' | 'saidaVtr' | 'criticos') =>
        () => {
            const raw = timeDrafts[field];
            if (raw === undefined) return;
            const seconds = parseTimeInputToSeconds(raw);
            setFormData(prev => ({ ...prev, [field]: seconds }));
            setTimeDrafts(prev => {
                const next = { ...prev };
                delete next[field];
                return next;
            });
        };

    const valorTempo = (field: 'durationSeconds' | 'pausaMensalSeconds' | 'saidaVtr' | 'criticos') =>
        timeDrafts[field] ?? formatSecondsToTime(formData[field]);

    const handleChangeNumber = (field: keyof Pick<FormData, 'criticos' | 'removidos' | 'points' | 'removidosLider'>) =>
        (e: ChangeEvent<HTMLInputElement>) => {
            const value = Number(e.target.value);
            setFormData(prev => ({ ...prev, [field]: isNaN(value) ? 0 : value }));
        };

    const handleSubmit = async () => {
        setLoading(true);
        setError('');

        try {
            if (projectId) {
                const dto: UpdateProjectCollabDto = {
                    durationSeconds: formData.durationSeconds ?? 0,
                    criticos: formData.criticos ?? 0,
                    removidos: formData.removidos || 0,
                    removidosLider: isLider ? formData.removidosLider || 0 : 0,
                    pausaMensalSeconds: formData.pausaMensalSeconds ?? 0,
                    saidaVtr: isFrota ? formData.saidaVtr || 0 : 0,
                    role: initialData!.role,
                    nome: initialData!.nome,
                    medicoRole: initialData!.medicoRole,
                    shiftHours: initialData!.shiftHours as ShiftHours,
                    workPeriod: initialData!.workPeriod,
                    pontuacao: formData.points || 0,
                };

                await updateProjectCollaborator(
                    projectId,
                    (initialData as CombinedCollaboratorData).id!,
                    dto,
                    true
                );
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || err.message || 'Erro ao salvar dados');
        } finally {
            setLoading(false);
        }
    };

    const isSubmitDisabled = loading || formData.durationSeconds < 0 || formData.removidos < 0 || formData.pausaMensalSeconds < 0 || formData.points < 0 || (formData.removidosLider ?? 0) < 0;

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>Editar Pontos</DialogTitle>
            <DialogContent className={styles.modalContent}>
                {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                <Grid container spacing={2} marginTop={2}>

                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Pontos"
                            type="number"
                            fullWidth
                            value={formData.points}
                            onChange={handleChangeNumber('points')}
                        />
                    </Grid>

                    {showSpecificFields && (
                        <>
                            {!isLider && (
                                <Grid size={{ xs: 12, sm: 4 }}>
                                    <TextField
                                        label="Tempo de Regulação"
                                        type="text"
                                        placeholder="HH:MM:SS"
                                        fullWidth
                                        InputLabelProps={{ shrink: true }}
                                        inputProps={{ step: 1 }}
                                        value={valorTempo('durationSeconds')}
                                        onChange={handleChangeTime('durationSeconds')}
                                        onBlur={handleBlurTime('durationSeconds')}
                                    />
                                </Grid>
                            )}

                            {isLider && (
                                <Grid size={{ xs: 12, sm: 4 }}>
                                    <TextField
                                        label="Criticos"
                                        type="text"
                                        placeholder="HH:MM:SS"
                                        fullWidth
                                        InputLabelProps={{ shrink: true }}
                                        inputProps={{ step: 1 }}
                                        value={valorTempo('criticos')}
                                        onChange={handleChangeTime('criticos')}
                                        onBlur={handleBlurTime('criticos')}
                                    />
                                </Grid>
                            )}

                            {!isFrota && !isLider && (
                                <Grid size={{ xs: 12, sm: 4 }}>
                                    <TextField
                                        label="Removidos"
                                        type="number"
                                        fullWidth
                                        value={formData.removidos}
                                        onChange={handleChangeNumber('removidos')}
                                    />
                                </Grid>
                            )}
                            {isLider && (
                                <Grid size={{ xs: 12, sm: 4 }}>
                                    <TextField
                                        label="Removidos"
                                        type="number"
                                        fullWidth
                                        value={formData.removidosLider}
                                        onChange={handleChangeNumber('removidosLider')}
                                    />
                                </Grid>
                            )}

                            {isFrota && (
                                <Grid size={{ xs: 12, sm: 4 }}>
                                    <TextField
                                        label="Saida VTR"
                                        type="text"
                                        placeholder="HH:MM:SS"
                                        fullWidth
                                        InputLabelProps={{ shrink: true }}
                                        inputProps={{ step: 1 }}
                                        value={valorTempo('saidaVtr')}
                                        onChange={handleChangeTime('saidaVtr')}
                                        onBlur={handleBlurTime('saidaVtr')}
                                    />
                                </Grid>
                            )}
                        </>
                    )}

                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Pausas Mensais"
                            type="text"
                            fullWidth
                            InputLabelProps={{ shrink: true }}
                            inputProps={{ step: 1 }}
                            value={valorTempo('pausaMensalSeconds')}
                            onChange={handleChangeTime('pausaMensalSeconds')}
                            onBlur={handleBlurTime('pausaMensalSeconds')}
                            placeholder="HH:MM:SS"
                        />
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={loading}>Cancelar</Button>
                <Button onClick={handleSubmit} variant="contained" disabled={isSubmitDisabled}>
                    Salvar
                </Button>
            </DialogActions>
        </Dialog>
    );
}
