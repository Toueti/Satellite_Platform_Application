'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Project } from '@/types/api'; // Keep Project from api.ts
import { User } from '@/types/user'; // Import User from user.ts
import { CreateProjectData } from '@/types/project';
import { projectsService } from '@/services/projects.service';
import { adminService } from '@/services/admin.service';

export default function ProjectManagement() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false); // State for create modal
  const [newProjectData, setNewProjectData] = useState<CreateProjectData>({ // State for new project form
    name: '',
    description: '',
  });
  const [showAssignModal, setShowAssignModal] = useState(false); // State for assign modal
  const [assigningProject, setAssigningProject] = useState<Project | null>(null); // Project being assigned to
  const [availableUsers, setAvailableUsers] = useState<User[]>([]); // All users for selection
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]); // IDs of users to assign
  const [usersLoading, setUsersLoading] = useState(false); // Loading state for users
  const [usersError, setUsersError] = useState<string | null>(null); // Error state for users

  // Define fetchProjects using useCallback
  const fetchProjects = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Use the correct service name here too
      const paginatedResponse = await projectsService.getAllProjects();
      // Assuming getAllProjects returns PaginatedResponse<Project>
      setProjects(paginatedResponse.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  }, []); // Empty dependency array: function is created once

  // Effect to fetch projects on mount
  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  // Effect to fetch users when assign modal opens
  useEffect(() => {
    const fetchUsersForAssignment = async () => {
      if (showAssignModal) {
        setUsersLoading(true);
        setUsersError(null);
        try {
          // Fetch all users - adjust if only specific roles should be assignable
          const users = await adminService.getAllUsers();
          setAvailableUsers(users);
          // Pre-select currently assigned users if that info is available on the project object
          // setSelectedUserIds(assigningProject?.collaborators || []); // Assuming 'collaborators' holds user IDs
        } catch (err) {
          setUsersError(err instanceof Error ? err.message : 'Failed to fetch users');
        } finally {
          setUsersLoading(false);
        }
      }
    };
    fetchUsersForAssignment();
  }, [showAssignModal, assigningProject]); // Re-run if modal opens or project changes

  if (loading) return <div>Loading projects...</div>;
  if (error) return <div className="text-red-500">Error: {error}</div>;

  return (
    <div className="container mx-auto px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Project Management</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        >
          Create Project
        </button>
      </div>

      {/* Project Table */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {projects.length === 0 && !loading && (
              <tr>
                <td colSpan={4} className="px-6 py-4 text-center text-gray-500">No projects found.</td>
              </tr>
            )}
            {/* Add index to map for fallback key */}
            {projects.map((project, index) => (
              // Use fallback key if id is missing
              <tr key={project.id ?? project._id ?? index}>
                {/* Handle potentially missing name, fallback to projectName or N/A */}
                <td className="px-6 py-4 whitespace-nowrap">{project.name ?? project.projectName ?? 'N/A'}</td>
                {/* Handle potentially missing description */}
                <td className="px-6 py-4 whitespace-normal max-w-xs truncate">{project.description ?? 'No description'}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                   {/* Handle potentially missing status */}
                   <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                       project.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                       project.status === 'IN_PROGRESS' ? 'bg-yellow-100 text-yellow-800' :
                       project.status === 'ARCHIVED' ? 'bg-gray-100 text-gray-800' :
                       project.status === 'CREATED' ? 'bg-purple-100 text-purple-800' : // Added CREATED status style
                       'bg-blue-100 text-blue-800' // Default for PENDING or others
                   }`}>
                       {project.status ?? 'UNKNOWN'}
                   </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  {/* Edit button - requires updateProject service method (PM5) */}
                  {/*
                  <button
                    // onClick={() => handleOpenEditModal(project)}
                    className="text-yellow-600 hover:text-yellow-900 mr-4"
                    disabled // Disable until update service method exists
                  >
                    Edit
                  </button>
                  */}
                  {/* Assign Users button (PM7) */}
                  <button
                    onClick={() => handleOpenAssignModal(project)}
                    className="text-indigo-600 hover:text-indigo-900 mr-4"
                  >
                    Assign
                  </button>
                  {/* Delete button (PM6) */}
                  <button
                    onClick={() => handleDeleteProject(project.id ?? project._id)}
                    className="text-red-600 hover:text-red-900"
                    disabled={!project.id && !project._id}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Create Project Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96">
            <h2 className="text-xl font-bold mb-4">Create New Project</h2>
            {/* Display creation-specific errors */}
            {error && <p className="text-red-500 mb-2">Error: {error}</p>}
            <form onSubmit={handleCreateProject}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Project Name</label>
                <input
                  type="text"
                  value={newProjectData.name}
                  onChange={(e) => setNewProjectData({ ...newProjectData, name: e.target.value })}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  required
                  autoFocus
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Description</label>
                <textarea
                  value={newProjectData.description}
                  onChange={(e) => setNewProjectData({ ...newProjectData, description: e.target.value })}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  rows={3}
                />
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateModal(false);
                    setNewProjectData({ name: '', description: '' }); // Reset form
                    setError(null); // Clear errors
                  }}
                  className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Assign Users Modal */}
      {showAssignModal && assigningProject && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">Assign Users to {assigningProject.name ?? assigningProject.projectName}</h2>
            {/* Display assignment-specific errors */}
            {usersError && <p className="text-red-500 mb-2">Error loading users: {usersError}</p>}
            {error && <p className="text-red-500 mb-2">Error assigning users: {error}</p>}

            <form onSubmit={handleAssignUsers}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Select Users</label>
                {usersLoading ? (
                  <p>Loading users...</p>
                ) : (
                  <select
                    multiple
                    value={selectedUserIds}
                    onChange={(e) => setSelectedUserIds(Array.from(e.target.selectedOptions, option => option.value))}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 h-40" // Increased height
                    required
                  >
                    {/* Map over availableUsers (type from user.ts) */}
                    {availableUsers.map(user => (
                      <option key={user.id} value={user.id}>
                        {/* Display username or email from user.ts type */}
                        {user.username} ({user.email})
                      </option>
                    ))}
                  </select>
                )}
                 <p className="text-xs text-gray-500 mt-1">Hold Ctrl/Cmd to select multiple users.</p>
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowAssignModal(false);
                    setAssigningProject(null);
                    setSelectedUserIds([]); // Reset selection
                    setError(null); // Clear errors
                    setUsersError(null); // Clear user loading errors
                  }}
                  className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
                  disabled={usersLoading} // Disable while users are loading
                >
                  Assign Selected
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );

  // Function to open the assign modal
  function handleOpenAssignModal(project: Project) {
    setAssigningProject(project);
    // Pre-populate selected users if available on project object
    // setSelectedUserIds(project.collaborators || []); // Example: if project.collaborators exists
    setShowAssignModal(true);
  }

  // Handler function for creating a project
  async function handleCreateProject(e: React.FormEvent) {
    e.preventDefault();
    if (!newProjectData.name.trim()) {
      setError("Project name cannot be empty.");
      return;
    }
    setError(null); // Clear previous errors
    try {
      // Adapt the data structure to match projectsService.createProject expectation
      const dataToSend = {
        projectName: newProjectData.name.trim(),
        description: newProjectData.description.trim() || undefined, // Send undefined if empty
        // Add tags or status if needed by the backend function
      };
      await projectsService.createProject(dataToSend);
      setShowCreateModal(false); // Close modal on success
      setNewProjectData({ name: '', description: '' }); // Reset form
      await fetchProjects(); // Refresh the projects list
      alert(`Project "${dataToSend.projectName}" created successfully.`); // Optional success message
    } catch (err) {
      console.error("Create project error:", err); // Log the full error
      let errorMessage = 'Failed to create project.';
      if (err instanceof Error) {
        errorMessage = err.message || errorMessage;
      }
      setError(errorMessage);
      // Keep modal open on error
    }
  }

  // Handler function for assigning users
  async function handleAssignUsers(e: React.FormEvent) {
    e.preventDefault();
    if (!assigningProject || (!assigningProject.id && !assigningProject._id)) {
      setError("Cannot assign users: Missing project ID.");
      return;
    }
    if (selectedUserIds.length === 0) {
      setError("Please select at least one user to assign.");
      return;
    }

    const projectId = assigningProject.id ?? assigningProject._id;
    setError(null); // Clear previous errors

    console.log(`Assigning users [${selectedUserIds.join(', ')}] to project ${projectId}`);
    alert(`TODO: Implement backend call to assign users [${selectedUserIds.join(', ')}] to project ${projectId}`);

    // --- TODO: Replace with actual service call when available ---
    // try {
    //   // Example: await projectsService.assignUsersToProject(projectId, selectedUserIds);
    //   setShowAssignModal(false);
    //   setAssigningProject(null);
    //   setSelectedUserIds([]);
    //   await fetchProjects(); // Optionally refresh project data if assignment changes are reflected
    //   alert('Users assigned successfully.');
    // } catch (err) {
    //   console.error("Assign users error:", err);
    //   let errorMessage = 'Failed to assign users.';
    //   if (err instanceof Error) {
    //     errorMessage = err.message || errorMessage;
    //   }
    //   setError(errorMessage);
    //   // Keep modal open on error
    // }
    // --- End TODO ---

     // Close modal for now as service call is stubbed
     setShowAssignModal(false);
     setAssigningProject(null);
     setSelectedUserIds([]);
  }


  // Handler function for deleting a project
  async function handleDeleteProject(projectId: string | undefined) {
    if (!projectId) {
      setError("Cannot delete project: Missing ID.");
      return;
    }
    if (!confirm(`Are you sure you want to delete this project? This action cannot be undone.`)) {
      return;
    }
    setError(null); // Clear previous errors
    try {
      await projectsService.deleteProject(projectId);
      await fetchProjects(); // Refresh the projects list
      alert(`Project deleted successfully.`); // Optional success message
    } catch (err) {
      console.error("Delete project error:", err); // Log the full error
      let errorMessage = 'Failed to delete project.';
       if (err instanceof Error) {
          errorMessage = err.message || errorMessage;
      }
      setError(errorMessage);
    }
  }
}
