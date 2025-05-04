import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, IconButton, Box, Stack
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import { SatelliteImage } from '@/types/image';

interface ImageFullPageProps {
  open: boolean;
  onClose: () => void;
  image: SatelliteImage | null;
}

const ImageFullPage: React.FC<ImageFullPageProps> = ({ open, onClose, image }) => {
  const [zoom, setZoom] = useState(1);
  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [startDrag, setStartDrag] = useState({ x: 0, y: 0 });

  const handleZoomIn = () => {
    setZoom((prevZoom) => Math.min(prevZoom + 0.2, 5)); // Limit zoom to 5x
  };

  const handleZoomOut = () => {
    setZoom((prevZoom) => Math.max(prevZoom - 0.2, 0.5)); // Limit zoom to 0.5x
  };

  const handleResetZoom = () => {
    setZoom(1); // Reset zoom to default
    setPosition({ x: 0, y: 0 }); // Reset position
  };

  const handleMouseDown = (event: React.MouseEvent) => {
    setIsDragging(true);
    setStartDrag({ x: event.clientX - position.x, y: event.clientY - position.y });
  };

  const handleMouseMove = (event: React.MouseEvent) => {
    if (!isDragging) return;
    const newX = event.clientX - startDrag.x;
    const newY = event.clientY - startDrag.y;
    setPosition({ x: newX, y: newY });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xl"
      fullWidth
      fullScreen
    >
      <DialogTitle sx={{ position: 'absolute', top: 0, left: 0, zIndex: 10 }}>
        <IconButton onClick={onClose} size="large" sx={{ color: 'white' }}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ padding: 0, backgroundColor: 'black', position: 'relative' }}>
        {image ? (
          <Box
            sx={{
              width: '100%',
              height: '100%',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              overflow: 'hidden',
              position: 'relative',
              cursor: isDragging ? 'grabbing' : 'grab',
            }}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp} // Stop dragging if the mouse leaves the container
          >
            <Box
              component="img"
              src={image.url}
              alt={image.filename}
              onDoubleClick={handleResetZoom} // Reset zoom on double-click
              sx={{
                transform: `scale(${zoom}) translate(${position.x}px, ${position.y}px)`,
                transformOrigin: 'center',
                transition: isDragging ? 'none' : 'transform 0.3s ease',
                maxWidth: '100%',
                maxHeight: '100%',
                objectFit: 'contain',
              }}
            />
          </Box>
        ) : (
          <Box
            sx={{
              width: '100%',
              height: '100%',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              color: 'white',
              fontSize: '1.5rem',
            }}
          >
            No image available
          </Box>
        )}
        <Stack
          direction="row"
          spacing={2}
          sx={{
            position: 'absolute',
            bottom: 16,
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 10,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            padding: 1,
            borderRadius: 2,
          }}
        >
          <IconButton
            onClick={handleZoomIn}
            sx={{ color: 'white', fontSize: '2rem' }} // Larger icon size
          >
            <ZoomInIcon fontSize="inherit" />
          </IconButton>
          <IconButton
            onClick={handleZoomOut}
            sx={{ color: 'white', fontSize: '2rem' }} // Larger icon size
          >
            <ZoomOutIcon fontSize="inherit" />
          </IconButton>
        </Stack>
      </DialogContent>
    </Dialog>
  );
};

export default ImageFullPage;
