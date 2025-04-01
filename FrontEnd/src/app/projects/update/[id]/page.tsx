 'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { projectsService, Project } from '@/services/projects.service';

export default function UpdateProjectPage() {
  const router = useRouter();
  const { id } = useParams();

  const [formData, setFormData] = useState<{
    name: string;
    description: string;
    status?: string;
  }>({
    name: '',
    description: '',
    status: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false); // New state for delete loading

  useEffect(() => {
    const fetchProject = async () => {
      // More robust check for id
      if (id && typeof id === 'string' && id.length > 0) {
        setLoading(true);
        try {
          const project: Project = await projectsService.getProject(id);
          setFormData({
            name: project.name,
            description: project.description,
            status: project.status,
          });
        } catch (error) {
          setError('Failed to fetch project data.');
        } finally {
          setLoading(false);
        }
      }
    };

    fetchProject();
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    // More robust check for id
    if (!id || typeof id !== 'string' || id.length === 0) {
      setError('Invalid project ID.');
      setLoading(false);
      return; // Exit early if id is invalid
    }

    try {
      await projectsService.updateProject(id, formData);
      router.push('/projects');
    } catch (err: any) {
      if (err.message === 'You have been rate limited. Please try again later.') {
        setError(err.message);
      } else {
        setError('Failed to update project. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // New function to handle project deletion
  const handleDelete = async () => {
    if (!id || typeof id !== 'string') {
      setError('Invalid project ID.');
      return;
    }

    if (window.confirm('Are you sure you want to delete this project?')) {
      setDeleteLoading(true);
      try {
        await projectsService.deleteProject(id);
        router.push('/projects');
      } catch (error: any) {
        setError(error.message || 'Failed to delete project.');
      } finally {
        setDeleteLoading(false);
      }
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center mb-12">
          <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
            <span className="block">Update Project</span>
          </h1>
          <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
            Modify project details
          </p>
        </div>

        <div className="max-w-2xl mx-auto">
          <form onSubmit={handleSubmit} className="space-y-8 bg-white shadow-lg rounded-lg p-8">
            {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">{error}</div>
            )}

            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                Project Name
              </label>
              <input
                type="text"
                id="name"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-600 focus:ring-primary-600 sm:text-sm"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700">
                Description
              </label>
              <textarea
                id="description"
                rows={4}
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-600 focus:ring-primary-600 sm:text-sm"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>

            {/* Status Update Dropdown */}
            <div>
              <label htmlFor="status" className="block text-sm font-medium text-gray-700">
                Status
              </label>
              <select
                id="status"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-600 focus:ring-primary-600 sm:text-sm"
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              >
                <option value="active">Active</option>
                <option value="completed">Completed</option>
                <option value="archived">Archived</option>
              </select>
            </div>

            <div className="flex justify-end space-x-4">
              <button
                type="button"
                onClick={() => router.back()}
                className="px-6 py-3 text-base font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-600"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className={`px-6 py-3 text-base font-medium text-white bg-primary-600 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-600 ${
                  loading ? 'opacity-50 cursor-not-allowed' : ''
                }`}
              >
                {loading ? 'Updating...' : 'Update Project'}
              </button>
              <button
                type="button"
                onClick={handleDelete}
                className="px-6 py-3 text-base font-medium text-white bg-red-600 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-600"
              >
                {deleteLoading ? 'Deleting...' : 'Delete Project'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
