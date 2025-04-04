'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { projectsService } from '@/services/projects.service';

export default function NewProjectPage() {
    const router = useRouter();
    const [formData, setFormData] = useState({
        projectName: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (isSubmitting) return; // Prevent multiple submissions
        
        setLoading(true);
        setError('');
        setIsSubmitting(true);

        try {
            await projectsService.createProject(formData);
            router.push('/projects');
        } catch (err: any) {
            console.error('Project creation error:', err);
            if (err.message.includes('rate limited')) {
                setError('Too many requests. Please wait a moment before trying again.');
            } else {
                setError(err.message || 'Failed to create project. Please try again.');
            }
        } finally {
            setLoading(false);
            // Add a delay before allowing another submission
            setTimeout(() => {
                setIsSubmitting(false);
            }, 2000); // 2 second cooldown
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
                <div className="text-center mb-12">
                    <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
                        <span className="block">Create New Project</span>
                    </h1>
                    <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
                        Start a new satellite image processing project
                    </p>
                </div>

                <div className="max-w-2xl mx-auto">
                    <form onSubmit={handleSubmit} className="space-y-8 bg-white shadow-lg rounded-lg p-8">
                        {error && (
                            <div className="rounded-md bg-red-50 p-4">
                                <div className="flex">
                                    <div className="ml-3">
                                        <h3 className="text-sm font-medium text-red-800">
                                            {error}
                                        </h3>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div>
                            <label htmlFor="projectName" className="block text-sm font-medium text-gray-700">
                                Project Name
                            </label>
                            <div className="mt-1">
                                <input
                                    type="text"
                                    name="projectName"
                                    id="projectName"
                                    required
                                    disabled={loading || isSubmitting}
                                    value={formData.projectName}
                                    onChange={(e) => setFormData({ ...formData, projectName: e.target.value })}
                                    className="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md disabled:bg-gray-100 disabled:cursor-not-allowed"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end space-x-4">
                            <button
                                type="button"
                                onClick={() => router.back()}
                                disabled={loading || isSubmitting}
                                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={loading || isSubmitting}
                                className="inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-indigo-600 border border-transparent rounded-md shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-400 disabled:cursor-not-allowed"
                            >
                                {loading ? 'Creating...' : isSubmitting ? 'Please wait...' : 'Create Project'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
