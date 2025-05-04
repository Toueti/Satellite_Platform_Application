import React from 'react';
import {
    Box,
    Typography,
    Paper,
    Chip,
    TextField,
    Stack,
    Divider,
    Button,
    CircularProgress, // Import CircularProgress for loading state
} from '@mui/material';
import { Project } from '@/types/api';

// Define props based on the hooks it will use
interface ProjectDetailsTabPanelProps {
    project: Project;
    projectId: string; // Needed for display
    // From useProjectSharing
    sharingEmail: string;
    setSharingEmail: (email: string) => void;
    sharingError: string | null;
    sharingSuccess: string | null;
    isSharing: boolean;
    handleShareProject: () => void;
    handleUnshareProject: (email: string) => void;
}

const ProjectDetailsTabPanel: React.FC<ProjectDetailsTabPanelProps> = ({
    project,
    projectId,
    sharingEmail,
    setSharingEmail,
    sharingError,
    sharingSuccess,
    isSharing,
    handleShareProject,
    handleUnshareProject,
}) => {
    return (
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
            {/* Project Information Box */}
            <Box sx={{ width: { xs: '100%', md: '50%' } }}>
                <Paper sx={{ p: 3, height: '100%' }}>
                    <Typography variant="h6" gutterBottom>
                        Project Information
                    </Typography>
                    <Divider sx={{ my: 1 }} />
                    <Box sx={{ mt: 2, '& > *': { mb: 1 } }}> {/* Consistent spacing */}
                        <Typography variant="body2"><strong>ID:</strong> {projectId}</Typography>
                        <Typography variant="body2">
                            <strong>Created:</strong> {project.createdAt ? new Date(project.createdAt).toLocaleString() : 'N/A'}
                        </Typography>
                        <Typography variant="body2">
                            <strong>Last Updated:</strong> {project.updatedAt ? new Date(project.updatedAt).toLocaleString() : 'N/A'}
                        </Typography>
                        <Typography variant="body2" component="div"> {/* Changed component to div */}
                            <strong>Status:</strong> <Chip label={project.status || 'N/A'} size="small" />
                        </Typography>
                        <Typography variant="body2">
                            <strong>Owner:</strong> {project.owner || 'N/A'}
                        </Typography>
                        {project.metadata?.location && (
                            <Typography variant="body2">
                                <strong>Location:</strong> Lat: {project.metadata.location.lat?.toFixed(4)}, Lng: {project.metadata.location.lng?.toFixed(4)}
                            </Typography>
                        )}
                        {/* Add other relevant metadata if available */}
                        {/* Example: */}
                        {/* {project.metadata?.area && (
                            <Typography variant="body2">
                                <strong>Area:</strong> {project.metadata.area} sq km
                            </Typography>
                        )} */}
                    </Box>
                </Paper>
            </Box>

            {/* Sharing Box */}
            <Box sx={{ width: { xs: '100%', md: '50%' } }}>
                <Paper sx={{ p: 3, height: '100%' }}>
                    <Typography variant="h6" gutterBottom>
                        Sharing & Collaboration
                    </Typography>
                    <Divider sx={{ my: 1 }} />
                    <Box sx={{ mt: 2 }}>
                        <Typography variant="body2" gutterBottom>
                            Share this project:
                        </Typography>
                        <Stack direction="row" spacing={1} sx={{ mt: 1, alignItems: 'flex-start' }}>
                            <TextField
                                type="email"
                                size="small"
                                value={sharingEmail}
                                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSharingEmail(e.target.value)}
                                placeholder="Enter collaborator email"
                                fullWidth
                                error={!!sharingError}
                                helperText={sharingError} // Display error message below field
                                disabled={isSharing} // Disable input while sharing
                            />
                            <Button
                                onClick={handleShareProject}
                                variant="contained"
                                size="medium" // Match TextField height better
                                disabled={isSharing || !sharingEmail} // Disable if loading or no email
                                sx={{ minWidth: '80px' }} // Prevent button width change
                            >
                                {isSharing ? <CircularProgress size={24} color="inherit" /> : 'Share'}
                            </Button>
                        </Stack>
                        {/* Success message */}
                        {sharingSuccess && (
                            <Typography color="success.main" variant="caption" sx={{ mt: 1, display: 'block' }}>
                                {sharingSuccess}
                            </Typography>
                        )}

                        <Divider sx={{ my: 2 }} />

                        <Typography variant="body2" gutterBottom>
                            Collaborators:
                        </Typography>
                        {project.collaborators && project.collaborators.length > 0 ? (
                            <Stack spacing={1} sx={{ mt: 1 }}>
                                {project.collaborators.map((email) => (
                                    <Paper
                                        key={email}
                                        variant="outlined"
                                        sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 1 }}
                                    >
                                        <Typography variant="body2" sx={{ overflowWrap: 'break-word', wordBreak: 'break-all', mr: 1 }}>
                                            {email}
                                        </Typography>
                                        <Button
                                            size="small"
                                            color="error"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleUnshareProject(email);
                                            }}
                                            disabled={isSharing} // Disable remove button while any sharing action is in progress
                                        >
                                            Remove
                                        </Button>
                                    </Paper>
                                ))}
                            </Stack>
                        ) : (
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                No collaborators yet.
                            </Typography>
                        )}
                    </Box>
                </Paper>
            </Box>
        </Stack>
    );
};

export default ProjectDetailsTabPanel;
