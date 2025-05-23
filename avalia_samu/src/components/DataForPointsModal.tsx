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
import { UpdateProjectCollabDto, GlobalCollaborator, ShiftHours } from '@/types/project';

interface DataForPointsModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: CombinedCollaboratorData | GlobalCollaborator;
    projectId?: string;
}

type FormData = {
    durationSeconds: number;
    quantity: number;
    pausaMensalSeconds: number;
    saidaVtrSeconds: number;
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
        quantity: 0,
        pausaMensalSeconds: 0,
        saidaVtrSeconds: 0
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const isFrota = initialData?.role === 'FROTA';

    // Função para formatar segundos para o formato hh:mm:ss
    function formatTime(seconds: number) {
        const h = Math.floor(seconds / 3600).toString().padStart(2, '0');
        const m = Math.floor((seconds % 3600) / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');
        return `${h}:${m}:${s}`;
    }

    // Função para converter hh:mm:ss para segundos
    function timeStringToSeconds(val: string): number {
        const [h = "0", m = "0", s = "0"] = val.split(":");
        return Number(h) * 3600 + Number(m) * 60 + Number(s);
    }

    useEffect(() => {
        if (initialData) {
            setFormData({
                durationSeconds: (initialData as any).durationSeconds ?? 0,
                quantity: (initialData as any).quantity ?? 0,
                pausaMensalSeconds: (initialData as any).pausaMensalSeconds ?? 0,
                saidaVtrSeconds: (initialData as any).saidaVtr ?? 0
            });
        } else {
            setFormData({ durationSeconds: 0, quantity: 0, pausaMensalSeconds: 0, saidaVtrSeconds: 0 });
        }
        setError('');
    }, [initialData]);

    const handleChangeTime = (field: 'durationSeconds' | 'pausaMensalSeconds' | 'saidaVtrSeconds') =>
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
                    durationSeconds: formData.durationSeconds || 0,
                    quantity: formData.quantity || 0,
                    pausaMensalSeconds: formData.pausaMensalSeconds || 0,
                    saidaVtr: isFrota ? formData.saidaVtrSeconds || 0 : 0, // Apenas para Frota, senão 0
                    role: initialData!.role,
                    nome: initialData!.nome,
                    medicoRole: initialData!.medicoRole,
                    shiftHours: initialData!.shiftHours as ShiftHours,
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
                            inputProps={{ step: 1 }}
                            value={formatTime(formData.durationSeconds)}
                            onChange={handleChangeTime('durationSeconds')}
                        />
                    </Grid>

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
                                value={formatTime(formData.saidaVtrSeconds)}
                                onChange={handleChangeTime('saidaVtrSeconds')}
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
