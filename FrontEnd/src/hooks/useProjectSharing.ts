import { useState } from 'react';
import { projectsService, ProjectSharingRequest } from '@/services/projects.service';
import { Project } from '@/types/api'; // Import Project type

// Define the type for the setProject function more explicitly
type SetProjectFunction = React.Dispatch<React.SetStateAction<Project | null>>;

export function useProjectSharing(projectId: string | undefined | null, setProject: SetProjectFunction) {
    const [sharingEmail, setSharingEmail] = useState('');
    const [sharingError, setSharingError] = useState<string | null>(null);
    const [sharingSuccess, setSharingSuccess] = useState<string | null>(null);
    const [isSharing, setIsSharing] = useState(false); // Add loading state

    const handleShareProject = async () => {
        setSharingError(null);
        setSharingSuccess(null);
        if (!projectId) {
            setSharingError('Project ID is missing.');
            return;
        }

        if (!sharingEmail || !/\S+@\S+\.\S+/.test(sharingEmail)) {
            setSharingError('Please enter a valid email address.');
            return;
        }

        setIsSharing(true);
        const request: ProjectSharingRequest = {
            projectId: projectId,
            otherEmail: sharingEmail,
        };

        try {
            await projectsService.shareProject(request);
            setSharingSuccess('Project shared successfully!');
            setSharingEmail(''); // Clear input on success

            // Update project data to reflect new collaborator
            // Use functional update to ensure we have the latest project state
            setProject(currentProject => {
                if (!currentProject) return null;
                // Avoid adding duplicate emails if backend handles it, but safe check here
                const newCollaborators = currentProject.collaborators ? [...currentProject.collaborators] : [];
                if (!newCollaborators.includes(sharingEmail)) {
                    newCollaborators.push(sharingEmail);
                }
                return {
                    ...currentProject,
                    collaborators: newCollaborators,
                };
            });

        } catch (error: any) {
            console.error('Error sharing project:', error);
            setSharingError(error.message || 'Failed to share project.');
        } finally {
            setIsSharing(false);
        }
    };

    const handleUnshareProject = async (emailToUnshare: string) => {
        setSharingError(null);
        setSharingSuccess(null);

        if (!projectId) {
            setSharingError('Invalid project ID.');
            return;
        }
        if (!window.confirm(`Are you sure you want to remove ${emailToUnshare} as a collaborator?`)) {
            return;
        }

        setIsSharing(true); // Use the same loading state
        const request: ProjectSharingRequest = {
            projectId: projectId,
            otherEmail: emailToUnshare,
        };

        try {
            await projectsService.unshareProject(request);
            setSharingSuccess(`Project unshared with ${emailToUnshare}.`);

            // Update project data to remove collaborator
            setProject(currentProject => {
                if (!currentProject || !currentProject.collaborators) return currentProject;
                return {
                    ...currentProject,
                    collaborators: currentProject.collaborators.filter(email => email !== emailToUnshare),
                };
            });
        } catch (error: any) {
            console.error('Error unsharing project:', error);
            setSharingError(error.message || 'Failed to unshare project.');
        } finally {
            setIsSharing(false);
        }
    };

    return {
        sharingEmail,
        setSharingEmail,
        sharingError,
        sharingSuccess,
        isSharing, // Expose loading state
        handleShareProject,
        handleUnshareProject,
    };
}
