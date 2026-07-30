import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions,
    Button
} from '@mui/material';

interface ConfirmationDialogProps {
    open: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title?: string;
    message?: string;
    cancelText?: string;
    confirmText?: string;
    confirmColor?: 'primary' | 'secondary' | 'error' | 'success' | 'info' | 'warning';
}

export default function ConfirmationDialog({
    open,
    onClose,
    onConfirm,
    title = "Confirmar ação",
    message = "Você tem certeza que deseja realizar esta ação?",
    cancelText = "Cancelar",
    confirmText = "Confirmar",
    confirmColor = "error"
}: ConfirmationDialogProps) {
    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <DialogContentText>{message}</DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>{cancelText}</Button>
                <Button onClick={onConfirm} color={confirmColor} autoFocus>
                    {confirmText}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
