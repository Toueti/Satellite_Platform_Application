import React, { useState, useEffect } from 'react';
import {
    Modal,
    Box,
    Typography,
    TextField,
    Button,
    Stack,
    CircularProgress,
    Alert,
} from '@mui/material';
import { GeeParams } from '@/services/gee.service'; // Assuming GeeParams type exists

interface GeeTweakParamsModalProps {
    open: boolean;
    onClose: () => void;
    initialParams: Partial<GeeParams>; // Use partial as not all params might be tweakable initially
    onSubmit: (updatedParams: Partial<GeeParams>) => Promise<void>; // Make async to handle potential loading state
    isSubmitting?: boolean; // Optional prop to indicate submission in progress
}

// Default values (adjust as needed based on backend defaults or common usage)
const defaultParams: Partial<GeeParams> = {
    max_cloud_cover: 20,
    images_number: 10,
    visualization_params: JSON.stringify({ bands: ['B4', 'B3', 'B2'], min: 0, max: 3000 }, null, 2),
    dimensions: '768x768',
    scale: 30,
    crs: 'EPSG:4326',
};

const GeeTweakParamsModal: React.FC<GeeTweakParamsModalProps> = ({
    open,
    onClose,
    initialParams,
    onSubmit,
    isSubmitting = false, // Default to false
}) => {
    const [params, setParams] = useState<Partial<GeeParams>>(defaultParams);
    const [errors, setErrors] = useState<Record<string, string>>({});

    // Reset form state when modal opens or initial params change
    useEffect(() => {
        if (open) {
            // Merge initial params with defaults, giving precedence to initialParams
            const mergedParams = { ...defaultParams, ...initialParams };
            // Ensure visualization_params is stringified if it's an object
            if (typeof mergedParams.visualization_params === 'object' && mergedParams.visualization_params !== null) {
                try {
                    mergedParams.visualization_params = JSON.stringify(mergedParams.visualization_params, null, 2);
                } catch (e) {
                    console.error("Error stringifying initial visualization params:", e);
                    mergedParams.visualization_params = defaultParams.visualization_params; // Fallback
                }
            }
            // Ensure dimensions is stringified if it's an object
            if (typeof mergedParams.dimensions === 'object' && mergedParams.dimensions !== null) {
                // Assuming object format is { width: number, height: number }
                try {
                    // @ts-ignore // Ignore potential type error for accessing width/height
                    mergedParams.dimensions = `${mergedParams.dimensions.width}x${mergedParams.dimensions.height}`;
                } catch (e) {
                     console.error("Error stringifying initial dimensions:", e);
                     mergedParams.dimensions = defaultParams.dimensions; // Fallback
                }
            }
            setParams(mergedParams);
            setErrors({}); // Clear errors on open
        }
    }, [open, initialParams]);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;
        setParams((prevParams: Partial<GeeParams>) => ({ // Explicitly type prevParams
            ...prevParams,
            [name]: value,
        }));
        // Clear error for this field on change
        if (errors[name]) {
            setErrors(prevErrors => {
                const newErrors = { ...prevErrors };
                delete newErrors[name];
                return newErrors;
            });
        }
    };

    const validateParams = (): boolean => {
        const newErrors: Record<string, string> = {};
        let isValid = true;

        // Cloud Cover
        const cloudCover = Number(params.max_cloud_cover);
        if (isNaN(cloudCover) || cloudCover < 0 || cloudCover > 100) {
            newErrors.max_cloud_cover = 'Must be a number between 0 and 100.';
            isValid = false;
        }

        // Images Number
        const imagesNumber = Number(params.images_number);
        if (isNaN(imagesNumber) || !Number.isInteger(imagesNumber) || imagesNumber <= 0) {
            newErrors.images_number = 'Must be a positive integer.';
            isValid = false;
        }

        // Visualization Params (Basic JSON check - ensure it's a string first)
        const visParamsString = typeof params.visualization_params === 'string' ? params.visualization_params : '';
        try {
            // Only attempt parse if it was originally a string or became one
            if (visParamsString) {
                 JSON.parse(visParamsString);
            } else if (params.visualization_params !== null && typeof params.visualization_params === 'object') {
                 // If it's an object, assume it's valid for now, or add deeper validation if needed
            } else {
                 // If it's null/undefined/empty string, treat as valid (or invalid if required)
                 // JSON.parse('{}'); // Or handle as needed
            }
        } catch (e) {
            newErrors.visualization_params = 'Must be valid JSON.';
            isValid = false;
        }

        // Dimensions (ensure it's a string first)
        const dimensionsString = typeof params.dimensions === 'string' ? params.dimensions : '';
        if (dimensionsString) {
            if (!/^\d+x\d+$/.test(dimensionsString)) {
                newErrors.dimensions = 'Must be in format "WIDTHxHEIGHT" (e.g., 768x768).';
                isValid = false;
            }
        } else if (params.dimensions !== null && typeof params.dimensions === 'object') {
             // If it's an object, assume it's valid for now, or add deeper validation if needed
             // e.g., check for width/height properties
        } else {
             // If it's null/undefined/empty string, treat as invalid if required
             newErrors.dimensions = 'Dimensions are required.';
             isValid = false;
        }

        // Scale
        const scale = Number(params.scale);
        if (isNaN(scale) || scale <= 0) {
            newErrors.scale = 'Must be a positive number.';
            isValid = false;
        }

        // CRS (basic check - could be more complex)
        if (!params.crs || params.crs.trim() === '') {
            newErrors.crs = 'CRS cannot be empty.';
            isValid = false;
        }

        setErrors(newErrors);
        return isValid;
    };

    const handleSubmit = async () => {
        if (!validateParams()) {
            return;
        }

        // Prepare params for submission (convert types if necessary)
        const submissionParams: Partial<GeeParams> = {
            ...params,
            max_cloud_cover: Number(params.max_cloud_cover),
            images_number: Number(params.images_number),
            scale: Number(params.scale),
            // Keep vis_params as string, backend should handle parsing
        };

        try {
            await onSubmit(submissionParams);
            // onClose(); // Keep modal open until submission finishes? Parent decides.
        } catch (error) {
            // Error handling might be done in the parent component via the onSubmit promise
            console.error("Error submitting tweaked parameters:", error);
            // Optionally set a general error state within the modal
        }
    };

    const modalStyle = {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: '90%',
        maxWidth: 600,
        bgcolor: 'background.paper',
        boxShadow: 24,
        p: 4,
        maxHeight: '90vh',
        overflowY: 'auto',
    };

    return (
        <Modal
            open={open}
            onClose={onClose}
            aria-labelledby="tweak-gee-params-modal-title"
            aria-describedby="tweak-gee-params-modal-description"
        >
            <Box sx={modalStyle}>
                <Typography id="tweak-gee-params-modal-title" variant="h6" component="h2" gutterBottom>
                    Tweak GEE Parameters
                </Typography>

                <Stack spacing={2} sx={{ mt: 2 }}>
                    <TextField
                        label="Max Cloud Cover (%)"
                        name="max_cloud_cover"
                        type="number"
                        value={params.max_cloud_cover ?? ''}
                        onChange={handleChange}
                        error={!!errors.max_cloud_cover}
                        helperText={errors.max_cloud_cover}
                        fullWidth
                        inputProps={{ min: 0, max: 100, step: 1 }}
                    />
                    <TextField
                        label="Number of Images"
                        name="images_number"
                        type="number"
                        value={params.images_number ?? ''}
                        onChange={handleChange}
                        error={!!errors.images_number}
                        helperText={errors.images_number}
                        fullWidth
                        inputProps={{ min: 1, step: 1 }}
                    />
                    <TextField
                        label="Dimensions (WidthxHeight)"
                        name="dimensions"
                        value={params.dimensions ?? ''}
                        onChange={handleChange}
                        error={!!errors.dimensions}
                        helperText={errors.dimensions}
                        fullWidth
                    />
                    <TextField
                        label="Scale (meters)"
                        name="scale"
                        type="number"
                        value={params.scale ?? ''}
                        onChange={handleChange}
                        error={!!errors.scale}
                        helperText={errors.scale}
                        fullWidth
                        inputProps={{ min: 1 }}
                    />
                    <TextField
                        label="CRS (Coordinate Reference System)"
                        name="crs"
                        value={params.crs ?? ''}
                        onChange={handleChange}
                        error={!!errors.crs}
                        helperText={errors.crs}
                        fullWidth
                    />
                    <TextField
                        label="Visualization Parameters (JSON)"
                        name="visualization_params"
                        multiline
                        rows={4}
                        value={params.visualization_params ?? ''}
                        onChange={handleChange}
                        error={!!errors.visualization_params}
                        helperText={errors.visualization_params || 'Enter valid JSON for band visualization.'}
                        fullWidth
                        variant="outlined"
                        InputProps={{ style: { fontFamily: 'monospace' } }} // Use monospace for JSON
                    />

                    {/* Display general submission error if needed */}
                    {/* {submitError && <Alert severity="error">{submitError}</Alert>} */}

                </Stack>

                <Stack direction="row" spacing={2} sx={{ mt: 3, justifyContent: 'flex-end' }}>
                    <Button onClick={onClose} color="inherit" disabled={isSubmitting}>
                        Cancel
                    </Button>
                    <Button
                        variant="contained"
                        onClick={handleSubmit}
                        disabled={isSubmitting || Object.keys(errors).length > 0}
                        startIcon={isSubmitting ? <CircularProgress size={20} color="inherit" /> : null}
                    >
                        {isSubmitting ? 'Applying...' : 'Apply Parameters'}
                    </Button>
                </Stack>
            </Box>
        </Modal>
    );
};

export default GeeTweakParamsModal;
