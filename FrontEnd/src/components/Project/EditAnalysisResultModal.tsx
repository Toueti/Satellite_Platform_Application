import React, { useState, useEffect } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Button,
    CircularProgress,
    Typography,
    Alert,
} from '@mui/material';
import { AnalysisResult, UpdateAnalysisData } from '@/services/analysis.service';

interface EditAnalysisResultModalProps {
    open: boolean;
    result: AnalysisResult | null;
    isUpdating: boolean;
    error: string | null;
    onClose: () => void;
    onSave: (modifiedData: Partial<UpdateAnalysisData['data']>) => Promise<void>;
}

const EditAnalysisResultModal: React.FC<EditAnalysisResultModalProps> = ({
    open,
    result,
    isUpdating,
    error,
    onClose,
    onSave,
}) => {
    const [notes, setNotes] = useState('');

    useEffect(() => {
        // Pre-fill notes when the modal opens with a result
        // Assuming the 'notes' field might exist in the backend response's 'data' object,
        // even though it wasn't in the log. If not, we need to adjust where notes are stored/retrieved.
        // For now, let's assume it *could* be there or we add it.
        // The backend PUT request example includes 'notes', so we should allow editing it.
        // We need to check the actual structure of `result.data` if available or assume it's just `notes`.
        // Let's default to empty string if not found.
        // TODO: Verify where 'notes' are actually stored in the AnalysisResult fetched data.
        // For now, we'll manage notes locally in the modal.
        if (result) {
            // If notes were part of the fetched result.data, use that:
            // setNotes((result.data as any)?.notes || '');
            // Since they likely aren't, we just reset on new result:
             setNotes(''); // Or load from somewhere if notes are stored separately
        }
    }, [result]);

    const handleSave = () => {
        if (!result) return;
        // We only save the 'notes' field for now
        onSave({ notes: notes });
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Edit Analysis Result (ID: {result?.id})</DialogTitle>
            <DialogContent>
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                    </Alert>
                )}
                <Typography variant="body1" gutterBottom>
                    Index Type: {result?.index_type}
                </Typography>
                <Typography variant="body1" gutterBottom>
                    Image ID: {result?.imageId}
                </Typography>
                 <Typography variant="body1" gutterBottom>
                    Date: {result?.end_time ? new Date(result.end_time).toLocaleString() : 'N/A'}
                </Typography>
                <TextField
                    label="Notes"
                    multiline
                    rows={4}
                    fullWidth
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    margin="normal"
                    variant="outlined"
                    disabled={isUpdating}
                />
            </DialogContent>
            <DialogActions sx={{ p: 2 }}>
                <Button onClick={onClose} disabled={isUpdating} color="secondary">
                    Cancel
                </Button>
                <Button
                    onClick={handleSave}
                    variant="contained"
                    color="primary"
                    disabled={isUpdating}
                    startIcon={isUpdating ? <CircularProgress size={20} color="inherit" /> : null}
                >
                    {isUpdating ? 'Saving...' : 'Save Changes'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default EditAnalysisResultModal;
