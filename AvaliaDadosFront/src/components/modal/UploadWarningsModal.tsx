'use client';

import { useState, useEffect } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Typography,
    Button,
    Box,
    List,
    ListItem,
    Chip,
    IconButton
} from '@mui/material';
import {
    Edit as EditIcon,
    Warning as WarningIcon
} from '@mui/icons-material';

interface UploadWarningsModalProps {
    open: boolean;
    warnings: string[];
    onClose: () => void;
    onEditCollaborator: (warning: string) => void;
}

export default function UploadWarningsModal({
    open,
    warnings,
    onClose,
    onEditCollaborator
}: UploadWarningsModalProps) {
    const [localWarnings, setLocalWarnings] = useState<string[]>([]);

    useEffect(() => {
        setLocalWarnings(warnings);
    }, [warnings]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullWidth
            PaperProps={{
                sx: {
                    borderRadius: 2,
                    bgcolor: '#fffde7'
                }
            }}
        >
            <DialogTitle sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                bgcolor: 'warning.light',
                color: 'warning.contrastText'
            }}>
                <WarningIcon />
                <Typography variant="h6" component="span">
                    Alertas do Upload
                </Typography>
                <Chip
                    label={`${warnings.length} alerta(s)`}
                    size="small"
                    color="warning"
                    variant="outlined"
                    sx={{ ml: 1 }}
                />
            </DialogTitle>

            <DialogContent sx={{ py: 2 }}>
                <List sx={{ width: '100%' }}>
                    {localWarnings.map((warning, index) => (
                        <ListItem
                            key={index}
                            sx={{
                                py: 1.5,
                                borderBottom: index < localWarnings.length - 1 ? 1 : 0,
                                borderColor: 'divider'
                            }}
                        >
                            <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                                <Typography variant="body2" sx={{ flexGrow: 1 }}>
                                    {warning}
                                </Typography>
                                <IconButton
                                    onClick={() => onEditCollaborator(warning)}
                                    size="small"
                                    title="Editar colaborador relacionado"
                                >
                                    <EditIcon fontSize="small" />
                                </IconButton>
                            </Box>
                        </ListItem>
                    ))}
                </List>
            </DialogContent>

            <DialogActions sx={{ px: 3, py: 2 }}>
                <Button
                    onClick={onClose}
                    variant="contained"
                    color="primary"
                >
                    Fechar
                </Button>
            </DialogActions>
        </Dialog>
    );
}
