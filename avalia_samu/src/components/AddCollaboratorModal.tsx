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
    FormControl,
    Alert
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
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

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
        setErrorMessage(null); // Limpa erro ao editar campo
    };

    const handleSubmit = async () => {
        try {
            setLoading(true);
            setErrorMessage(null);

            const finalRole = formData.baseRole === 'MEDICO'
                ? `MEDICO_${formData.medicoRole}_${formData.shiftHours}`
                : formData.baseRole;

            if (!projectId) {
                throw new Error('Projeto não selecionado');
            }

            if (isEdit) {
                const dto: UpdateProjectCollabDto = {
                    role: finalRole,
                    durationSeconds: typeof formData.durationSeconds === 'number' ? formData.durationSeconds : undefined,
                    quantity: typeof formData.quantity === 'number' ? formData.quantity : undefined,
                    pausaMensalSeconds: typeof formData.pausaMensalSeconds === 'number' ? formData.pausaMensalSeconds : undefined,
                    medicoRole: formData.medicoRole as MedicoRole,
                    shiftHours: formData.shiftHours as ShiftHours
                };
                await updateProjectCollaborator(projectId, initialData!.id!, dto);
            } else {
                await addCollaboratorToProject(
                    projectId,
                    {
                        id: '',
                        role: finalRole,
                        durationSeconds: typeof formData.durationSeconds === 'number' ? formData.durationSeconds : undefined,
                        quantity: typeof formData.quantity === 'number' ? formData.quantity : undefined,
                        pausaMensalSeconds: typeof formData.pausaMensalSeconds === 'number' ? formData.pausaMensalSeconds : undefined,
                        parametros: {}
                    }
                );
            }

            onSuccess();
            onClose();
        } catch (error: any) {
            console.error('Erro ao salvar colaborador:', error);
            setErrorMessage(
                error.response?.data?.message ||
                error.message ||
                'Ocorreu um erro ao salvar o colaborador'
            );
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
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>{isEdit ? 'Editar Colaborador' : 'Novo Colaborador'}</DialogTitle>
            <DialogContent className={styles.modalContent}>
                {errorMessage && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorMessage}
                    </Alert>
                )}

                <div className={styles.formGrid}>
                    <TextField
                        label="Nome"
                        fullWidth
                        margin="dense"
                        value={formData.nome}
                        onChange={e => handleChange('nome', e.target.value)}
                        error={!formData.nome && !!errorMessage}
                        helperText={!formData.nome && !!errorMessage && "Nome é obrigatório"}
                    />

                    <TextField
                        label="CPF"
                        fullWidth
                        margin="dense"
                        value={formData.cpf}
                        onChange={e => handleChange('cpf', e.target.value)}
                        error={!formData.cpf && !!errorMessage}
                        helperText={!formData.cpf && !!errorMessage && "CPF é obrigatório"}
                    />

                    <TextField
                        label="ID Call Rote"
                        fullWidth
                        margin="dense"
                        value={formData.idCallRote}
                        onChange={e => handleChange('idCallRote', e.target.value)}
                        error={!formData.idCallRote && !!errorMessage}
                        helperText={!formData.idCallRote && !!errorMessage && "ID Call Rote é obrigatório"}
                    />

                    <FormControl fullWidth margin="dense" className={styles.roleSelect}>
                        <InputLabel id="base-role-label">Função</InputLabel>
                        <Select
                            labelId="base-role-label"
                            value={formData.baseRole}
                            onChange={e => handleChange('baseRole', e.target.value)}
                            label="Função"
                            error={!formData.baseRole && !!errorMessage}
                        >
                            <MenuItem value="TARM">TARM</MenuItem>
                            <MenuItem value="FROTA">FROTA</MenuItem>
                            <MenuItem value="MEDICO">MÉDICO</MenuItem>
                        </Select>
                    </FormControl>

                    {formData.baseRole === 'MEDICO' && (
                        <>
                            <FormControl fullWidth margin="dense">
                                <InputLabel id="papel-label">Papel Médico</InputLabel>
                                <Select
                                    labelId="papel-label"
                                    label="Papel Médico"
                                    value={formData.medicoRole}
                                    onChange={e => handleChange('medicoRole', e.target.value)}
                                    error={!formData.medicoRole && !!errorMessage}
                                >
                                    {Object.values(MedicoRole).map(mr => (
                                        <MenuItem key={mr} value={mr}>{mr}</MenuItem>
                                    ))}
                                </Select>
                            </FormControl>

                            <FormControl fullWidth margin="dense">
                                <InputLabel id="turno-label">Turno</InputLabel>
                                <Select
                                    labelId="turno-label"
                                    label="Turno"
                                    value={formData.shiftHours}
                                    onChange={e => handleChange('shiftHours', e.target.value)}
                                    error={!formData.shiftHours && !!errorMessage}
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
                <Button
                    onClick={handleSubmit}
                    variant="contained"
                    disabled={isSubmitDisabled}
                    className={loading ? styles.buttonLoading : ''}
                >
                    {loading ? <CircularProgress size={20} /> : isEdit ? 'Salvar' : 'Cadastrar'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}