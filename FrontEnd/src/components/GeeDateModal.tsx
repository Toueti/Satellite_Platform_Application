import React, { useState, useEffect } from 'react';
import {
    Modal,
    Box,
    Typography,
    Button,
    Stack,
    TextField,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Divider,
    InputAdornment,
    // Grid removed
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { Dayjs } from 'dayjs';
import { GeeParams } from '@/services/gee.service'; // Import GeeParams type

// Define the type for advanced parameters we allow editing here
type AdvancedGeeParams = Omit<
    Partial<GeeParams>,
    'collection_id' | 'region' | 'start_date' | 'end_date' | 'metadata' | 'export_format' | 'export_destination'
> & {
    // Ensure dimensions and visualization_params can hold intermediate string state from TextField
    dimensions?: string | { width: number; height: number };
    visualization_params?: string | object;
};

interface GeeDateModalProps {
    open: boolean;
    onClose: () => void;
    onSubmit: (
        startDate: Dayjs | null,
        endDate: Dayjs | null,
        advancedParams: AdvancedGeeParams // Send the collected advanced params
    ) => void;
    initialStartDate?: Dayjs | null;
    initialEndDate?: Dayjs | null;
}

// Default values for advanced parameters
const defaultAdvancedParams: AdvancedGeeParams = {
    max_cloud_cover: 40, // Updated default
    images_number: 1,
    bands: ['B4', 'B3', 'B2'],
    visualization_params: { bands: ["B4", "B3", "B2"], min: 0, max: 3000 },
    dimensions: { width: 2048, height: 2048 }, // Updated default
    scale: 10, // Updated default
    crs: 'EPSG:4326',
};

const modalStyle = {
    position: 'absolute' as 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: { xs: '90%', sm: 500 },
    maxHeight: '90vh',
    overflowY: 'auto',
    bgcolor: 'background.paper',
    border: '1px solid #ccc',
    borderRadius: 1,
    boxShadow: 24,
    p: 4,
};

const GeeDateModal = ({
    open,
    onClose,
    onSubmit,
    initialStartDate,
    initialEndDate,
}: GeeDateModalProps) => {
    const [startDate, setStartDate] = useState<Dayjs | null>(null);
    const [endDate, setEndDate] = useState<Dayjs | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);

    const [advancedParams, setAdvancedParams] = useState<AdvancedGeeParams>(defaultAdvancedParams);

    const handleAdvancedParamChange = (param: keyof AdvancedGeeParams, value: any) => {
        setAdvancedParams(prev => ({ ...prev, [param]: value }));
    };

    // Handler for visualization_params - store string directly from TextField
    const handleVisParamsChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        handleAdvancedParamChange('visualization_params', event.target.value);
    };

     // Handler for dimensions - store string directly from TextField
     const handleDimensionsChange = (event: React.ChangeEvent<HTMLInputElement>) => {
         handleAdvancedParamChange('dimensions', event.target.value);
     };

     // Handler for bands - parse comma-separated string
     const handleBandsChange = (event: React.ChangeEvent<HTMLInputElement>) => {
         const value = event.target.value;
         const bandsArray = value.split(',').map(b => b.trim()).filter(b => b !== '');
         handleAdvancedParamChange('bands', bandsArray);
     };

    useEffect(() => {
        if (open) {
            setStartDate(initialStartDate || null);
            setEndDate(initialEndDate || null);
            setError(null);
            // Reset advanced params to default, ensuring deep copy for objects
            setAdvancedParams(JSON.parse(JSON.stringify(defaultAdvancedParams)));
            setIsAdvancedOpen(false);
        } else {
            setStartDate(null);
            setEndDate(null);
            setError(null);
        }
    }, [open, initialStartDate, initialEndDate]);

    const handleSubmit = () => {
        setError(null);
        if (!startDate || !endDate) {
            setError('Please select both a start and end date.');
            return;
        }
        if (endDate.isBefore(startDate)) {
            setError('End date cannot be before start date.');
            return;
        }

        let finalAdvancedParams = { ...advancedParams };

        // Parse visualization_params if it's a string
        if (typeof finalAdvancedParams.visualization_params === 'string') {
            try {
                // Allow empty string to mean "use default" or "no params"
                // Let's treat empty string as deleting the param, service should use default
                if (finalAdvancedParams.visualization_params.trim() === '') {
                    delete finalAdvancedParams.visualization_params;
                } else {
                    finalAdvancedParams.visualization_params = JSON.parse(finalAdvancedParams.visualization_params);
                }
            } catch (e) {
                setError('Invalid JSON format for Visualization Parameters.');
                return;
            }
        }

        // Parse dimensions if it's a string 'WxH'
        if (typeof finalAdvancedParams.dimensions === 'string') {
            try {
                 // Treat empty string as deleting the param
                 if (finalAdvancedParams.dimensions.trim() === '') {
                    delete finalAdvancedParams.dimensions;
                 } else {
                    const parts = finalAdvancedParams.dimensions.split('x');
                    if (parts.length === 2 && !isNaN(parseInt(parts[0])) && !isNaN(parseInt(parts[1]))) {
                        finalAdvancedParams.dimensions = { width: parseInt(parts[0]), height: parseInt(parts[1]) };
                    } else {
                        throw new Error("Invalid dimensions format. Use 'WidthxHeight' (e.g., '768x768').");
                    }
                 }
            } catch (e: any) {
                 setError(e.message || 'Invalid format for Dimensions.');
                 return;
            }
        }

        onSubmit(startDate, endDate, finalAdvancedParams);
        onClose();
    };

    // Helper to get string representation for text fields
    const getVisParamsString = (): string => {
        const param = advancedParams.visualization_params;
        if (typeof param === 'object' && param !== null) {
            try {
                return JSON.stringify(param, null, 2);
            } catch {
                return ''; // Should not happen with valid objects, but fallback
            }
        }
        return typeof param === 'string' ? param : '';
    };

    const getDimensionsString = (): string => {
        const param = advancedParams.dimensions;
        if (typeof param === 'object' && param !== null) {
            // Ensure width and height exist before accessing
            if ('width' in param && 'height' in param) {
                 return `${param.width}x${param.height}`;
            }
            return ''; // Invalid object structure
        }
        return typeof param === 'string' ? param : '';
    };


    return (
        <Modal
            open={open}
            onClose={onClose}
            aria-labelledby="gee-date-modal-title"
            aria-describedby="gee-date-modal-description"
        >
            <Box sx={modalStyle}>
                <Typography id="gee-date-modal-title" variant="h6" component="h2" gutterBottom>
                    Select Date Range & Options
                </Typography>
                <LocalizationProvider dateAdapter={AdapterDayjs}>
                    <Stack spacing={3} sx={{ mt: 2 }}>
                        <DatePicker
                            label="Start Date"
                            value={startDate}
                            onChange={(newValue) => setStartDate(newValue)}
                            slotProps={{ textField: { fullWidth: true, error: !!error && (!startDate || !endDate || endDate.isBefore(startDate)) } }}
                        />
                        <DatePicker
                            label="End Date"
                            value={endDate}
                            onChange={(newValue) => setEndDate(newValue)}
                            slotProps={{ textField: { fullWidth: true, error: !!error && (!startDate || !endDate || endDate.isBefore(startDate)) } }}
                            minDate={startDate || undefined}
                        />

                        <Accordion expanded={isAdvancedOpen} onChange={() => setIsAdvancedOpen(!isAdvancedOpen)} sx={{ border: '1px solid rgba(0, 0, 0, 0.12)', boxShadow: 'none' }}>
                            <AccordionSummary
                                expandIcon={<ExpandMoreIcon />}
                                aria-controls="advanced-gee-params-content"
                                id="advanced-gee-params-header"
                            >
                                <Typography variant="subtitle1">Advanced GEE Options</Typography>
                            </AccordionSummary>
                            <AccordionDetails>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                    {/* Row 1: Cloud Cover, Image Number */}
                                    <Box sx={{ display: 'flex', gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
                                        <TextField
                                            label="Max Cloud Cover"
                                            type="number"
                                            sx={{ flexGrow: 1 }}
                                            value={advancedParams.max_cloud_cover ?? ''}
                                            onChange={(e) => handleAdvancedParamChange('max_cloud_cover', e.target.value === '' ? undefined : Number(e.target.value))}
                                            fullWidth
                                            InputProps={{
                                                endAdornment: <InputAdornment position="end">%</InputAdornment>,
                                                inputProps: { min: 0, max: 100 }
                                            }}
                                            variant="outlined"
                                            size="small"
                                        />
                                        <TextField
                                            label="Number of Images"
                                            type="number"
                                            sx={{ flexGrow: 1 }}
                                            value={advancedParams.images_number ?? ''}
                                            onChange={(e) => handleAdvancedParamChange('images_number', e.target.value === '' ? undefined : Number(e.target.value))}
                                            fullWidth
                                             InputProps={{ inputProps: { min: 1 } }}
                                            variant="outlined"
                                            size="small"
                                        />
                                    </Box>
                                    {/* Row 2: Bands */}
                                    <TextField
                                        label="Bands (comma-separated)"
                                        value={(advancedParams.bands || []).join(', ')}
                                        onChange={handleBandsChange}
                                        fullWidth
                                        placeholder="e.g., B4, B3, B2"
                                        variant="outlined"
                                        size="small"
                                    />
                                    {/* Row 3: Vis Params */}
                                    <TextField
                                        label="Visualization Params (JSON)"
                                        multiline
                                        rows={3}
                                        value={getVisParamsString()} // Use helper
                                        onChange={handleVisParamsChange}
                                        fullWidth
                                        placeholder='e.g., {"bands": ["B4", "B3", "B2"], "min": 0, "max": 3000}'
                                        variant="outlined"
                                        size="small"
                                    />
                                    {/* Row 4: Dimensions, Scale */}
                                    <Box sx={{ display: 'flex', gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
                                        <TextField
                                            label="Dimensions (WidthxHeight)"
                                            value={getDimensionsString()} // Use helper
                                            onChange={handleDimensionsChange}
                                            fullWidth
                                            sx={{ flexGrow: 1 }}
                                            placeholder="e.g., 768x768"
                                            variant="outlined"
                                            size="small"
                                        />
                                        <TextField
                                            label="Scale (meters)"
                                            type="number"
                                            value={advancedParams.scale ?? ''}
                                            onChange={(e) => handleAdvancedParamChange('scale', e.target.value === '' ? undefined : Number(e.target.value))}
                                            fullWidth
                                            sx={{ flexGrow: 1 }}
                                            InputProps={{ inputProps: { min: 1 } }}
                                            variant="outlined"
                                            size="small"
                                        />
                                    </Box>
                                    {/* Row 5: CRS */}
                                    <TextField
                                        label="CRS (Coordinate Reference System)"
                                        value={advancedParams.crs ?? ''}
                                        onChange={(e) => handleAdvancedParamChange('crs', e.target.value)}
                                        fullWidth
                                        placeholder="e.g., EPSG:4326"
                                        variant="outlined"
                                        size="small"
                                    />
                                </Box>
                            </AccordionDetails>
                        </Accordion>

                        {error && (
                            <Typography color="error" variant="caption" sx={{ mt: 1 }}>
                                {error}
                            </Typography>
                        )}

                        <Stack direction="row" spacing={2} justifyContent="flex-end" sx={{ mt: 3 }}>
                            <Button onClick={onClose} color="inherit">
                                Cancel
                            </Button>
                            <Button
                                onClick={handleSubmit}
                                variant="contained"
                                disabled={!startDate || !endDate}
                            >
                                Submit
                            </Button>
                        </Stack>
                    </Stack>
                </LocalizationProvider>
            </Box>
        </Modal>
    );
};

export default GeeDateModal;
