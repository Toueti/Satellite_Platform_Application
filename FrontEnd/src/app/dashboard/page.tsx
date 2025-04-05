'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import {
    ChartBarIcon,
    FolderIcon,
    ClockIcon,
    CheckCircleIcon,
    ExclamationCircleIcon,
    PhotoIcon,
    ArchiveBoxIcon,
    ShareIcon,
    UserGroupIcon,
    ArrowPathIcon
} from '@heroicons/react/24/outline'
import { dashboardService, DashboardData } from '@/services/dashboard.service'
import { projectsService } from '@/services/projects.service'
import { CircularProgress, Alert } from '@mui/material'
import { Project } from '@/types/api'

export default function Dashboard() {
    const router = useRouter()
    const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const data = await dashboardService.getDashboardData();
                setDashboardData(data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to fetch dashboard data');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
                    {error}
                </div>
            </div>
        );
    }

    if (!dashboardData) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded-md">
                    No dashboard data available
                </div>
            </div>
        );
    }

    const stats = [
        {
            name: 'Total Projects',
            value: dashboardData.totalProjects,
            icon: FolderIcon,
            color: 'bg-blue-500',
        },
        {
            name: 'Active Projects',
            value: dashboardData.activeProjects,
            icon: ArrowPathIcon,
            color: 'bg-green-500',
        },
        {
            name: 'Completed Projects',
            value: dashboardData.completedProjects,
            icon: CheckCircleIcon,
            color: 'bg-purple-500',
        },
        {
            name: 'Archived Projects',
            value: dashboardData.archivedProjects,
            icon: ArchiveBoxIcon,
            color: 'bg-gray-500',
        },
        {
            name: 'Total Images',
            value: dashboardData.totalImages,
            icon: PhotoIcon,
            color: 'bg-yellow-500',
        },
        {
            name: 'Shared Projects',
            value: dashboardData.sharedProjectsCount,
            icon: UserGroupIcon,
            color: 'bg-indigo-500',
        },
    ];

    const handleProjectClick = (projectId: string) => {
        // Debug log
        console.log('Attempting to navigate to project with ID:', projectId);
        
        // Only navigate if we have a valid ID
        if (!projectId || projectId === '[object Object]' || projectId.includes('temp-')) {
            console.error('Invalid project ID detected:', projectId);
            return;
        }
        
        router.push(`/projects/${projectId}`);
    };

    const ProjectCard = ({ project }: { project: Project }) => {
        if (!project) {
            console.error('ProjectCard received null project');
            return null;
        }

        // Extract ID - prefer MongoDB ObjectId for navigation
        let projectId: string;
        
        try {
            // Debug log the project structure
            console.log('Project data:', project);
            
            // First try to get MongoDB ObjectId
            if (project._id) {
                projectId = project._id;
            } else if (project.projectId && typeof project.projectId === 'object' && project.projectId._id) {
                projectId = project.projectId._id;
            } else if (project.id && /^[0-9a-fA-F]{24}$/.test(project.id)) {
                projectId = project.id;
            } else {
                throw new Error('No valid MongoDB ObjectId found');
            }

            // Validate MongoDB ObjectId format
            if (!projectId || projectId.length !== 24 || !/^[0-9a-fA-F]{24}$/.test(projectId)) {
                throw new Error('Invalid MongoDB ObjectId format');
            }

            console.log('Using MongoDB ObjectId for navigation:', projectId);

        } catch (error) {
            console.error('Failed to extract valid MongoDB ObjectId:', error);
            return null;
        }

        // Get the project name from either projectName (backend) or name (frontend)
        const displayName = (project as any).projectName || project.name || 'Unnamed Project';
        
        return (
            <div 
                onClick={() => handleProjectClick(projectId)}
                className="cursor-pointer block bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition-shadow"
            >
                <h3 className="font-medium text-gray-900">{displayName}</h3>
                <p className="text-sm text-gray-500 mt-1">{project.description || 'No description'}</p>
                <div className="flex items-center mt-2 text-sm text-gray-500">
                    <ClockIcon className="h-4 w-4 mr-1" />
                    <span>Updated {new Date(project.updatedAt || Date.now()).toLocaleDateString()}</span>
                </div>
            </div>
        );
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white p-6">
            <div className="max-w-7xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
                    <Link
                        href="/projects/new"
                        className="bg-primary-600 text-white px-4 py-2 rounded-md hover:bg-primary-700 transition-colors"
                    >
                        New Project
                    </Link>
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                    {stats.map((stat) => (
                        <div
                            key={stat.name}
                            className="bg-white rounded-lg shadow-sm p-6 flex items-center"
                        >
                            <div className={`${stat.color} p-3 rounded-lg`}>
                                <stat.icon className="h-6 w-6 text-white" />
                            </div>
                            <div className="ml-4">
                                <p className="text-sm font-medium text-gray-600">{stat.name}</p>
                                <p className="text-2xl font-semibold text-gray-900">
                                    {stat.value}
                                </p>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Recent Projects */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">
                        Recent Projects
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {dashboardData.recentProjects.map((project, index) => {
                            // Ensure we have a valid project object
                            if (!project) return null;
                            
                            // Create a guaranteed unique key
                            const key = typeof project.id === 'object'
                                ? `recent-${index}-${Date.now()}`
                                : String(project.id || `recent-project-${index}`);
                            
                            return (
                                <div key={key}>
                                    <ProjectCard project={project} />
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* Last Accessed Projects */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">
                        Last Accessed Projects
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {dashboardData.lastAccessedProjects.map((project, index) => {
                            // Ensure we have a valid project object
                            if (!project) return null;
                            
                            // Create a guaranteed unique key
                            const key = typeof project.id === 'object'
                                ? `last-accessed-${index}-${Date.now()}`
                                : String(project.id || `last-accessed-${index}`);
                            
                            return (
                                <div key={key}>
                                    <ProjectCard project={project} />
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    )
}
