import React from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    IconButton,
    Typography,
    Box,
} from '@mui/material';
import { XMarkIcon } from '@heroicons/react/24/outline';

interface ModalAction {
    label: string;
    onClick: () => void;
    color?: 'inherit' | 'primary' | 'secondary' | 'success' | 'error' | 'info' | 'warning';
    variant?: 'text' | 'outlined' | 'contained';
    disabled?: boolean;
}

interface ModalProps {
    open: boolean;
    onClose: () => void;
    title: string;
    content: React.ReactNode;
    actions?: ModalAction[];
    maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
    fullWidth?: boolean;
}

const Modal: React.FC<ModalProps> = ({
    open,
    onClose,
    title,
    content,
    actions = [],
    maxWidth = 'sm',
    fullWidth = true,
}) => {
    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth={maxWidth}
            fullWidth={fullWidth}
            aria-labelledby="modal-title"
        >
            <DialogTitle id="modal-title">
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h6" component="span">{title}</Typography>
                    <IconButton
                        aria-label="close"
                        onClick={onClose}
                        sx={{
                            color: (theme) => theme.palette.grey[500],
                        }}
                    >
                        <XMarkIcon className="h-6 w-6" />
                    </IconButton>
                </Box>
            </DialogTitle>
            <DialogContent dividers>
                {content}
            </DialogContent>
            {actions.length > 0 && (
                <DialogActions sx={{ p: 2 }}>
                    {actions.map((action, index) => (
                        <Button
                            key={index}
                            onClick={action.onClick}
                            color={action.color || 'primary'}
                            variant={action.variant || 'contained'}
                            disabled={action.disabled || false}
                        >
                            {action.label}
                        </Button>
                    ))}
                </DialogActions>
            )}
        </Dialog>
    );
};

export default Modal;
