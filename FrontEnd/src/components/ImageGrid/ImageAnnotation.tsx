import React, { useState, useRef, useEffect } from 'react';
import { 
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  Box, Stack, Typography, TextField, IconButton, Tooltip, Radio, RadioGroup,
  FormControlLabel, FormControl, FormLabel, Slider, Paper
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import UndoIcon from '@mui/icons-material/Undo';
import DeleteIcon from '@mui/icons-material/Delete';
import { SatelliteImage, ImageAnnotation as ImageAnnotationType } from '@/types/image';

// Color palette for annotations
const COLOR_PALETTE = [
  '#f44336', '#e91e63', '#9c27b0', '#673ab7', '#3f51b5',
  '#2196f3', '#03a9f4', '#00bcd4', '#009688', '#4caf50',
  '#8bc34a', '#cddc39', '#ffeb3b', '#ffc107', '#ff9800',
];

interface ImageAnnotationProps {
  open: boolean;
  onClose: () => void;
  image: SatelliteImage | null;
  onSave: (imageId: string, annotations: ImageAnnotationType[]) => void;
}

const ImageAnnotationDialog: React.FC<ImageAnnotationProps> = ({
  open,
  onClose,
  image,
  onSave
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const contextRef = useRef<CanvasRenderingContext2D | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [annotations, setAnnotations] = useState<ImageAnnotationType[]>([]);
  const [currentAnnotation, setCurrentAnnotation] = useState<Partial<ImageAnnotationType>>({
    type: 'rectangle',
    color: COLOR_PALETTE[0],
    label: '',
    description: '',
  });
  const [startPoint, setStartPoint] = useState<[number, number] | null>(null);
  const [points, setPoints] = useState<[number, number][]>([]);
  const [selectedAnnotationId, setSelectedAnnotationId] = useState<string | null>(null);

  // Initialize canvas and load existing annotations when the image changes
  useEffect(() => {
    if (open && image && canvasRef.current) {
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');
      
      if (context) {
        // Set canvas dimensions to match image
        const img = new Image();
        img.src = image.url;
        
        img.onload = () => {
          // Set canvas dimensions
          canvas.width = img.width;
          canvas.height = img.height;
          
          // Draw the image
          context.drawImage(img, 0, 0, img.width, img.height);
          
          // Store the context
          contextRef.current = context;
          
          // Load existing annotations
          setAnnotations(image.annotations || []);
          drawAllAnnotations();
        };
      }
    }
  }, [open, image]);

  const drawAllAnnotations = () => {
    if (!contextRef.current || !image) return;
    
    const ctx = contextRef.current;
    const img = new Image();
    img.src = image.url;
    
    // Redraw image and all annotations
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.drawImage(img, 0, 0, ctx.canvas.width, ctx.canvas.height);
    
    annotations.forEach((annotation) => {
      ctx.strokeStyle = annotation.color;
      ctx.lineWidth = 2;
      ctx.beginPath();
      
      if (annotation.type === 'polygon' && Array.isArray(annotation.coordinates)) {
        const coords = annotation.coordinates as number[][];
        if (coords.length > 0) {
          ctx.moveTo(coords[0][0], coords[0][1]);
          for (let i = 1; i < coords.length; i++) {
            ctx.lineTo(coords[i][0], coords[i][1]);
          }
          ctx.closePath();
        }
      } else if (annotation.type === 'rectangle' && Array.isArray(annotation.coordinates)) {
        const [startCoord, endCoord] = annotation.coordinates as number[][];
        const width = endCoord[0] - startCoord[0];
        const height = endCoord[1] - startCoord[1];
        ctx.rect(startCoord[0], startCoord[1], width, height);
      } else if (annotation.type === 'point' && Array.isArray(annotation.coordinates)) {
        const [x, y] = annotation.coordinates as [number, number];
        ctx.arc(x, y, 5, 0, 2 * Math.PI);
      }
      
      ctx.stroke();
      
      // Add label if exists
      if (annotation.label) {
        ctx.font = '14px Arial';
        ctx.fillStyle = annotation.color;
        if (annotation.type === 'polygon' && Array.isArray(annotation.coordinates)) {
          // For polygon, place the text at the first point
          const coords = annotation.coordinates as number[][];
          if (coords.length > 0) {
            ctx.fillText(annotation.label, coords[0][0], coords[0][1] - 5);
          }
        } else if (annotation.type === 'rectangle' && Array.isArray(annotation.coordinates)) {
          // For rectangle, place text at top-left
          const [startCoord] = annotation.coordinates as number[][];
          ctx.fillText(annotation.label, startCoord[0], startCoord[1] - 5);
        } else if (annotation.type === 'point' && Array.isArray(annotation.coordinates)) {
          // For point, place text nearby
          const [x, y] = annotation.coordinates as [number, number];
          ctx.fillText(annotation.label, x + 10, y - 5);
        }
      }
    });
  };

  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!contextRef.current) return;
    
    const rect = canvasRef.current!.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    setIsDrawing(true);
    
    if (currentAnnotation.type === 'polygon') {
      // For polygon, collect points
      if (!startPoint) {
        setStartPoint([x, y]);
        setPoints([[x, y]]);
      } else {
        setPoints([...points, [x, y]]);
      }
    } else if (currentAnnotation.type === 'rectangle') {
      // For rectangle, set start point
      setStartPoint([x, y]);
    } else if (currentAnnotation.type === 'point') {
      // For point, create the point immediately
      const newAnnotation: ImageAnnotationType = {
        id: Date.now().toString(),
        type: 'point',
        coordinates: [x, y],
        color: currentAnnotation.color || '#f44336',
        label: currentAnnotation.label || '',
        description: currentAnnotation.description || '',
        createdAt: new Date().toISOString(),
        createdBy: 'current-user', // This would be replaced by actual user ID
      };
      
      setAnnotations([...annotations, newAnnotation]);
      drawAllAnnotations();
      setIsDrawing(false);
    }
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isDrawing || !contextRef.current || !startPoint) return;
    
    const rect = canvasRef.current!.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    if (currentAnnotation.type === 'rectangle') {
      // For rectangle, draw a preview
      drawAllAnnotations(); // Redraw everything
      
      const ctx = contextRef.current;
      ctx.strokeStyle = currentAnnotation.color || '#f44336';
      ctx.lineWidth = 2;
      ctx.beginPath();
      
      const width = x - startPoint[0];
      const height = y - startPoint[1];
      ctx.rect(startPoint[0], startPoint[1], width, height);
      ctx.stroke();
    }
  };

  const handleMouseUp = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isDrawing || !contextRef.current) return;
    
    const rect = canvasRef.current!.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    if (currentAnnotation.type === 'rectangle' && startPoint) {
      // Create rectangle annotation
      const newAnnotation: ImageAnnotationType = {
        id: Date.now().toString(),
        type: 'rectangle',
        coordinates: [startPoint, [x, y]],
        color: currentAnnotation.color || '#f44336',
        label: currentAnnotation.label || '',
        description: currentAnnotation.description || '',
        createdAt: new Date().toISOString(),
        createdBy: 'current-user', // This would be replaced by actual user ID
      };
      
      setAnnotations([...annotations, newAnnotation]);
      setStartPoint(null);
    }
    
    setIsDrawing(false);
    drawAllAnnotations();
  };

  const handleFinishPolygon = () => {
    if (points.length < 3) {
      alert('A polygon must have at least 3 points');
      return;
    }
    
    // Create polygon annotation
    const newAnnotation: ImageAnnotationType = {
      id: Date.now().toString(),
      type: 'polygon',
      coordinates: points,
      color: currentAnnotation.color || '#f44336',
      label: currentAnnotation.label || '',
      description: currentAnnotation.description || '',
      createdAt: new Date().toISOString(),
      createdBy: 'current-user', // This would be replaced by actual user ID
    };
    
    setAnnotations([...annotations, newAnnotation]);
    setStartPoint(null);
    setPoints([]);
    drawAllAnnotations();
  };

  const handleClearPoints = () => {
    setStartPoint(null);
    setPoints([]);
    drawAllAnnotations();
  };

  const handleDeleteAnnotation = (id: string) => {
    setAnnotations(annotations.filter(a => a.id !== id));
    setSelectedAnnotationId(null);
    setTimeout(() => drawAllAnnotations(), 0);
  };

  const handleSaveAnnotations = () => {
    if (!image) return;
    onSave(image.id, annotations);
    onClose();
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="lg" 
      fullWidth
    >
      <DialogTitle>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6">
            Image Annotation: {image?.filename}
          </Typography>
          <IconButton onClick={onClose} size="small">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 66.66%' } }}>
            <Box sx={{ position: 'relative', border: '1px solid #ccc', mb: 2 }}>
              <canvas
                ref={canvasRef}
                onMouseDown={handleMouseDown}
                onMouseMove={handleMouseMove}
                onMouseUp={handleMouseUp}
                style={{ 
                  maxWidth: '100%', 
                  maxHeight: '600px',
                  display: 'block',
                  cursor: isDrawing ? 'crosshair' : 'default'
                }}
              />
              {points.length > 0 && (
                <Box sx={{ position: 'absolute', top: 8, right: 8, display: 'flex', gap: 1 }}>
                  <Button
                    variant="contained"
                    color="primary"
                    size="small"
                    onClick={handleFinishPolygon}
                  >
                    Finish Polygon
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    size="small"
                    onClick={handleClearPoints}
                  >
                    Clear
                  </Button>
                </Box>
              )}
            </Box>
          </Box>

          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 33.33%' } }}>
            <Typography variant="h6" gutterBottom>
              Annotation Controls
            </Typography>
            <Stack spacing={2}>
              <FormControl component="fieldset" sx={{ mb: 2 }}>
                <FormLabel component="legend">Annotation Type</FormLabel>
                <RadioGroup 
                  row 
                  value={currentAnnotation.type} 
                  onChange={(e) => setCurrentAnnotation({...currentAnnotation, type: e.target.value as any})}
                >
                  <FormControlLabel value="point" control={<Radio />} label="Point" />
                  <FormControlLabel value="rectangle" control={<Radio />} label="Rectangle" />
                  <FormControlLabel value="polygon" control={<Radio />} label="Polygon" />
                </RadioGroup>
              </FormControl>
              
              <Box sx={{ mb: 2 }}>
                <FormLabel component="legend" sx={{ mb: 1 }}>Color</FormLabel>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {COLOR_PALETTE.map((color) => (
                    <Tooltip key={color} title={color}>
                      <Box 
                        sx={{ 
                          width: 24, 
                          height: 24, 
                          bgcolor: color, 
                          borderRadius: '50%',
                          cursor: 'pointer',
                          border: currentAnnotation.color === color ? '2px solid black' : 'none',
                        }}
                        onClick={() => setCurrentAnnotation({...currentAnnotation, color})}
                      />
                    </Tooltip>
                  ))}
                </Box>
              </Box>
              
              <TextField
                label="Label"
                fullWidth
                margin="dense"
                value={currentAnnotation.label}
                onChange={(e) => setCurrentAnnotation({...currentAnnotation, label: e.target.value})}
                sx={{ mb: 2 }}
              />
              
              <TextField
                label="Description"
                fullWidth
                multiline
                rows={3}
                margin="dense"
                value={currentAnnotation.description}
                onChange={(e) => setCurrentAnnotation({...currentAnnotation, description: e.target.value})}
                sx={{ mb: 2 }}
              />
              
              <Typography variant="subtitle2" gutterBottom>
                Existing Annotations ({annotations.length})
              </Typography>
              
              {annotations.length > 0 ? (
                <Box sx={{ maxHeight: '200px', overflowY: 'auto' }}>
                  {annotations.map((annotation) => (
                    <Box 
                      key={annotation.id} 
                      sx={{ 
                        p: 1, 
                        mb: 1, 
                        borderRadius: 1,
                        border: selectedAnnotationId === annotation.id ? `2px solid ${annotation.color}` : '1px solid #e0e0e0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        cursor: 'pointer',
                        '&:hover': {
                          bgcolor: 'rgba(0,0,0,0.03)'
                        }
                      }}
                      onClick={() => setSelectedAnnotationId(annotation.id === selectedAnnotationId ? null : annotation.id)}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <Box 
                          sx={{ 
                            width: 12, 
                            height: 12, 
                            bgcolor: annotation.color,
                            borderRadius: '50%',
                            mr: 1 
                          }} 
                        />
                        <Typography variant="body2">
                          {annotation.label || annotation.type}
                        </Typography>
                      </Box>
                      <IconButton 
                        size="small" 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteAnnotation(annotation.id);
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  ))}
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  No annotations yet. Start drawing on the image.
                </Typography>
              )}
            </Stack>
          </Box>
        </Stack>
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button 
          onClick={handleSaveAnnotations}
          variant="contained" 
          color="primary"
        >
          Save Annotations
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ImageAnnotationDialog;
