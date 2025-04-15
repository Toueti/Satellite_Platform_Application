import React from 'react';
import { 
  Dialog, 
  DialogContent, 
  IconButton, 
  Box,
  Typography,
  Paper
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { SatelliteImage } from '@/types/image';
import { formatFileSize, formatDate } from '@/utils/formatters';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import PhotoSizeSelectActualIcon from '@mui/icons-material/PhotoSizeSelectActual';

interface FullscreenImageViewerProps {
  open: boolean;
  image: SatelliteImage | null;
  onClose: () => void;
}

const FullscreenImageViewer: React.FC<FullscreenImageViewerProps> = ({ 
  open, 
  image, 
  onClose 
}) => {
  if (!image) return null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xl"
      fullWidth
      PaperProps={{
        sx: {
          bgcolor: 'rgba(0, 0, 0, 0.9)',
          color: 'white',
          height: '100vh',
          margin: 0,
          maxHeight: '100vh',
          borderRadius: 0,
        }
      }}
    >
      <Box sx={{ 
        position: 'absolute', 
        top: 16, 
        right: 16, 
        zIndex: 10 
      }}>
        <IconButton 
          onClick={onClose} 
          sx={{ 
            color: 'white',
            bgcolor: 'rgba(0, 0, 0, 0.5)',
            '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.7)' }
          }}
        >
          <CloseIcon />
        </IconButton>
      </Box>

      <DialogContent sx={{ 
        display: 'flex', 
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        p: 0,
        height: '100%',
        overflow: 'hidden'
      }}>
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center',
          height: '90%',
          width: '100%',
          overflow: 'hidden'
        }}>
          <img 
            src={image.url} 
            alt={image.filename} 
            style={{ 
              maxHeight: '100%', 
              maxWidth: '100%', 
              objectFit: 'contain'
            }} 
          />
        </Box>

        <Paper elevation={3} sx={{ 
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          p: 2,
          bgcolor: 'rgba(0, 0, 0, 0.7)',
          color: 'white',
          borderRadius: 0
        }}>
          <Typography variant="h6">{image.filename}</Typography>
          <Box sx={{ display: 'flex', gap: 2, mt: 1 }}>
            <Typography 
              variant="body2" 
              sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: 0.5 
              }}
            >
              <CalendarTodayIcon fontSize="small" /> {formatDate(image.uploadDate)}
            </Typography>
            <Typography 
              variant="body2" 
              sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: 0.5 
              }}
            >
              <PhotoSizeSelectActualIcon fontSize="small" /> {formatFileSize(image.size)}
            </Typography>
          </Box>
        </Paper>
      </DialogContent>
    </Dialog>
  );
};

export default FullscreenImageViewer;