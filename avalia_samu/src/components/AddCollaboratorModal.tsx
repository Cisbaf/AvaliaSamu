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
    FormControl
} from '@mui/material';
import styles from './styles/Modal.module.css';
import { useProjectCollaborators } from '@/context/project/hooks/useProjectCollaborators';
import { CombinedCollaboratorData } from './CollaboratorsPanel';
import { MedicoRole, ShiftHours, UpdateProjectCollabDto } from '@/types/project';

interface CollaboratorModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: CombinedCollaboratorData;
    projectId?: string;
}

type FormData = {
    nome: string;
    cpf: string;
    idCallRote: string;
    baseRole: string;
    medicoRole: MedicoRole | '';
    shiftHours: ShiftHours | '';
    durationSeconds: number | '';
    quantity: number | '';
    pausaMensalSeconds: number | '';
};

export default function CollaboratorModal({
    open,
    onClose,
    onSuccess,
    initialData,
    projectId
}: CollaboratorModalProps) {
    const { addCollaboratorToProject, updateProjectCollaborator } = useProjectCollaborators().actions;
    const [formData, setFormData] = useState<FormData>({
        nome: '',
        cpf: '',
        idCallRote: '',
        baseRole: '',
        medicoRole: '',
        shiftHours: '',
        durationSeconds: '',
        quantity: '',
        pausaMensalSeconds: ''
    });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (initialData) {
            const isMedico = initialData.role.startsWith('MEDICO');
            setFormData({
                nome: initialData.nome,
                cpf: initialData.cpf,
                idCallRote: initialData.idCallRote,
                baseRole: isMedico ? 'MEDICO' : initialData.role,
                medicoRole: initialData.medicoRole || '',
                shiftHours: initialData.shiftHours || '',
                durationSeconds: initialData.durationSeconds || '',
                quantity: initialData.quantity || '',
                pausaMensalSeconds: initialData.pausaMensalSeconds || ''
            });
        } else {
            setFormData({
                nome: '', cpf: '', idCallRote: '', baseRole: '',
                medicoRole: '', shiftHours: '',
                durationSeconds: '', quantity: '', pausaMensalSeconds: ''
            });
        }
    }, [initialData]);

    const isEdit = Boolean(initialData?.id);

    const handleChange = (key: keyof FormData, value: any) => {
        setFormData(f => ({ ...f, [key]: value }));
    };

    const handleSubmit = async () => {
        setLoading(true);
        const finalRole = formData.baseRole === 'MEDICO'
            ? `MEDICO_${formData.medicoRole}_${formData.shiftHours}`
            : formData.baseRole;

        if (projectId) {
            if (isEdit) {
                const dto: UpdateProjectCollabDto = {
                    role: finalRole,
                    durationSeconds: typeof formData.durationSeconds === 'number' ? formData.durationSeconds : undefined,
                    quantity: typeof formData.quantity === 'number' ? formData.quantity : undefined,
                    pausaMensalSeconds: typeof formData.pausaMensalSeconds === 'number' ? formData.pausaMensalSeconds : undefined
                };
                await updateProjectCollaborator(projectId, initialData!.id!, dto);
            } else {
                await addCollaboratorToProject(
                    projectId,
                    {
                        id: '', // set after creating global collaborator elsewhere
                        role: finalRole,
                        durationSeconds: typeof formData.durationSeconds === 'number' ? formData.durationSeconds : undefined,
                        quantity: typeof formData.quantity === 'number' ? formData.quantity : undefined,
                        pausaMensalSeconds: typeof formData.pausaMensalSeconds === 'number' ? formData.pausaMensalSeconds : undefined,
                        parametros: {}
                    }
                );
            }
        }

        setLoading(false);
        onSuccess();
        onClose();
    };

    const isSubmitDisabled = loading
        || !formData.nome
        || !formData.cpf
        || !formData.idCallRote
        || !formData.baseRole
        || (formData.baseRole === 'MEDICO' && (!formData.medicoRole || !formData.shiftHours));

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>{isEdit ? 'Editar Colaborador' : 'Novo Colaborador'}</DialogTitle>
            <DialogContent>
                <TextField
                    label="Nome"
                    fullWidth
                    margin="dense"
                    value={formData.nome}
                    onChange={e => handleChange('nome', e.target.value)}
                />
                <TextField
                    label="CPF"
                    fullWidth
                    margin="dense"
                    value={formData.cpf}
                    onChange={e => handleChange('cpf', e.target.value)}
                />
                <TextField
                    label="ID Call Rote"
                    fullWidth
                    margin="dense"
                    value={formData.idCallRote}
                    onChange={e => handleChange('idCallRote', e.target.value)}
                />
                <FormControl fullWidth margin="dense">
                    <InputLabel>Função</InputLabel>
                    <Select
                        value={formData.baseRole}
                        onChange={e => handleChange('baseRole', e.target.value)}
                    >
                        <MenuItem value="TARM">TARM</MenuItem>
                        <MenuItem value="FROTA">FROTA</MenuItem>
                        <MenuItem value="MEDICO">MÉDICO</MenuItem>
                    </Select>
                </FormControl>
                {formData.baseRole === 'MEDICO' && (
                    <>
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Papel Médico</InputLabel>
                            <Select
                                value={formData.medicoRole}
                                onChange={e => handleChange('medicoRole', e.target.value)}
                            >
                                {Object.values(MedicoRole).map(mr => (
                                    <MenuItem key={mr} value={mr}>{mr}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Turno</InputLabel>
                            <Select
                                value={formData.shiftHours}
                                onChange={e => handleChange('shiftHours', e.target.value)}
                            >
                                {Object.values(ShiftHours).map(sh => (
                                    <MenuItem key={sh} value={sh}>{sh}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </>
                )}
                <TextField
                    label="Duração (s)"
                    type="number"
                    fullWidth
                    margin="dense"
                    value={formData.durationSeconds}
                    onChange={e => handleChange('durationSeconds', parseInt(e.target.value, 10))}
                />
                <TextField
                    label="Quantidade"
                    type="number"
                    fullWidth
                    margin="dense"
                    value={formData.quantity}
                    onChange={e => handleChange('quantity', parseInt(e.target.value, 10))}
                />
                <TextField
                    label="Pausa Mensal (s)"
                    type="number"
                    fullWidth
                    margin="dense"
                    value={formData.pausaMensalSeconds}
                    onChange={e => handleChange('pausaMensalSeconds', parseInt(e.target.value, 10))}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={loading}>Cancelar</Button>
                <Button onClick={handleSubmit} variant="contained" disabled={isSubmitDisabled}>
                    {loading ? <CircularProgress size={20} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}