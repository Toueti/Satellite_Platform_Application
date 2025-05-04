import React from 'react';
import Link from 'next/link';
import { Button, Typography } from '@mui/material';
import {
    ArrowLeftIcon,
    PencilIcon,
    ArchiveBoxIcon,
    TrashIcon,
} from '@heroicons/react/24/outline';
import { Project, ProjectStatus } from '@/types/api';

interface ProjectHeaderProps {
    project: Project;
    projectId: string;
    onArchive: () => void;
    onUnarchive: () => void;
    onDelete: () => void;
    actionError?: string | null;
    actionSuccess?: string | null;
    isLoadingAction?: boolean; // To disable buttons during actions
}

const ProjectHeader: React.FC<ProjectHeaderProps> = ({
    project,
    projectId,
    onArchive,
    onUnarchive,
    onDelete,
    actionError,
    actionSuccess,
    isLoadingAction = false, // Default to false
}) => {
    return (
        <div className="mb-8">
            {/* Back Link */}
            <Link
                href="/projects"
                className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 mb-4"
            >
                <ArrowLeftIcon className="h-4 w-4 mr-1" />
                Back to Projects
            </Link>

            {/* Title and Actions */}
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                {/* Title and Description */}
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">{project?.projectName ?? 'Unnamed Project'}</h1>
                    <p className="mt-1 text-sm text-gray-500">{project?.description ?? 'No description provided.'}</p>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-2 flex-wrap">
                    <Link
                        href={`/projects/update/${projectId}`}
                        className={`inline-flex items-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 ${isLoadingAction ? 'opacity-50 cursor-not-allowed' : ''}`}
                        aria-disabled={isLoadingAction}
                        onClick={(e) => { if (isLoadingAction) e.preventDefault(); }} // Prevent navigation if loading
                    >
                        <PencilIcon className="h-4 w-4 mr-1" />
                        Edit
                    </Link>
                    {project.status === ProjectStatus.ARCHIVED ? (
                        <Button
                            onClick={onUnarchive}
                            variant="outlined"
                            size="small"
                            startIcon={<ArchiveBoxIcon className="h-4 w-4" />}
                            disabled={isLoadingAction}
                        >
                            Unarchive
                        </Button>
                    ) : (
                        <Button
                            onClick={onArchive}
                            variant="outlined"
                            size="small"
                            startIcon={<ArchiveBoxIcon className="h-4 w-4" />}
                            disabled={isLoadingAction}
                        >
                            Archive
                        </Button>
                    )}
                    <Button
                        onClick={onDelete}
                        variant="outlined"
                        color="error"
                        size="small"
                        startIcon={<TrashIcon className="h-4 w-4" />}
                        disabled={isLoadingAction}
                    >
                        Delete
                    </Button>
                </div>
            </div>

            {/* Display Action Feedback */}
            {actionError && (
                <Typography color="error" sx={{ mt: 2 }}>{actionError}</Typography>
            )}
            {actionSuccess && (
                <Typography color="success.main" sx={{ mt: 2 }}>{actionSuccess}</Typography>
            )}
        </div>
    );
};

export default ProjectHeader;
