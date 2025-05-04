
import React from 'react';
import {
    Grid,
    Card,
    CardMedia,
    CardContent,
    Typography,
    CardActions,
    IconButton,
    Chip,
    Box,
    CircularProgress,
    Tooltip,
} from '@mui/material';
import {
    EyeIcon,
    PencilSquareIcon,
    TrashIcon,
    StarIcon as StarOutlineIcon,
} from '@heroicons/react/24/outline';
import { StarIcon as StarSolidIcon } from '@heroicons/react/24/solid';
import { SatelliteImage } from '@/types/image'; // Assuming path is correct

interface ImageGridProps {
    images: SatelliteImage[];
    loading?: boolean; // Optional loading state
    onSelectImage: (image: SatelliteImage) => void; // Renamed from onImageView for clarity
    onAnnotateImage?: (image: SatelliteImage) => void; // Optional annotation handler
    onDeleteImage?: (imageId: string) => void; // Optional delete handler
    onToggleFavorite?: (imageId: string) => void; // Optional favorite handler
    favoriteImages?: string[]; // List of favorite image IDs
}

const ImageGrid: React.FC<ImageGridProps> = ({
    images,
    loading = false,
    onSelectImage,
    onAnnotateImage,
    onDeleteImage,
    onToggleFavorite,
    favoriteImages = [],
}) => {
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 8 }}>
                <CircularProgress size={40} sx={{ mr: 2 }} />
                <Typography>Loading Images...</Typography>
            </Box>
        );
    }

    if (!images || images.length === 0) {
        // This case might be handled by the parent component (ProjectImagesTabPanel)
        // but providing a fallback here is good practice.
        return (
            <Box sx={{ textAlign: 'center', py: 8 }}>
                <Typography variant="body1" color="text.secondary">
                    No images found.
                </Typography>
            </Box>
        );
    }

    const formatBytes = (bytes: number, decimals = 2) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    } // Ensure no semicolon here

    return (
        <Box
            sx={{
                display: 'grid',
                gap: 3, // Corresponds to spacing={3}
                gridTemplateColumns: {
                    xs: 'repeat(1, 1fr)', // 1 column on extra-small screens
                    sm: 'repeat(2, 1fr)', // 2 columns on small screens
                    md: 'repeat(3, 1fr)', // 3 columns on medium screens
                    lg: 'repeat(4, 1fr)', // 4 columns on large screens
                },
            }}
        >
            {images.map((image) => (
                // Replace inner Grid item with a simple div or Box (handled below)
                <Box key={image.id}> {/* Use Box instead of Grid item */}
                    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <CardMedia
                            component="img"
                            height="160"
                            image={image.thumbnailUrl || image.url || '/placeholder-image.png'} // Fallback placeholder
                            alt={image.filename || 'Satellite Image'}
                            sx={{ objectFit: 'cover', cursor: 'pointer' }}
                            onClick={() => onSelectImage(image)} // Use main select handler for image click
                        />
                        <CardContent sx={{ flexGrow: 1 }}>
                            <Tooltip title={image.filename || 'No filename'} placement="top">
                                <Typography
                                    gutterBottom
                                    variant="subtitle2"
                                    component="div"
                                    noWrap // Prevent long filenames from breaking layout
                                >
                                    {image.filename || 'Unnamed Image'}
                                </Typography>
                            </Tooltip>
                            <Typography variant="caption" color="text.secondary" display="block">
                                ID: {image.id}
                            </Typography>
                            {image.captureDate && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                    Captured: {new Date(image.captureDate).toLocaleDateString()}
                                </Typography>
                            )}
                            {image.uploadDate && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                    Uploaded: {new Date(image.uploadDate).toLocaleDateString()}
                                </Typography>
                            )}
                             <Typography variant="caption" color="text.secondary" display="block">
                                Size: {formatBytes(image.size || 0)}
                            </Typography>
                            {image.tags && image.tags.length > 0 && (
                                <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                    {image.tags.slice(0, 3).map((tag) => ( // Limit displayed tags
                                        <Chip key={tag} label={tag} size="small" />
                                    ))}
                                    {image.tags.length > 3 && (
                                        <Chip label={`+${image.tags.length - 3}`} size="small" />
                                    )}
                                </Box>
                            )}
                        </CardContent>
                        <CardActions sx={{ justifyContent: 'space-between', pt: 0 }}>
                            <Box>
                                <Tooltip title="View Details">
                                    <IconButton size="small" onClick={() => onSelectImage(image)}>
                                        <EyeIcon className="h-5 w-5" />
                                    </IconButton>
                                </Tooltip>
                                {onAnnotateImage && (
                                    <Tooltip title="Annotate">
                                        <IconButton size="small" onClick={() => onAnnotateImage(image)}>
                                            <PencilSquareIcon className="h-5 w-5" />
                                        </IconButton>
                                    </Tooltip>
                                )}
                                {onDeleteImage && (
                                    <Tooltip title="Delete Image">
                                        <IconButton size="small" color="error" onClick={() => onDeleteImage(image.id)}>
                                            <TrashIcon className="h-5 w-5" />
                                        </IconButton>
                                    </Tooltip>
                                )}
                            </Box>
                            {onToggleFavorite && (
                                <Tooltip title={favoriteImages.includes(image.id) ? "Remove from Favorites" : "Add to Favorites"}>
                                    <IconButton size="small" onClick={() => onToggleFavorite(image.id)}>
                                        {favoriteImages.includes(image.id) ? (
                                            <StarSolidIcon className="h-5 w-5 text-yellow-500" />
                                        ) : (
                                            <StarOutlineIcon className="h-5 w-5" />
                                        )}
                                    </IconButton>
                                </Tooltip>
                            )}
                        </CardActions>
                    </Card>
                </Box> 
            ))}
        </Box> // End of Box wrapping the grid
    );
};

export default ImageGrid;
