'use client'

import { useState, useEffect } from 'react'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import {
    ChartBarIcon,
    FolderIcon,
    ClockIcon,
    CheckCircleIcon,
    ExclamationCircleIcon
} from '@heroicons/react/24/outline'
import { dashboardService } from '@/services/dashboard.service'
import { Alert, CircularProgress } from '@mui/material'

interface DashboardData {
    totalProjects: number;
    activeAnalyses: number;
    storageUsed: string;
    mapCoverage: string;
    recentProjects: Array<{
        id: number;
        name: string;
        description: string;
        lastModified: string;
    }>;
    recentAnalyses: Array<{
        id: number;
        name: string;
        description: string;
        lastModified: string;
    }>;
}

export default function Dashboard() {
    const [dashboardData, setDashboardData] = useState<DashboardData>({
        totalProjects: 0,
        activeAnalyses: 0,
        storageUsed: '0 GB',
        mapCoverage: '0%',
        recentProjects: [],
        recentAnalyses: []
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [retryCount, setRetryCount] = useState(0);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const data = await dashboardService.getDashboardData();
                setDashboardData(data);
                setError('');
                setRetryCount(0);
            } catch (error: any) {
                console.error('Dashboard error:', error);
                setError(error.message || 'Failed to fetch dashboard data');
                
                // Implement exponential backoff for retries
                if (retryCount < 3) {
                    const timeout = setTimeout(() => {
                        setRetryCount(prev => prev + 1);
                    }, Math.pow(2, retryCount) * 1000);
                    return () => clearTimeout(timeout);
                }
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [retryCount]); // Retry when retryCount changes

    const stats = [
        { 
            name: 'Total Projects', 
            stat: dashboardData.totalProjects, 
            icon: FolderIcon,
            color: 'text-blue-600'
        },
        { 
            name: 'Active Projects', 
            stat: dashboardData.activeAnalyses, 
            icon: ChartBarIcon,
            color: 'text-green-600'
        },
        { 
            name: 'Recent Activities', 
            stat: dashboardData.recentProjects.length, 
            icon: ClockIcon,
            color: 'text-purple-600'
        }
    ];

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <CircularProgress />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Navigation />

            <main className="py-6">
                <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center">
                        <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>
                        {error && (
                            <Alert 
                                severity="error" 
                                action={
                                    <button
                                        onClick={() => setRetryCount(prev => prev + 1)}
                                        className="bg-red-50 text-red-600 px-3 py-1 rounded-md text-sm font-medium hover:bg-red-100"
                                    >
                                        Retry
                                    </button>
                                }
                            >
                                {error}
                            </Alert>
                        )}
                    </div>

                    {/* Stats */}
                    <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
                        {stats.map((item) => (
                            <div
                                key={item.name}
                                className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6"
                            >
                                <div className="flex items-center">
                                    <div className="flex-shrink-0">
                                        <item.icon className={`h-6 w-6 ${item.color}`} aria-hidden="true" />
                                    </div>
                                    <div className="ml-5 w-0 flex-1">
                                        <dt className="truncate text-sm font-medium text-gray-500">{item.name}</dt>
                                        <dd className="mt-1 text-3xl font-semibold tracking-tight text-gray-900">
                                            {item.stat}
                                        </dd>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Recent Projects */}
                    <div className="mt-8">
                        <div className="flex items-center justify-between">
                            <h2 className="text-lg font-medium text-gray-900">Recent Projects</h2>
                            <Link
                                href="/projects/new"
                                className="text-sm font-medium text-blue-600 hover:text-blue-500"
                            >
                                Create New Project
                            </Link>
                        </div>
                        <div className="mt-4 overflow-hidden bg-white shadow sm:rounded-md">
                            {dashboardData.recentProjects.length === 0 ? (
                                <div className="p-4 text-center text-gray-500">
                                    No recent projects found
                                </div>
                            ) : (
                                <ul role="list" className="divide-y divide-gray-200">
                                    {dashboardData.recentProjects.map((project) => (
                                        <li key={project.id}>
                                            <Link href={`/projects/${project.id}`} className="block hover:bg-gray-50">
                                                <div className="px-4 py-4 sm:px-6">
                                                    <div className="flex items-center justify-between">
                                                        <div className="truncate">
                                                            <div className="flex items-center">
                                                                {project.name.toLowerCase().includes('active') ? (
                                                                    <CheckCircleIcon className="h-5 w-5 text-green-500 mr-2" />
                                                                ) : (
                                                                    <ExclamationCircleIcon className="h-5 w-5 text-yellow-500 mr-2" />
                                                                )}
                                                                <p className="truncate text-sm font-medium text-blue-600">
                                                                    {project.name}
                                                                </p>
                                                            </div>
                                                            <p className="mt-1 truncate text-sm text-gray-500">
                                                                {project.description}
                                                            </p>
                                                        </div>
                                                        <div className="ml-4 flex flex-shrink-0">
                                                            <p className="text-sm text-gray-500">{project.lastModified}</p>
                                                        </div>
                                                    </div>
                                                </div>
                                            </Link>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    </div>
                </div>
            </main>
        </div>
    )
}
