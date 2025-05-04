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
    ArrowPathIcon,
    BeakerIcon,
    ServerIcon,
    ChartBarSquareIcon
} from '@heroicons/react/24/outline'
import { dashboardService, DashboardData } from '@/services/dashboard.service'
import { CircularProgress, Alert } from '@mui/material'

export default function Dashboard() {
    const router = useRouter()
    const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const data = await dashboardService.getDashboardData();
                console.log('Dashboard data received:', data); // Debug log
                setDashboardData(data);
            } catch (err) {
                console.error('Error fetching dashboard data:', err); // Debug log
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
            value: dashboardData.totalProjects ?? 0,
            icon: FolderIcon,
            color: 'bg-blue-500',
        },
        {
            name: 'Total Images',
            value: dashboardData.totalImages ?? 0,
            icon: PhotoIcon,
            color: 'bg-yellow-500',
        },
        {
            name: 'Total Treatments',
            value: dashboardData.totalTreatments ?? 0,
            icon: BeakerIcon,
            color: 'bg-green-500',
        },
        {
            name: 'Storage Used',
            value: dashboardData.storageUsed || `${((dashboardData.totalStorageUsedBytes ?? 0) / 1024 / 1024).toFixed(2)} MB`,
            icon: ServerIcon,
            color: 'bg-purple-500',
        },
        {
            name: 'Shared By Me',
            value: dashboardData.sharedByUserCount ?? 0,
            icon: ShareIcon,
            color: 'bg-indigo-500',
        },
        {
            name: 'Average Images/Project',
            value: typeof dashboardData.averageImagesPerProject === 'number' 
                ? dashboardData.averageImagesPerProject.toFixed(1) 
                : '0.0',
            icon: ChartBarSquareIcon,
            color: 'bg-pink-500',
        },
    ];

    const processingStats = [
        {
            name: 'Pending',
            value: dashboardData.processingStatusSummary?.pendingCount ?? 0,
            icon: ClockIcon,
            color: 'bg-yellow-500',
        },
        {
            name: 'Processing',
            value: dashboardData.processingStatusSummary?.processingCount ?? 0,
            icon: ArrowPathIcon,
            color: 'bg-blue-500',
        },
        {
            name: 'Completed',
            value: dashboardData.processingStatusSummary?.completedCount ?? 0,
            icon: CheckCircleIcon,
            color: 'bg-green-500',
        },
        {
            name: 'Failed',
            value: dashboardData.processingStatusSummary?.failedCount ?? 0,
            icon: ExclamationCircleIcon,
            color: 'bg-red-500',
        },
    ];

    // Ensure we have arrays even if they're undefined in the data
    const recentlyAccessedProjects = dashboardData.recentlyAccessedProjects ?? [];
    const recentImageUploads = dashboardData.recentImageUploads ?? [];

    const activityTrend = {
        current: dashboardData.activityTrend?.last7DaysCount ?? 0,
        previous: dashboardData.activityTrend?.previous7DaysCount ?? 0,
        trend: (dashboardData.activityTrend?.last7DaysCount ?? 0) - (dashboardData.activityTrend?.previous7DaysCount ?? 0)
    };

    const handleProjectClick = (projectId: string) => {
        if (!projectId) {
            console.error('Invalid project ID detected:', projectId);
            return;
        }
        router.push(`/projects/${projectId}`);
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

                {/* Main Stats Grid */}
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

                {/* Activity Trend */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">Activity Trend</h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="bg-gray-50 rounded-lg p-4">
                            <p className="text-sm text-gray-600">Last 7 Days</p>
                            <p className="text-xl font-semibold">{activityTrend.current}</p>
                        </div>
                        <div className="bg-gray-50 rounded-lg p-4">
                            <p className="text-sm text-gray-600">Previous 7 Days</p>
                            <p className="text-xl font-semibold">{activityTrend.previous}</p>
                        </div>
                        <div className="bg-gray-50 rounded-lg p-4">
                            <p className="text-sm text-gray-600">Trend</p>
                            <p className={`text-xl font-semibold ${activityTrend.trend > 0 ? 'text-green-600' : activityTrend.trend < 0 ? 'text-red-600' : 'text-gray-600'}`}>
                                {activityTrend.trend > 0 ? '+' : ''}{activityTrend.trend}
                            </p>
                        </div>
                    </div>
                </div>

                {/* Processing Status */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">
                        Processing Status
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                        {processingStats.map((stat) => (
                            <div
                                key={stat.name}
                                className="bg-gray-50 rounded-lg p-4 flex items-center"
                            >
                                <div className={`${stat.color} p-2 rounded-lg`}>
                                    <stat.icon className="h-5 w-5 text-white" />
                                </div>
                                <div className="ml-3">
                                    <p className="text-sm font-medium text-gray-600">{stat.name}</p>
                                    <p className="text-xl font-semibold text-gray-900">
                                        {stat.value}
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Recently Accessed Projects */}
                {recentlyAccessedProjects.length > 0 && (
                    <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                        <h2 className="text-lg font-semibold text-gray-900 mb-4">
                            Recently Accessed Projects
                        </h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {recentlyAccessedProjects.map((project) => (
                                <div
                                    key={project.projectId}
                                    onClick={() => handleProjectClick(project.projectId)}
                                    className="cursor-pointer block bg-gray-50 rounded-lg p-4 hover:bg-gray-100 transition-colors"
                                >
                                    <h3 className="font-medium text-gray-900">{project.projectName}</h3>
                                    <div className="flex items-center mt-2 text-sm text-gray-500">
                                        <ClockIcon className="h-4 w-4 mr-1" />
                                        <span>Accessed {new Date(project.lastAccessedTime).toLocaleDateString()}</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Recent Image Uploads */}
                {recentImageUploads.length > 0 && (
                    <div className="bg-white rounded-lg shadow-sm p-6">
                        <h2 className="text-lg font-semibold text-gray-900 mb-4">
                            Recent Image Uploads
                        </h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {recentImageUploads.map((image) => (
                                <div
                                    key={image.imageId}
                                    onClick={() => handleProjectClick(image.projectId)}
                                    className="cursor-pointer block bg-gray-50 rounded-lg p-4 hover:bg-gray-100 transition-colors"
                                >
                                    <h3 className="font-medium text-gray-900">{image.imageName}</h3>
                                    <div className="flex items-center mt-2 text-sm text-gray-500">
                                        <ClockIcon className="h-4 w-4 mr-1" />
                                        <span>Uploaded {new Date(image.uploadTime).toLocaleDateString()}</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
