'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Project, projectsService } from '@/services/projects.service';

export default function ProjectsPage() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        loadProjects();
    }, []);

    const loadProjects = async () => {
        try {
            const data = await projectsService.getAllProjects();
            setProjects(data);
        } catch (err) {
            setError('Failed to load projects');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
                <div className="text-center mb-12">
                    <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
                        <span className="block">Your Projects</span>
                    </h1>
                    <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
                        Manage and organize your satellite image processing projects
                    </p>
                    <div className="mt-5 max-w-md mx-auto sm:flex sm:justify-center md:mt-8">
                        <div className="rounded-md shadow">
                            <Link
                                href="/projects/new"
                                className="w-full flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 md:py-4 md:text-lg md:px-10"
                            >
                                Create New Project
                            </Link>
                        </div>
                    </div>
                </div>

                {error && (
                    <div className="max-w-3xl mx-auto mb-8">
                        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
                            {error}
                        </div>
                    </div>
                )}

                <div className="mt-12 max-w-lg mx-auto grid gap-8 lg:grid-cols-3 lg:max-w-none">
                    {projects.map((project) => (
                        <div
                            key={project.id}
                            className="flex flex-col rounded-lg shadow-lg overflow-hidden bg-white hover:shadow-xl transition-shadow duration-300"
                        >
                            <div className="flex-1 p-6 flex flex-col justify-between">
                                <div className="flex-1">
                                    <div className="flex items-center justify-between">
                                        <h3 className="text-xl font-semibold text-gray-900">
                                            {project.name}
                                        </h3>
                                        <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                                            project.status === 'active' ? 'bg-green-100 text-green-800' :
                                            project.status === 'completed' ? 'bg-blue-100 text-blue-800' :
                                            'bg-gray-100 text-gray-800'
                                        }`}>
                                            {project.status}
                                        </span>
                                    </div>
                                    <p className="mt-3 text-base text-gray-500">
                                        {project.description}
                                    </p>
                                </div>
                                <div className="mt-6">
                                    <div className="text-sm text-gray-500">
                                        Created on {new Date(project.createdAt).toLocaleDateString()}
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {projects.length === 0 && !error && (
                    <div className="text-center py-12">
                        <h3 className="text-xl font-medium text-gray-900 mb-2">No projects yet</h3>
                        <p className="text-gray-500 mb-6">
                            Get started by creating your first project
                        </p>
                        <Link
                            href="/projects/new"
                            className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700"
                        >
                            Create Project
                        </Link>
                    </div>
                )}
            </div>
        </div>
    );
}
