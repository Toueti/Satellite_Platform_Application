import React, { useState } from 'react';
import { 
  Box, 
  Card, 
  CardContent, 
  CardMedia, 
  Typography, 
  IconButton, 
  Skeleton,
  Stack,
  Menu,
  MenuItem
} from '@mui/material';
import { SatelliteImage } from '@/types/image';
import { formatFileSize, formatDate } from '@/utils/formatters';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import PhotoSizeSelectActualIcon from '@mui/icons-material/PhotoSizeSelectActual';
import AddIcon from '@mui/icons-material/Add';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import LibraryAddIcon from '@mui/icons-material/LibraryAdd';
import GetAppIcon from '@mui/icons-material/GetApp';

interface ImageGridProps {
  images: SatelliteImage[];
  loading: boolean;
  onSelectImage: (image: SatelliteImage) => void;
  onDeleteImage?: (imageId: string) => void;
  onAnnotateImage?: (image: SatelliteImage) => void;
  onToggleFavorite?: (imageId: string) => void;
  favorites?: string[];
}

const ImageGrid: React.FC<ImageGridProps> = ({
  images,
  loading,
  onSelectImage,
  onDeleteImage,
  onAnnotateImage,
  onToggleFavorite,
  favorites = []
}) => {
  const [menuAnchorEl, setMenuAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  
  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, imageId: string) => {
    setMenuAnchorEl(event.currentTarget);
    setSelectedImageId(imageId);
  };
  
  const handleMenuClose = () => {
    setMenuAnchorEl(null);
    setSelectedImageId(null);
  };
  
  const handleSelect = (image: SatelliteImage) => {
    onSelectImage(image);
  };
  
  const handleDelete = () => {
    if (selectedImageId && onDeleteImage) {
      onDeleteImage(selectedImageId);
    }
    handleMenuClose();
  };
  
  const handleAnnotate = () => {
    if (selectedImageId && onAnnotateImage) {
      const image = images.find(img => img.id === selectedImageId);
      if (image) {
        onAnnotateImage(image);
      }
    }
    handleMenuClose();
  };
  
  const handleDownload = () => {
    if (selectedImageId) {
      const image = images.find(img => img.id === selectedImageId);
      if (image && image.url) {
        // Create a temporary link element to trigger the download
        const link = document.createElement('a');
        link.href = image.url;
        link.setAttribute('download', image.filename || `image-${image.id}`); // Set filename for download
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link); // Clean up the link
      }
    }
    handleMenuClose();
  };

  const handleFavoriteToggle = (imageId: string, event: React.MouseEvent) => {
    event.stopPropagation();
    if (onToggleFavorite) {
      onToggleFavorite(imageId);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        {[...Array(8)].map((_, index) => (
          <Box key={index} sx={{ width: { xs: '100%', sm: '47%', md: '31%', lg: '23%' }, mb: 2 }}>
            <Skeleton variant="rectangular" height={160} />
            <Skeleton variant="text" height={30} sx={{ mt: 1 }} />
            <Skeleton variant="text" width="60%" />
          </Box>
        ))}
      </Box>
    );
  }

  if (images.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">
          No images found
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Try adjusting your filters or upload new images
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
      {images.map((image) => (
        <Box 
          key={image.id} 
          sx={{ 
            width: { xs: '100%', sm: '47%', md: '31%', lg: '23%' },
            mb: 2
          }}
        >
          <Card 
            sx={{ 
              height: '100%', 
              display: 'flex', 
              flexDirection: 'column',
              transition: 'transform 0.2s',
              '&:hover': {
                transform: 'scale(1.02)',
                boxShadow: 3
              },
              cursor: 'pointer'
            }}
            onClick={() => handleSelect(image)}
          >
            <Box sx={{ position: 'relative' }}>
              <CardMedia
                component="img"
                height="160"
                image={image.thumbnailUrl}
                alt={image.filename}
              />
              <Box sx={{ 
                position: 'absolute', 
                top: 8, 
                right: 8, 
                display: 'flex', 
                gap: 1 
              }}>
                <IconButton 
                  size="small" 
                  sx={{ 
                    bgcolor: 'rgba(255,255,255,0.8)', 
                    '&:hover': { bgcolor: 'rgba(255,255,255,0.95)' } 
                  }}
                  onClick={(e) => handleFavoriteToggle(image.id, e)}
                >
                  {favorites.includes(image.id) ? 
                    <StarIcon fontSize="small" color="warning" /> : 
                    <StarBorderIcon fontSize="small" />
                  }
                </IconButton>
                
                <IconButton 
                  size="small" 
                  sx={{ 
                    bgcolor: 'rgba(255,255,255,0.8)', 
                    '&:hover': { bgcolor: 'rgba(255,255,255,0.95)' } 
                  }}
                  onClick={(e) => handleMenuOpen(e, image.id)}
                >
                  <MoreVertIcon fontSize="small" />
                </IconButton>
              </Box>
            </Box>
            
            <CardContent sx={{ flexGrow: 1 }}>
              <Typography variant="h6" noWrap>
                {image.filename}
              </Typography>
              
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                <Typography 
                  variant="caption" 
                  sx={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: 0.5 
                  }}
                >
                  <CalendarTodayIcon fontSize="inherit" /> {formatDate(image.uploadDate)} {/* Changed captureDate to uploadDate */}
                </Typography>
                <Typography 
                  variant="caption" 
                  sx={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: 0.5 
                  }}
                >
                  <PhotoSizeSelectActualIcon fontSize="inherit" /> 
                  {(() => { // IIFE for logging
                    const sizeValue = image.size;
                    console.log(`Rendering image ${image.id}: size = ${sizeValue}, type = ${typeof sizeValue}`); // DEBUG LOG
                    return formatFileSize(sizeValue);
                  })()}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Box>
      ))}
      
      <Menu
        anchorEl={menuAnchorEl}
        open={Boolean(menuAnchorEl)}
        onClose={handleMenuClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <MenuItem key="annotate" onClick={() => {
          handleAnnotate();
          handleMenuClose();
        }}>
          <EditIcon fontSize="small" sx={{ mr: 1 }} /> Annotate
        </MenuItem>
        <MenuItem key="delete" onClick={() => {
          handleDelete();
          handleMenuClose();
        }}>
          <DeleteIcon fontSize="small" sx={{ mr: 1 }} /> Delete
        </MenuItem>
        <MenuItem key="download" onClick={() => {
          handleDownload();
          handleMenuClose();
        }}>
          <GetAppIcon fontSize="small" sx={{ mr: 1 }} /> Download
        </MenuItem>
      </Menu>
    </Box>
  );
};

export default ImageGrid;
