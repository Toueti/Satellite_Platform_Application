'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { projectsService } from '@/services/projects.service';

export default function NewProjectPage() {
    const router = useRouter();
    const [formData, setFormData] = useState({
        projectName: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [cooldown, setCooldown] = useState(0);
    const [lastSubmitTime, setLastSubmitTime] = useState<number | null>(null);

    // Check for existing cooldown on component mount
    useEffect(() => {
        const lastSubmit = localStorage.getItem('lastProjectSubmit');
        if (lastSubmit) {
            const timeSinceLastSubmit = Date.now() - parseInt(lastSubmit);
            const remainingCooldown = Math.max(0, 5000 - timeSinceLastSubmit);
            if (remainingCooldown > 0) {
                setCooldown(Math.ceil(remainingCooldown / 1000));
            }
        }
    }, []);

    // Cooldown timer
    useEffect(() => {
        let timer: NodeJS.Timeout;
        if (cooldown > 0) {
            timer = setInterval(() => {
                setCooldown(prev => Math.max(0, prev - 1));
            }, 1000);
        }
        return () => {
            if (timer) clearInterval(timer);
        };
    }, [cooldown]);

    const validateProjectName = (name: string) => {
        if (!name.trim()) {
            return 'Project name is required.';
        }
        if (name.length < 3) {
            return 'Project name must be at least 3 characters long.';
        }
        if (name.length > 50) {
            return 'Project name must be less than 50 characters long.';
        }
        // Only allow letters, numbers, spaces, and common punctuation
        if (!/^[a-zA-Z0-9\s\-_.,()]+$/.test(name)) {
            return 'Project name can only contain letters, numbers, spaces, and common punctuation (.-_,()).';
        }
        return null;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        // Check if we're in cooldown
        if (cooldown > 0) {
            setError(`Please wait ${cooldown} seconds before trying again.`);
            return;
        }

        // Validate project name
        const validationError = validateProjectName(formData.projectName);
        if (validationError) {
            setError(validationError);
            return;
        }

        setLoading(true);
        setError('');

        try {
            await projectsService.createProject(formData);
            localStorage.setItem('lastProjectSubmit', Date.now().toString());
            router.push('/projects');
        } catch (err: any) {
            console.error('Project creation error:', err);
            
            // Parse the error message if it's a JSON string
            let errorMessage = err.message;
            try {
                if (typeof err.message === 'string' && err.message.includes('{')) {
                    const errorData = JSON.parse(err.message.substring(err.message.indexOf('{')));
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                }
            } catch (e) {
                // If parsing fails, use the original error message
            }

            if (errorMessage.includes('already exists')) {
                setError('A project with this name already exists. Please choose a different name.');
            } else if (err.message.includes('429') || err.message.includes('rate limited')) {
                setCooldown(5);
                setError('Too many requests. Please wait a moment before trying again.');
                localStorage.setItem('lastProjectSubmit', Date.now().toString());
            } else {
                setError(errorMessage || 'Failed to create project. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newName = e.target.value;
        setFormData({ ...formData, projectName: newName });
        // Clear error when user starts typing
        if (error) {
            setError('');
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
                                    disabled={loading || cooldown > 0}
                                    value={formData.projectName}
                                    onChange={handleNameChange}
                                    placeholder="Enter a unique project name"
                                    className="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md disabled:bg-gray-100 disabled:cursor-not-allowed"
                                />
                                <p className="mt-2 text-sm text-gray-500">
                                    Use a unique name containing letters, numbers, and common punctuation.
                                </p>
                            </div>
                        </div>

                        <div className="flex justify-end space-x-4">
                            <button
                                type="button"
                                onClick={() => router.back()}
                                disabled={loading}
                                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={loading || cooldown > 0 || !formData.projectName.trim()}
                                className="inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-indigo-600 border border-transparent rounded-md shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-400 disabled:cursor-not-allowed"
                            >
                                {loading ? 'Creating...' : 
                                 cooldown > 0 ? `Wait ${cooldown}s` : 
                                 'Create Project'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
