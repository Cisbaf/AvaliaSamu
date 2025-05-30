import React, { useState, useEffect } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    Alert,
    Grid
} from '@mui/material';
import styles from './styles/Modal.module.css';
import { useProjectCollaborators } from '@/context/project/hooks/useProjectCollaborators';
import { CombinedCollaboratorData } from './CollaboratorsPanel';
import { UpdateProjectCollabDto, GlobalCollaborator, ShiftHours, MedicoRole } from '@/types/project';

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
    quantity: number;
    pausaMensalSeconds: number;
    saidaVtr: number;
};

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
        quantity: 0,
        pausaMensalSeconds: 0,
        saidaVtr: 0
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const isFrota = initialData?.role === 'FROTA';
    const isLiderRef = initialData?.medicoRole === MedicoRole.LIDER_REGULADOR;

    function formatTime(seconds: number): string {
        if (!seconds && seconds !== 0) return '00:00:00';

        const h = Math.floor(seconds / 3600).toString().padStart(2, '0');
        const m = Math.floor((seconds % 3600) / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');

        return `${h}:${m}:${s}`;
    }

    function timeStringToSeconds(val: string): number {
        if (!val) return 0;

        const cleanVal = val.replace(/\D/g, '').padEnd(6, '0');

        const h = parseInt(cleanVal.substring(0, 2)) || 0;
        const m = parseInt(cleanVal.substring(2, 4)) || 0;
        const s = parseInt(cleanVal.substring(4, 6)) || 0;

        return (h * 3600) + (m * 60) + s;
    }

    useEffect(() => {
        if (initialData) {
            setFormData({
                durationSeconds:
                    // tenta pegar durationSeconds, depois duration, depois 0
                    initialData.durationSeconds ??
                    (initialData as any).duration ??
                    0,
                criticos: (initialData as any).criticos ?? 0,
                quantity: initialData.quantity ?? 0,
                pausaMensalSeconds:
                    initialData.pausaMensalSeconds ??
                    (initialData as any).pausaMensal ??
                    0,
                saidaVtr: initialData.saidaVtr ?? 0,
            });
        }
    }, [initialData]);
    const handleChangeTime = (field: 'durationSeconds' | 'pausaMensalSeconds' | 'saidaVtr' | 'criticos') =>
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const timeValue = e.target.value;
            const seconds = timeStringToSeconds(timeValue);
            setFormData(prev => ({ ...prev, [field]: seconds }));
        };

    const handleChangeNumber = (field: 'quantity') =>
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const value = Number(e.target.value);
            setFormData(prev => ({ ...prev, [field]: isNaN(value) ? 0 : value }));
        };

    const handleSubmit = async () => {
        setLoading(true);
        setError('');

        try {
            if (projectId) {
                const dto: UpdateProjectCollabDto = {
                    durationSeconds: formData.durationSeconds ?? 0, // Usar nullish coalescing
                    criticos: formData.criticos ?? 0,
                    quantity: formData.quantity || 0,
                    pausaMensalSeconds: formData.pausaMensalSeconds ?? 0,
                    saidaVtr: isFrota ? formData.saidaVtr || 0 : 0,
                    role: initialData!.role,
                    nome: initialData!.nome,
                    medicoRole: initialData!.medicoRole,
                    shiftHours: initialData!.shiftHours as ShiftHours,
                };
                console.info('DTO:', dto);
                console.info("FormData:", formData);

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

    const isSubmitDisabled = loading || formData.durationSeconds < 0 || formData.quantity < 0 || formData.pausaMensalSeconds < 0;

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>Editar Pontos</DialogTitle>
            <DialogContent className={styles.modalContent}>
                {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
                <Grid container spacing={2} marginTop={2}>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Tempo de Regulação"
                            type="time"
                            fullWidth
                            InputLabelProps={{ shrink: true }}
                            inputProps={{
                                step: 1,
                                pattern: "[0-9]{2}:[0-9]{2}:[0-9]{2}"
                            }}
                            value={formatTime(formData.durationSeconds)}
                            onChange={handleChangeTime('durationSeconds')}
                        />
                    </Grid>
                    {isLiderRef && (
                        <Grid size={{ xs: 12, sm: 4 }}>
                            <TextField
                                label="Criticos"
                                type="time"
                                fullWidth
                                InputLabelProps={{ shrink: true }}
                                inputProps={{
                                    step: 1,
                                    pattern: "[0-9]{2}:[0-9]{2}:[0-9]{2}"
                                }}
                                value={formatTime(formData.criticos)}
                                onChange={handleChangeTime('criticos')}
                            />
                        </Grid>
                    )}

                    {/* Mostra "Removidos" apenas se não for Frota */}
                    {!isFrota && (
                        <Grid size={{ xs: 12, sm: 4 }}>
                            <TextField
                                label="Removidos"
                                type="number"
                                fullWidth
                                value={formData.quantity}
                                onChange={handleChangeNumber('quantity')}
                            />
                        </Grid>
                    )}


                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Pausas Mensais"
                            type="time"
                            fullWidth
                            inputProps={{ step: 1 }}
                            value={formatTime(formData.pausaMensalSeconds)}
                            onChange={handleChangeTime('pausaMensalSeconds')}
                        />
                    </Grid>

                    {/* Mostra "Saída VTR" apenas se for Frota */}
                    {isFrota && (
                        <Grid size={{ xs: 12, sm: 4 }}>
                            <TextField
                                label="Saída VTR"
                                type="time"
                                fullWidth
                                inputProps={{ step: 1 }}
                                value={formatTime(formData.saidaVtr)}
                                onChange={handleChangeTime('saidaVtr')}
                            />
                        </Grid>
                    )}
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
