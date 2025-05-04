
import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { projectsService } from '@/services/projects.service';
import { Project, ProjectStatus } from '@/types/api';

const MAX_RETRIES = 3;
const RETRY_DELAYS = [1000, 2000, 3000];

export function useProjectData(projectId: string | undefined | null) {
    const router = useRouter();
    const [project, setProject] = useState<Project | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [retryCount, setRetryCount] = useState(0);
    const [actionError, setActionError] = useState<string | null>(null);
    const [actionSuccess, setActionSuccess] = useState<string | null>(null);
    const [isLoadingAction, setIsLoadingAction] = useState(false); // Add action loading state

    const fetchProjectData = useCallback(async () => {
        if (!projectId || typeof projectId !== 'string') {
            setError('Invalid project ID');
            setLoading(false);
            return;
        }

        // Keep loading true if retrying, otherwise set it for initial load
        if (retryCount === 0) {
            setLoading(true);
        }
        setError(null); // Clear previous errors on new attempt
        setActionError(null); // Clear action errors
        setActionSuccess(null); // Clear action success messages

        try {
            // console.log(`Fetching project data for ID: ${projectId}, Attempt: ${retryCount + 1}`); // Commented out
            const projectData = await projectsService.getProject(projectId);
            if (projectData) {
                setProject(projectData);
                setRetryCount(0); // Reset retry count on success
                setLoading(false); // Stop loading on success
            } else {
                setError('Project not found');
                setLoading(false); // Stop loading if not found
            }
        } catch (err: any) {
            console.error('Error fetching project data:', err);
            const isRateLimitError = err.message?.includes('429');

            if (isRateLimitError && retryCount < MAX_RETRIES) {
                const delay = RETRY_DELAYS[retryCount];
                console.log(`Rate limited. Retrying in ${delay}ms... (Attempt ${retryCount + 1}/${MAX_RETRIES})`);
                setError(`Rate limit exceeded. Retrying in ${delay / 1000} seconds... (Attempt ${retryCount + 1}/${MAX_RETRIES})`);
                setRetryCount(prev => prev + 1);
                // Use setTimeout with the async function directly
                setTimeout(fetchProjectData, delay);
                // Don't set loading to false here, wait for retry or final failure
            } else {
                // Final failure (non-retryable or max retries reached)
                setError(isRateLimitError ? 'Rate limit exceeded. Please try again later.' :
                    err.message || 'Failed to fetch project data');
                setRetryCount(0); // Reset retry count on final failure
                setLoading(false); // Set loading false only on final failure
            }
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, retryCount]); // Include retryCount in dependencies

    useEffect(() => {
        // Reset retry count when projectId changes
        setRetryCount(0);
        // Initial fetch or refetch when projectId changes
        if (projectId) {
            fetchProjectData();
        } else {
            // Handle case where projectId becomes invalid/null
            setProject(null);
            setLoading(false);
            setError('Project ID is not available.');
        }
    }, [projectId, fetchProjectData]); // fetchProjectData is now stable due to useCallback

    const handleArchiveProject = async () => {
        if (!projectId || typeof projectId !== 'string') {
            setActionError('Invalid project ID.');
            return;
        }
        if (!window.confirm('Are you sure you want to archive this project?')) return;

        setActionError(null);
        setActionSuccess(null);
        setIsLoadingAction(true); // Set loading true
        try {
            await projectsService.archiveProject(projectId);
            setProject((prevProject) => {
                if (!prevProject) return null;
                return {
                    ...prevProject,
                    status: ProjectStatus.ARCHIVED,
                };
            });
            setActionSuccess('Project archived successfully.');
        } catch (error: any) {
            console.error('Error archiving project:', error);
            setActionError(error.message || 'Failed to archive project.');
        } finally {
            setIsLoadingAction(false); // Set loading false
        }
    };

    const handleUnarchiveProject = async () => {
        if (!projectId || typeof projectId !== 'string') {
            setActionError('Invalid project ID.');
            return;
        }
        if (!window.confirm('Are you sure you want to unarchive this project?')) return;

        setActionError(null);
        setActionSuccess(null);
        setIsLoadingAction(true); // Set loading true
        try {
            await projectsService.unarchiveProject(projectId);
            setProject((prevProject) => {
                if (!prevProject) return null;
                return {
                    ...prevProject,
                    status: ProjectStatus.ACTIVE,
                };
            });
            setActionSuccess('Project unarchived successfully.');
        } catch (error: any) {
            console.error('Error unarchiving project:', error);
            setActionError(error.message || 'Failed to unarchive project.');
        } finally {
            setIsLoadingAction(false); // Set loading false
        }
    };

    const handleDeleteProject = async () => {
        if (!projectId || typeof projectId !== 'string') {
            setActionError('Invalid project ID.');
            return;
        };

        if (window.confirm('Are you sure you want to permanently delete this project? This action cannot be undone.')) {
            setActionError(null);
            setActionSuccess(null);
            setIsLoadingAction(true); // Set loading true
            try {
                await projectsService.deleteProject(projectId);
                // No need to set success message as we are navigating away
                router.push('/projects'); // Navigate away after deletion
            } catch (err: any) {
                console.error('Error deleting project:', err);
                setActionError(err.message || 'Failed to delete project');
            } finally {
                setIsLoadingAction(false); // Set loading false
            }
        }
    };

    // Function to manually trigger a refetch
    const refetchProject = () => {
        setRetryCount(0); // Reset retries
        fetchProjectData();
    };

    return {
        project,
        loading,
        error, // Initial loading error
        actionError, // Error from actions like archive/delete
        actionSuccess, // Success message from actions
        isLoadingAction, // Add to return object
        handleArchiveProject,
        handleUnarchiveProject,
        handleDeleteProject,
        setProject, // Expose setProject for sharing hook to update collaborators
        refetchProject // Expose refetch function
    };
}
