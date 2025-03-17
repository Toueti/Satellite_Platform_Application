'use client'

import { useState, useEffect } from 'react'
import Navigation from '@/components/Navigation'
import Link from 'next/link'
import {
    ChartBarIcon,
    FolderIcon,
    MapIcon,
    PhotoIcon
} from '@heroicons/react/24/outline'
import { dashboardService } from '@/services/dashboard.service'

interface DashboardData {
  totalProjects: number;
  activeAnalyses: number;
  storageUsed: string;
  mapCoverage: string;
  recentProjects: {
      id: number;
      name: string;
      description: string;
      lastModified: string;
  }[];
}

export default function Dashboard() {
    const [dashboardData, setDashboardData] = useState<DashboardData>({
        totalProjects: 0,
        activeAnalyses: 0,
        storageUsed: '',
        mapCoverage: '',
        recentProjects: [], // Initialize as empty array of correct type
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchData = async () => {
            try {
                const data = await dashboardService.getDashboardData();
                setDashboardData(data);
            } catch (error) {
                setError('Failed to fetch dashboard data.');
                console.error(error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);


    const stats = [
        { name: 'Total Projects', stat: dashboardData.totalProjects, icon: FolderIcon },
        { name: 'Active Analyses', stat: dashboardData.activeAnalyses, icon: ChartBarIcon },
        { name: 'Storage Used', stat: dashboardData.storageUsed, icon: PhotoIcon },
        { name: 'Map Coverage', stat: dashboardData.mapCoverage, icon: MapIcon },
    ];

    if (loading) {
        return <div>Loading...</div>;
    }

    if (error) {
        return <div>Error: {error}</div>;
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Navigation />

            <main className="py-6">
                <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                    <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>

                    {/* Stats */}
                    <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
                        {stats.map((item) => (
                            <div
                                key={item.name}
                                className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6"
                            >
                                <div className="flex items-center">
                                    <div className="flex-shrink-0">
                                        <item.icon className="h-6 w-6 text-gray-400" aria-hidden="true" />
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
                                className="text-sm font-medium text-primary-600 hover:text-primary-500"
                            >
                                Create New Project
                            </Link>
                        </div>
                        <div className="mt-4 overflow-hidden bg-white shadow sm:rounded-md">
                            <ul role="list" className="divide-y divide-gray-200">
                                {dashboardData.recentProjects.map((project) => (
                                    <li key={project.id}>
                                        <Link href={`/projects/${project.id}`} className="block hover:bg-gray-50">
                                            <div className="px-4 py-4 sm:px-6">
                                                <div className="flex items-center justify-between">
                                                    <div className="truncate">
                                                        <div className="flex">
                                                            <p className="truncate text-sm font-medium text-primary-600">
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
                        </div>
                    </div>
                </div>
            </main>
        </div>
    )
}
