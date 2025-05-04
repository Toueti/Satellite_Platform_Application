'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { projectsService } from '@/services/projects.service';
import { Project, ProjectStatus } from '@/types/api';
import {
  FolderIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
} from '@heroicons/react/24/outline';

const statusColors = {
  [ProjectStatus.ACTIVE]: 'bg-green-100 text-green-800',
  [ProjectStatus.COMPLETED]: 'bg-blue-100 text-blue-800',
  [ProjectStatus.ARCHIVED]: 'bg-gray-100 text-gray-800',
  [ProjectStatus.DRAFT]: 'bg-yellow-100 text-yellow-800',
  [ProjectStatus.CREATED]: 'bg-purple-100 text-purple-800', // Added for backend's "CREATED"
} as const;

const ProjectCard = ({ project, onClick }: { project: Project; onClick: () => void }) => {
  return (
    <div
      onClick={onClick}
      className="cursor-pointer bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition-shadow"
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center">
          <FolderIcon className="h-6 w-6 text-gray-400" />
          <h3 className="ml-2 text-lg font-medium text-gray-900">
            {project.projectName || project.name || 'Unnamed Project'}
          </h3>
        </div>
        <span
          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
            statusColors[project.status as ProjectStatus] || 'bg-gray-100 text-gray-800'
          }`}
        >
          {project.status || 'UNKNOWN'}
        </span>
      </div>
      <p className="mt-2 text-sm text-gray-500 line-clamp-2">
        {project.description || 'No description provided'}
      </p>
      {project.metadata?.tags && project.metadata.tags.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {project.metadata.tags.map((tag, index) => (
            <span
              key={`${project.id}-tag-${index}`}
              className="inline-flex items-center rounded-full bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700"
            >
              {tag}
            </span>
          ))}
        </div>
      )}
      <div className="mt-4 flex items-center text-sm text-gray-500">
        <span>
          Updated {new Date(project.updatedAt || Date.now()).toLocaleDateString()}
        </span>
      </div>
    </div>
  );
};

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | 'ALL'>('ALL');
  const [showFilters, setShowFilters] = useState(false);
  const [retryAttempt, setRetryAttempt] = useState(0);
  const maxRetries = 3;
  const retryDelays = [1000, 2000, 3000];
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await projectsService.getAllProjects(page, pageSize);
        if (!data || !data.content) {
          throw new Error('Invalid response format from server');
        }
        // Normalize project data to match Project type
        const normalizedProjects = data.content.map((project) => ({
          ...project,
          id: project.id || project._id || '',
          projectName: project.projectName || project.name || 'Unnamed Project',
          name: project.projectName || project.name, // Fallback for compatibility
          description: project.description || '',
          status: project.status || ProjectStatus.CREATED, // Default to CREATED
          createdAt: project.createdAt || new Date().toISOString(),
          updatedAt: project.updatedAt || new Date().toISOString(),
          owner: project.ownerEmail || project.owner || '',
          collaborators: project.collaborators || [],
          images: project.images || [], // Handle null images
          metadata: {
            tags: project.tags || [], // Use tags from backend if available
            location: project.metadata?.location,
            satelliteData: project.metadata?.satelliteData,
          },
        }));
        setProjects(normalizedProjects);
        setTotalPages(data.page.totalPages || 1);
        setTotalElements(data.page.totalElements || 0);
        setRetryAttempt(0);
      } catch (err) {
        console.error('Error fetching projects:', err);
        const isRateLimitError = err instanceof Error && err.message.includes('429');
        const isAuthError = err instanceof Error && (
          err.message.includes('No authentication token found') ||
          err.message.includes('403') ||
          err.message.includes('401')
        );

        if (isAuthError) {
          setError('Please log in to view your projects');
          router.push('/login');
          return;
        }

        if (isRateLimitError && retryAttempt < maxRetries) {
          const delay = retryDelays[retryAttempt];
          console.log(`Rate limited. Retrying in ${delay}ms... (Attempt ${retryAttempt + 1}/${maxRetries})`);
          setTimeout(() => {
            setRetryAttempt((prev) => prev + 1);
          }, delay);
          setError(`Rate limit exceeded. Retrying in ${delay / 1000} seconds... (Attempt ${retryAttempt + 1}/${maxRetries})`);
        } else {
          setError(
            isRateLimitError
              ? 'Rate limit exceeded. Please wait a moment and try again.'
              : err instanceof Error
              ? err.message
              : 'Failed to fetch projects'
          );
          setRetryAttempt(0);
        }
      } finally {
        if (!error?.includes('Retrying')) {
          setLoading(false);
        }
      }
    };

    fetchProjects();
  }, [retryAttempt, page, pageSize, router, error]);

  const handleProjectClick = (projectId: string | number | undefined) => {
    if (projectId) {
      router.push(`/projects/${projectId}`);
    }
  };

  const handleNewProject = () => {
    router.push('/projects/new');
  };

  useEffect(() => {
    setPage(0);
  }, [searchQuery, statusFilter]);

  const filteredProjects = projects.filter((project) => {
    const matchesSearch =
      (project.projectName || project.name || '')
        .toLowerCase()
        .includes(searchQuery.toLowerCase()) ||
      (project.description || '').toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || project.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex flex-col items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600 mb-4"></div>
        {error && (
          <div className="text-center text-gray-600 mt-4">
            <p>{error}</p>
          </div>
        )}
      </div>
    );
  }

  if (error && !error.includes('Retrying')) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-md">
          <p>{error}</p>
          <button
            onClick={() => setRetryAttempt(0)}
            className="mt-2 bg-red-200 text-red-700 px-4 py-2 rounded hover:bg-red-300 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white p-6">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Projects</h1>
          <button
            onClick={handleNewProject}
            className="bg-primary-600 text-white px-4 py-2 rounded-md hover:bg-primary-700 transition-colors"
          >
            New Project
          </button>
        </div>

        {/* Search and Filters */}
        <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <MagnifyingGlassIcon className="h-5 w-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search projects..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowFilters(!showFilters)}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
              >
                <FunnelIcon className="h-5 w-5 text-gray-500" />
                <span>Filters</span>
              </button>
            </div>
          </div>

          {showFilters && (
            <div className="mt-4 pt-4 border-t border-gray-200">
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setStatusFilter('ALL')}
                  className={`px-3 py-1 rounded-full text-sm font-medium ${
                    statusFilter === 'ALL'
                      ? 'bg-primary-100 text-primary-800'
                      : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                  }`}
                >
                  All
                </button>
                {Object.values(ProjectStatus).map((status) => (
                  <button
                    key={status}
                    onClick={() => setStatusFilter(status)}
                    className={`px-3 py-1 rounded-full text-sm font-medium ${
                      statusFilter === status
                        ? 'bg-primary-100 text-primary-800'
                        : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                    }`}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {loading
            ? Array.from({ length: pageSize }).map((_, index) => (
                <div key={index} className="bg-white rounded-lg shadow-sm p-6 animate-pulse">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center space-x-3">
                      <div className="h-6 w-6 bg-gray-200 rounded" />
                      <div className="h-4 w-32 bg-gray-200 rounded" />
                    </div>
                    <div className="h-4 w-16 bg-gray-200 rounded-full" />
                  </div>
                  <div className="space-y-3">
                    <div className="h-4 w-3/4 bg-gray-200 rounded" />
                    <div className="h-4 w-1/2 bg-gray-200 rounded" />
                  </div>
                </div>
              ))
            : filteredProjects.map((project) => (
                <ProjectCard
                  key={String(project.id)}
                  project={project}
                  onClick={() => handleProjectClick(project.id)}
                />
              ))}
        </div>

        {filteredProjects.length === 0 && (
          <div className="text-center py-12">
            <FolderIcon className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No projects found</h3>
            <p className="mt-1 text-sm text-gray-500">
              {searchQuery || statusFilter !== 'ALL'
                ? 'Try adjusting your search or filter criteria'
                : 'Get started by creating a new project'}
            </p>
          </div>
        )}

        {totalPages > 1 && (
          <div className="mt-6 flex justify-center gap-2">
            <button
              onClick={() => setPage((prev) => Math.max(0, prev - 1))}
              disabled={page === 0 || loading}
              className="px-3 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Previous
            </button>
            <div className="px-3 py-2 text-center">
              {loading ? (
                <span className="animate-pulse">Loading...</span>
              ) : (
                <div>
                  <div className="text-sm text-gray-500">
                    {totalElements} total projects
                  </div>
                  <div>
                    Page {page + 1} of {totalPages}
                  </div>
                </div>
              )}
            </div>
            <button
              onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
              disabled={page >= totalPages - 1 || loading}
              className="px-3 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}