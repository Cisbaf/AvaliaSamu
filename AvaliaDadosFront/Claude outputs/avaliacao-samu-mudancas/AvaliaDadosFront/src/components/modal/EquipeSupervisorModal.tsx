'use client';

import { useState, useEffect, useMemo } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    List,
    ListItemButton,
    ListItemText,
    ListItemIcon,
    Checkbox,
    CircularProgress,
    Alert,
    Typography,
    Divider,
} from '@mui/material';
import { MedicoRole, ShiftHours } from '@/types/project';

export interface EquipeCandidato {
    id: string;
    nome: string;
    role: string;
    medicoRole?: MedicoRole;
    shiftHours?: ShiftHours;
}

interface EquipeSupervisorModalProps {
    open: boolean;
    onClose: () => void;
    onSave: (equipeIds: string[]) => Promise<void>;
    supervisorNome: string;
    initialSelectedIds: string[];
    candidatos: EquipeCandidato[];
    // Mensagem extra pra deixar claro se é o cadastro global ou só deste projeto.
    escopoDescricao: string;
}

const ROLE_LABEL: Record<string, string> = {
    TARM: 'TARM',
    FROTA: 'Frota',
    MEDICO: 'Médico',
};

export default function EquipeSupervisorModal({
    open,
    onClose,
    onSave,
    supervisorNome,
    initialSelectedIds,
    candidatos,
    escopoDescricao,
}: EquipeSupervisorModalProps) {
    const [selected, setSelected] = useState<Set<string>>(new Set());
    const [searchTerm, setSearchTerm] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (open) {
            setSelected(new Set(initialSelectedIds));
            setSearchTerm('');
            setError('');
        }
    }, [open, initialSelectedIds]);

    const filtered = useMemo(() => {
        const term = searchTerm.trim().toLowerCase();
        return candidatos
            .filter(c => !term || c.nome.toLowerCase().includes(term))
            .sort((a, b) => a.nome.localeCompare(b.nome));
    }, [candidatos, searchTerm]);

    const toggle = (id: string) => {
        setSelected(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id); else next.add(id);
            return next;
        });
    };

    const handleSave = async () => {
        setSaving(true);
        setError('');
        try {
            await onSave(Array.from(selected));
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || err.message || 'Falha ao salvar a equipe');
        } finally {
            setSaving(false);
        }
    };

    const describeCandidato = (c: EquipeCandidato) => {
        if (c.role === 'MEDICO' && c.medicoRole) {
            return `Médico (${c.medicoRole}${c.shiftHours ? ` - ${c.shiftHours}` : ''})`;
        }
        return ROLE_LABEL[c.role] || c.role;
    };

    return (
        <Dialog open={open} onClose={saving ? undefined : onClose} fullWidth maxWidth="sm">
            <DialogTitle>Equipe de {supervisorNome}</DialogTitle>
            <DialogContent>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    {escopoDescricao} A pontuação do supervisor passa a ser a média da pontuação
                    dos colaboradores selecionados aqui.
                </Typography>

                {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                <TextField
                    placeholder="Pesquisar colaborador"
                    size="small"
                    fullWidth
                    value={searchTerm}
                    onChange={e => setSearchTerm(e.target.value)}
                    sx={{ mb: 1 }}
                />

                <Typography variant="caption" color="text.secondary">
                    {selected.size} selecionado(s)
                </Typography>
                <Divider sx={{ my: 1 }} />

                {candidatos.length === 0 ? (
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                        Nenhum colaborador de TARM, Frota ou Médico disponível.
                    </Typography>
                ) : filtered.length === 0 ? (
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                        Nenhum colaborador encontrado.
                    </Typography>
                ) : (
                    <List dense sx={{ maxHeight: 360, overflowY: 'auto' }}>
                        {filtered.map(c => (
                            <ListItemButton key={c.id} onClick={() => toggle(c.id)} disabled={saving}>
                                <ListItemIcon>
                                    <Checkbox
                                        edge="start"
                                        checked={selected.has(c.id)}
                                        tabIndex={-1}
                                        disableRipple
                                    />
                                </ListItemIcon>
                                <ListItemText primary={c.nome} secondary={describeCandidato(c)} />
                            </ListItemButton>
                        ))}
                    </List>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={saving}>Cancelar</Button>
                <Button onClick={handleSave} variant="contained" disabled={saving}>
                    {saving ? <CircularProgress size={24} /> : 'Salvar equipe'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
