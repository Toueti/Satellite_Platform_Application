'use client';

import React, { useState, useEffect, useCallback } from 'react'; // Import useCallback
import { Authority } from '@/types/auth';
import { adminService } from '@/services/admin.service';

export default function RoleManagement() {
  const [roles, setRoles] = useState<Authority[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false); // State for create modal
  const [newRoleName, setNewRoleName] = useState('');

  // Define fetchRoles using useCallback
  const fetchRoles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const fetchedRoles = await adminService.getAllRoles();
      setRoles(fetchedRoles);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch roles');
    } finally {
      setLoading(false);
    }
  }, []); // Empty dependency array: function is created once

  // Effect to fetch roles on mount
  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]); // Depend on fetchRoles

  if (loading) return <div>Loading roles...</div>;
  if (error) return <div className="text-red-500">Error: {error}</div>;

  return (
    <div className="container mx-auto px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Role Management</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        >
          Create Role
        </button>
      </div>

      {/* Role Table */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Role Name</th>
              {/* Add other columns if needed, e.g., ID */}
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {roles.length === 0 && !loading && (
              <tr>
                <td colSpan={2} className="px-6 py-4 text-center text-gray-500">No roles found.</td>
              </tr>
            )}
            {roles.map((role) => (
              <tr key={role.id || role.roleName}>
                <td className="px-6 py-4 whitespace-nowrap">{role.roleName}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    onClick={() => handleDeleteRole(role.roleName)} // Attach delete handler
                    className="text-red-600 hover:text-red-900"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Create Role Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96">
            <h2 className="text-xl font-bold mb-4">Create New Role</h2>
            {/* Display error specific to creation if needed */}
            <form onSubmit={handleCreateRole}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Role Name</label>
                <input
                  type="text"
                  value={newRoleName}
                  onChange={(e) => setNewRoleName(e.target.value.toUpperCase())} // Often roles are uppercase
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  required
                  autoFocus
                  placeholder="E.g., MANAGER"
                />
                 <p className="text-xs text-gray-500 mt-1">Role names are typically uppercase.</p>
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateModal(false);
                    setNewRoleName(''); // Reset input on cancel
                    setError(null); // Clear errors on cancel
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
    </div>
  );

  // Handler function for creating a role (moved inside the component)
  async function handleCreateRole(e: React.FormEvent) {
    e.preventDefault();
    if (!newRoleName.trim()) {
      setError("Role name cannot be empty.");
      return;
    }
    setError(null); // Clear previous errors
    try {
      await adminService.createRole(newRoleName.trim());
      setShowCreateModal(false); // Close modal on success
      setNewRoleName(''); // Reset input
      await fetchRoles(); // Refresh the roles list
      alert(`Role "${newRoleName.trim()}" created successfully.`); // Optional success message
    } catch (err) {
      console.error("Create role error:", err); // Log the full error
      // Attempt to parse backend error message if available
      let errorMessage = 'Failed to create role.';
      if (err instanceof Error) {
          // Check if it's a custom error structure from httpClient or a standard error
          // This depends on how httpClient throws errors
          errorMessage = err.message || errorMessage;
      }
      setError(errorMessage);
      // Keep modal open on error
    }
  }

  // Handler function for deleting a role
  async function handleDeleteRole(roleName: string) {
    if (!confirm(`Are you sure you want to delete the role "${roleName}"? This action cannot be undone.`)) {
      return;
    }
    setError(null); // Clear previous errors
    try {
      await adminService.deleteRole(roleName);
      await fetchRoles(); // Refresh the roles list
      alert(`Role "${roleName}" deleted successfully.`); // Optional success message
    } catch (err) {
      console.error("Delete role error:", err); // Log the full error
      let errorMessage = 'Failed to delete role.';
       if (err instanceof Error) {
          errorMessage = err.message || errorMessage;
      }
      setError(errorMessage);
    }
  }
}
