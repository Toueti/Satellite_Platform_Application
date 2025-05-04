'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { projectsService } from '@/services/projects.service';

export default function NewProjectPage() {
    const router = useRouter();
    const [newProject, setNewProject] = useState({
        projectName: '',
        description: '',
        tags: ['initial'] // Default to match backend example
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleCreateProject = async () => {
        if (!newProject.projectName.trim()) {
            setError('Project name is required');
            return;
        }
        setLoading(true);
        setError('');

        try {
            await projectsService.createProject({
                projectName: newProject.projectName,
                description: newProject.description,
                tags: newProject.tags,
                status: 'CREATED' // Match backend expectation
            });
            router.push('/projects'); // Redirect to projects list
        } catch (err: any) {
            setError(err.message || 'Failed to create project');
            console.error('Project creation error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-2xl font-bold mb-6">Create New Project</h1>

            <div className="bg-white p-6 rounded-lg shadow-md max-w-2xl mx-auto">
                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                        {error}
                    </div>
                )}
                <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700">Project Name</label>
                    <input
                        type="text"
                        value={newProject.projectName}
                        onChange={(e) => setNewProject({ ...newProject, projectName: e.target.value })}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-200"
                        placeholder="Enter project name"
                        disabled={loading}
                    />
                </div>
                <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700">Description</label>
                    <textarea
                        value={newProject.description}
                        onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-200"
                        rows={4}
                        placeholder="Enter project description (optional)"
                        disabled={loading}
                    />
                </div>
                <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700">Tags</label>
                    <input
                        type="text"
                        value={newProject.tags.join(', ')}
                        onChange={(e) =>
                            setNewProject({
                                ...newProject,
                                tags: e.target.value
                                    .split(',')
                                    .map((tag) => tag.trim())
                                    .filter((tag) => tag)
                            })
                        }
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-200"
                        placeholder="Enter tags, comma-separated (e.g., initial, data)"
                        disabled={loading}
                    />
                </div>
                <div className="flex justify-end space-x-4">
                    <button
                        onClick={() => router.push('/projects')}
                        className="px-4 py-2 rounded text-gray-700 bg-gray-200 hover:bg-gray-300"
                        disabled={loading}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleCreateProject}
                        disabled={loading}
                        className={`px-4 py-2 rounded text-white ${
                            loading ? 'bg-gray-400 cursor-not-allowed' : 'bg-blue-500 hover:bg-blue-600'
                        }`}
                    >
                        {loading ? 'Creating...' : 'Create Project'}
                    </button>
                </div>
            </div>
        </div>
    );
}