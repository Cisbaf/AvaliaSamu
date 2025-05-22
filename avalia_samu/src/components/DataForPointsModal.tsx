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

    const handleChange = (field: keyof FormData) =>
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
                    durationSeconds: formData.durationSeconds,
                    quantity: formData.quantity,
                    pausaMensalSeconds: formData.pausaMensalSeconds,
                    saidaVtr: formData.saidaVtrSeconds,
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
            <DialogTitle> 'Editar Pontos'</DialogTitle>
            <DialogContent className={styles.modalContent}>
                {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
                <Grid container spacing={2} marginTop={2}>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Duração (s)"
                            type="number"
                            fullWidth
                            value={formData.durationSeconds}
                            onChange={handleChange('durationSeconds')}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Quantidade"
                            type="number"
                            fullWidth
                            value={formData.quantity}
                            onChange={handleChange('quantity')}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Pausa Mensal (s)"
                            type="number"
                            fullWidth
                            value={formData.pausaMensalSeconds}
                            onChange={handleChange('pausaMensalSeconds')}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Saida Vtr (s)"
                            type="number"
                            fullWidth
                            value={formData.saidaVtrSeconds}
                            onChange={handleChange('saidaVtrSeconds')}
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
