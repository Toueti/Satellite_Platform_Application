import React, { useState, useRef, useCallback } from 'react';
import { Box, Typography, CircularProgress, Paper } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import { Image } from '@/services/images.service';

interface DragDropUploadProps {
  onUpload: (files: File[]) => Promise<void>;
  isUploading: boolean;
  acceptedFileTypes?: string;
  maxFiles?: number;
  maxFileSizeMB?: number;
}

const DragDropUpload: React.FC<DragDropUploadProps> = ({
  onUpload,
  isUploading,
  acceptedFileTypes = 'image/*',
  maxFiles = 10,
  maxFileSizeMB = 10
}) => {
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragEnter = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const validateFiles = (files: FileList): File[] => {
    const validFiles: File[] = [];
    const errors: string[] = [];

    if (files.length > maxFiles) {
      errors.push(`Maximum ${maxFiles} files allowed`);
    }

    Array.from(files).forEach(file => {
      // Check file type
      if (!file.type.match(acceptedFileTypes.replace('*', '.*'))) {
        errors.push(`File type not supported: ${file.name}`);
        return;
      }

      // Check file size (convert MB to bytes)
      if (file.size > maxFileSizeMB * 1024 * 1024) {
        errors.push(`File too large: ${file.name} (max ${maxFileSizeMB}MB)`);
        return;
      }

      validFiles.push(file);
    });

    if (errors.length > 0) {
      setError(errors.join(', '));
      return [];
    }

    return validFiles;
  };

  const handleDrop = useCallback(async (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    setError(null);

    const files = e.dataTransfer.files;
    if (files.length === 0) {
      setError('No files selected. Please select at least one image file.');
      return;
    }

    const validFiles = validateFiles(files);
    if (validFiles.length > 0) {
      try {
        await onUpload(validFiles);
      } catch (error: any) {
        setError(error.message || 'Failed to upload files. Please try again.');
      }
    }
  }, [onUpload, acceptedFileTypes, maxFiles, maxFileSizeMB]);

  const handleFileInputChange = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) {
      setError('No files selected. Please select at least one image file.');
      return;
    }

    const validFiles = validateFiles(files);
    if (validFiles.length > 0) {
      try {
        await onUpload(validFiles);
      } catch (error: any) {
        setError(error.message || 'Failed to upload files. Please try again.');
      }
    }

    // Reset the input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, [onUpload, acceptedFileTypes, maxFiles, maxFileSizeMB]);

  const handleClick = useCallback(() => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  }, []);

  return (
    <Paper
      elevation={isDragging ? 8 : 1}
      sx={{
        p: 4,
        border: '2px dashed',
        borderColor: isDragging ? 'primary.main' : 'grey.300',
        borderRadius: 2,
        backgroundColor: isDragging ? 'rgba(25, 118, 210, 0.04)' : 'background.paper',
        transition: 'all 0.2s ease',
        cursor: 'pointer',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '200px',
        position: 'relative'
      }}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onClick={handleClick}
    >
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileInputChange}
        accept={acceptedFileTypes}
        multiple
        style={{ display: 'none' }}
      />
      
      {isUploading ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <CircularProgress size={40} sx={{ mb: 2 }} />
          <Typography variant="body1">Uploading...</Typography>
        </Box>
      ) : (
        <>
          <CloudUploadIcon sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
          <Typography variant="h6" gutterBottom>
            Drag & Drop Images Here
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            or click to browse files
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
            Supported formats: JPG, PNG, GIF (max {maxFileSizeMB}MB per file)
          </Typography>
        </>
      )}
      
      {error && (
        <Typography 
          variant="body2" 
          color="error" 
          sx={{ 
            mt: 2, 
            p: 1, 
            bgcolor: 'error.light', 
            borderRadius: 1,
            width: '100%',
            textAlign: 'center'
          }}
        >
          {error}
        </Typography>
      )}
    </Paper>
  );
};

export default DragDropUpload; 