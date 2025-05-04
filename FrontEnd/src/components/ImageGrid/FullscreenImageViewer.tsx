import React, { useState, useEffect } from 'react';
import {
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    Typography,
    Box,
    CircularProgress,
    Paper,
    Stack,
    Tooltip,
} from '@mui/material';
import {
    XMarkIcon,
    ChevronLeftIcon,
    ChevronRightIcon,
    MagnifyingGlassPlusIcon,
    MagnifyingGlassMinusIcon,
    ArrowPathIcon, // For reset zoom
} from '@heroicons/react/24/outline';
import { SatelliteImage } from '@/types/image'; // Assuming path is correct

interface FullscreenImageViewerProps {
    open: boolean;
    onClose: () => void;
    image: SatelliteImage | null;
    images: SatelliteImage[]; // Full list for navigation
    onNavigate: (direction: 'prev' | 'next') => void;
}

const FullscreenImageViewer: React.FC<FullscreenImageViewerProps> = ({
    open,
    onClose,
    image,
    images,
    onNavigate,
}) => {
    const [zoomLevel, setZoomLevel] = useState(1);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const currentImageIndex = image ? images.findIndex(img => img.id === image.id) : -1;
    const canNavigatePrev = currentImageIndex > 0;
    const canNavigateNext = currentImageIndex < images.length - 1;

    useEffect(() => {
        // Reset zoom and loading state when image changes or viewer opens/closes
        setZoomLevel(1);
        setLoading(true);
        setError(null);
        if (open && image) {
            // Preload image (optional, browser might handle caching)
            const img = new Image();
            img.src = image.url;
            img.onload = () => setLoading(false);
            img.onerror = () => {
                setError('Failed to load image.');
                setLoading(false);
            };
        } else {
            setLoading(false); // Ensure loading is false if not open or no image
        }
    }, [image, open]);

    const handleZoomIn = () => setZoomLevel(prev => Math.min(prev + 0.2, 3)); // Max zoom 3x
    const handleZoomOut = () => setZoomLevel(prev => Math.max(prev - 0.2, 0.5)); // Min zoom 0.5x
    const handleResetZoom = () => setZoomLevel(1);

    const handleClose = () => {
        onClose(); // Call the passed onClose handler
    };

    if (!image) {
        return null; // Don't render anything if no image is selected
    }

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth="xl" // Use a large width
            PaperProps={{
                sx: {
                    height: '90vh', // Limit height
                    display: 'flex',
                    flexDirection: 'column',
                },
            }}
        >
            <DialogTitle sx={{ p: 1, pr: 6, backgroundColor: 'grey.100' }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6" component="div" noWrap sx={{ flexGrow: 1, ml: 1 }}>
                        {image.filename || 'Image Viewer'}
                    </Typography>
                    <IconButton
                        aria-label="close"
                        onClick={handleClose}
                        sx={{
                            position: 'absolute',
                            right: 8,
                            top: 8,
                            color: (theme) => theme.palette.grey[500],
                        }}
                    >
                        <XMarkIcon className="h-6 w-6" />
                    </IconButton>
                </Stack>
            </DialogTitle>

            <DialogContent
                dividers
                sx={{
                    flexGrow: 1, // Allow content to take remaining space
                    display: 'flex',
                    flexDirection: 'column', // Stack image and controls vertically
                    overflow: 'hidden', // Prevent double scrollbars initially
                    p: 0, // Remove padding for full bleed image container
                    position: 'relative', // For positioning navigation buttons
                    backgroundColor: 'grey.200', // Background for the viewer area
                }}
            >
                {/* Navigation Buttons */}
                <IconButton
                    onClick={() => onNavigate('prev')}
                    disabled={!canNavigatePrev}
                    sx={{
                        position: 'absolute',
                        left: 16,
                        top: '50%',
                        transform: 'translateY(-50%)',
                        backgroundColor: 'rgba(0, 0, 0, 0.3)',
                        color: 'white',
                        '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.5)' },
                        zIndex: 1, // Ensure buttons are above image
                    }}
                >
                    <ChevronLeftIcon className="h-8 w-8" />
                </IconButton>
                <IconButton
                    onClick={() => onNavigate('next')}
                    disabled={!canNavigateNext}
                    sx={{
                        position: 'absolute',
                        right: 16,
                        top: '50%',
                        transform: 'translateY(-50%)',
                        backgroundColor: 'rgba(0, 0, 0, 0.3)',
                        color: 'white',
                        '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.5)' },
                        zIndex: 1,
                    }}
                >
                    <ChevronRightIcon className="h-8 w-8" />
                </IconButton>

                {/* Image Container */}
                <Box
                    sx={{
                        flexGrow: 1,
                        overflow: 'auto', // Enable scroll/pan when zoomed
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        position: 'relative', // Needed for absolute positioning of loading/error
                        p: 2, // Add some padding around the image
                    }}
                >
                    {loading && (
                        <CircularProgress sx={{ position: 'absolute' }} />
                    )}
                    {error && !loading && (
                        <Typography color="error" sx={{ position: 'absolute' }}>{error}</Typography>
                    )}
                    {!loading && !error && (
                        <img
                            src={image.url}
                            alt={image.filename || 'Satellite image'}
                            style={{
                                display: 'block', // Remove extra space below image
                                maxWidth: '100%',
                                maxHeight: '100%',
                                transform: `scale(${zoomLevel})`,
                                transition: 'transform 0.1s ease-out',
                                objectFit: 'contain', // Ensure image fits without distortion
                            }}
                        />
                    )}
                </Box>

                {/* Controls and Metadata Footer */}
                <Paper
                    elevation={2}
                    sx={{
                        p: 1,
                        backgroundColor: 'grey.100',
                        borderTop: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
                        {/* Metadata */}
                        <Box sx={{ overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>
                            <Typography variant="caption" display="inline">
                                {image.filename}
                            </Typography>
                            {image.captureDate && (
                                <Typography variant="caption" sx={{ ml: 1 }}>
                                    | Captured: {new Date(image.captureDate).toLocaleDateString()}
                                </Typography>
                            )}
                            {/* Add more metadata if needed */}
                        </Box>

                        {/* Zoom Controls */}
                        <Stack direction="row" alignItems="center" spacing={0.5}>
                            <Tooltip title="Zoom Out">
                                <IconButton size="small" onClick={handleZoomOut} disabled={zoomLevel <= 0.5}>
                                    <MagnifyingGlassMinusIcon className="h-5 w-5" />
                                </IconButton>
                            </Tooltip>
                            <Tooltip title="Reset Zoom">
                                <IconButton size="small" onClick={handleResetZoom} disabled={zoomLevel === 1}>
                                    <ArrowPathIcon className="h-5 w-5" />
                                </IconButton>
                            </Tooltip>
                            <Tooltip title="Zoom In">
                                <IconButton size="small" onClick={handleZoomIn} disabled={zoomLevel >= 3}>
                                    <MagnifyingGlassPlusIcon className="h-5 w-5" />
                                </IconButton>
                            </Tooltip>
                        </Stack>
                    </Stack>
                </Paper>
            </DialogContent>
        </Dialog>
    );
};

export default FullscreenImageViewer;
