import React, { ReactNode } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography } from '@mui/material';

export interface ModalAction {
  label: string;
  onClick: () => void | Promise<void>;
  color?: 'inherit' | 'primary' | 'secondary' | 'error';
  disabled?: boolean;
}

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  content: ReactNode;
  actions: ModalAction[];
}

const Modal: React.FC<ModalProps> = ({ open, onClose, title, content, actions }) => {
  if (!open) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        {typeof content === 'string' ? (
          <Typography>{content}</Typography>
        ) : (
          content
        )}
      </DialogContent>
      <DialogActions>
        {actions.map((action, index) => (
          <Button
            key={index}
            onClick={action.onClick}
            color={action.color || 'primary'}
            disabled={action.disabled}
          >
            {action.label}
          </Button>
        ))}
      </DialogActions>
    </Dialog>
  );
};

export default Modal;
