'use client';

'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { User } from '@/types/user';
import { Authority } from '@/types/auth';
import { AdminSignupRequest } from '@/types/admin'; // Import AdminSignupRequest type
import { adminService } from '@/services/admin.service';

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newUser, setNewUser] = useState({
    username: '',
    email: '',
    password: '',
    roles: [] as string[],
  });
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [resettingUser, setResettingUser] = useState<User | null>(null); // State for user whose password is being reset
  const [showResetPasswordModal, setShowResetPasswordModal] = useState(false); // State for reset password modal
  const [newPassword, setNewPassword] = useState(''); // State for the new password input
  const [availableRoles, setAvailableRoles] = useState<Authority[]>([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [rolesError, setRolesError] = useState<string | null>(null);
  const [pendingRequests, setPendingRequests] = useState<AdminSignupRequest[]>([]); // State for pending requests
  const [requestsLoading, setRequestsLoading] = useState(true); // Loading state for requests
  const [requestsError, setRequestsError] = useState<string | null>(null); // Error state for requests

  // Fetch users and pending requests on initial mount
  useEffect(() => {
    fetchUsers();
    fetchPendingRequests();
  }, []);

  // Function to fetch available roles
  const fetchRoles = useCallback(async () => {
    setRolesLoading(true);
    setRolesError(null);
    try {
      const roles = await adminService.getAllRoles();
      setAvailableRoles(roles);
    } catch (err) {
      setRolesError(err instanceof Error ? err.message : 'Failed to fetch roles');
    } finally {
      setRolesLoading(false);
    }
  }, []); // No dependencies, adminService is stable

  // Fetch roles when the create or edit modal is opened
  useEffect(() => {
    if (showCreateModal || showEditModal) {
      fetchRoles();
    }
  }, [showCreateModal, showEditModal, fetchRoles]);

  // Open reset password modal when resettingUser changes
  useEffect(() => {
    if (resettingUser) {
      setShowResetPasswordModal(true);
    } else {
      setShowResetPasswordModal(false);
    }
  }, [resettingUser]);

  // Pre-fill edit form when editingUser changes
  useEffect(() => {
    if (editingUser) {
      // Pre-fill the newUser state which is used by the modal form
      setNewUser({
        username: editingUser.username,
        email: editingUser.email,
        password: '', // Password is not edited here, handle separately if needed
        roles: editingUser.roles || [],
      });
      setShowEditModal(true);
    } else {
      setShowEditModal(false);
    }
  }, [editingUser]);

  const fetchUsers = async () => {
    setLoading(true); // Ensure loading state is set
    setError(null); // Clear previous errors
    try {
      const fetchedUsers = await adminService.getAllUsers();
      setUsers(fetchedUsers);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  };

  // Function to fetch pending admin signup requests
  const fetchPendingRequests = async () => {
    setRequestsLoading(true);
    setRequestsError(null);
    try {
      const requests = await adminService.getPendingAdminRequests();
      // Assuming AdminSignupRequest has at least an 'id' and relevant details
      setPendingRequests(requests);
    } catch (err) {
      setRequestsError(err instanceof Error ? err.message : 'Failed to fetch pending requests');
    } finally {
      setRequestsLoading(false);
    }
  };

  // Function to open the edit modal
  const handleOpenEditModal = (user: User) => {
    setEditingUser(user);
    // The useEffect above will handle setting newUser state and opening the modal
  };

  // Function to close modals and reset state
  const handleCloseModals = () => {
    setShowCreateModal(false);
    setShowEditModal(false);
    setEditingUser(null);
    setShowResetPasswordModal(false); // Close reset password modal
    setResettingUser(null);
    setNewPassword(''); // Clear new password
    setNewUser({ username: '', email: '', password: '', roles: [] }); // Reset create/edit form
    setRolesError(null); // Clear role errors
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null); // Clear previous errors
    try {
      // The service expects roles as string[], which newUser state already provides
      await adminService.createUser(newUser);
      await fetchUsers(); // Refresh the list
      handleCloseModals(); // Close modal and reset form
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create user');
    }
  };

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    setError(null);
    try {
      // Prepare data for update (excluding password)
      const updateData = {
        username: newUser.username,
        email: newUser.email,
        roles: newUser.roles,
      };
      await adminService.updateUser(editingUser.id, updateData);
      await fetchUsers(); // Refresh the list
      handleCloseModals(); // Close modal and reset form
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update user');
    }
  };

  // Function to open the reset password modal
  const handleOpenResetPasswordModal = (user: User) => {
    setResettingUser(user);
    // The useEffect above will handle opening the modal
  };

  // Function to handle the password reset submission
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resettingUser || !newPassword) return;
    setError(null);
    try {
      await adminService.resetUserPassword(resettingUser.id, newPassword);
      handleCloseModals(); // Close modal on success
      // Optionally show a success message
      alert(`Password for ${resettingUser.username} has been reset.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reset password');
      // Keep the modal open on error to show the message
    }
  };

  const handleDeleteUser = async (userId: string) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    setError(null); // Clear previous errors
    try {
      await adminService.deleteUser(userId);
      await fetchUsers(); // Refresh the list
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete user');
    }
  };

  const handleLockUser = async (userId: string, lock: boolean) => {
    setError(null); // Clear previous errors
    try {
      await adminService.lockUnlockUser(userId, lock);
      await fetchUsers(); // Refresh the list
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${lock ? 'lock' : 'unlock'} user`);
    }
  };

  // Function to handle approving a signup request
  const handleApproveRequest = async (requestId: string) => {
    // Optional: Add confirmation dialog
    setRequestsError(null); // Clear previous request errors
    try {
      await adminService.approveAdminRequest(requestId);
      // Refresh both users and pending requests
      await fetchUsers();
      await fetchPendingRequests();
      alert('Request approved successfully.'); // Optional success message
    } catch (err) {
      setRequestsError(err instanceof Error ? err.message : 'Failed to approve request');
    }
  };

  // Function to handle rejecting a signup request
  const handleRejectRequest = async (requestId: string) => {
    // Optional: Add confirmation dialog
    if (!confirm('Are you sure you want to reject this signup request?')) return;
    setRequestsError(null); // Clear previous request errors
    try {
      await adminService.rejectAdminRequest(requestId);
      // Refresh pending requests
      await fetchPendingRequests();
      alert('Request rejected successfully.'); // Optional success message
    } catch (err) {
      setRequestsError(err instanceof Error ? err.message : 'Failed to reject request');
    }
  };

  if (loading) return <div>Loading...</div>;
  // Keep the main error display for user fetching errors
  // if (error) return <div className="text-red-500">Error: {error}</div>;

  return (
    <div className="container mx-auto px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">User Management</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        >
          Create User
        </button>
      </div>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Roles</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {users.map((user) => (
              <tr key={user.id}>
                <td className="px-6 py-4 whitespace-nowrap">{user.username}</td>
                <td className="px-6 py-4 whitespace-nowrap">{user.email}</td>
                <td className="px-6 py-4 whitespace-nowrap">{user.roles.join(', ')}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    user.enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {user.enabled ? 'Active' : 'Locked'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    onClick={() => handleLockUser(user.id, !user.enabled)}
                    className="text-indigo-600 hover:text-indigo-900 mr-4"
                  >
                    {user.enabled ? 'Lock' : 'Unlock'}
                  </button>
                  <button
                    onClick={() => handleOpenEditModal(user)} // Add Edit button handler
                    className="text-yellow-600 hover:text-yellow-900 mr-4"
                  >
                    Edit
                  </button>
                   <button
                    onClick={() => handleOpenResetPasswordModal(user)} // Add Reset Password button handler
                    className="text-purple-600 hover:text-purple-900 mr-4"
                  >
                    Reset PW
                  </button>
                  <button
                    onClick={() => handleDeleteUser(user.id)}
                    className="text-red-600 hover:text-red-900"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {error && <div className="text-red-500 p-4">User Loading Error: {error}</div>} {/* Show user loading error below table */}
      </div>

      {/* Pending Admin Signup Requests Section */}
      <div className="mt-10">
        <h2 className="text-xl font-bold mb-4">Pending Admin Signup Requests</h2>
        {requestsLoading && <div>Loading requests...</div>}
        {requestsError && <div className="text-red-500 mb-4">Error loading requests: {requestsError}</div>}
        {!requestsLoading && !requestsError && pendingRequests.length === 0 && (
          <p>No pending signup requests.</p>
        )}
        {!requestsLoading && !requestsError && pendingRequests.length > 0 && (
          <div className="bg-white shadow-md rounded-lg overflow-hidden">
            <table className="min-w-full">
              <thead className="bg-gray-50">
                <tr>
                  {/* Adjust columns based on AdminSignupRequest properties */}
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                  {/* Add other relevant columns like 'requestDate' if available */}
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {pendingRequests.map((request) => (
                  // Assuming AdminSignupRequest has 'id', 'username', 'email'
                  <tr key={request.id}>
                    <td className="px-6 py-4 whitespace-nowrap">{request.username}</td>
                    <td className="px-6 py-4 whitespace-nowrap">{request.email}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <button
                        onClick={() => handleApproveRequest(request.id)}
                        className="text-green-600 hover:text-green-900 mr-4"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleRejectRequest(request.id)}
                        className="text-red-600 hover:text-red-900"
                      >
                        Reject
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>


      {/* Modal for Create/Edit User */}
      {(showCreateModal || showEditModal) && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"> {/* Added z-index */}
          <div className="bg-white p-6 rounded-lg w-96 max-h-[90vh] overflow-y-auto"> {/* Added max-height and overflow */}
            <h2 className="text-xl font-bold mb-4">{editingUser ? 'Edit User' : 'Create New User'}</h2>
            {/* Use handleUpdateUser when editing, handleCreateUser when creating */}
            <form onSubmit={editingUser ? handleUpdateUser : handleCreateUser}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Username</label>
                <input
                  type="text"
                  value={newUser.username}
                  onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  required
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Email</label>
                <input
                  type="email"
                  value={newUser.email}
                  onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  required
                />
              </div>
              {/* Conditionally render password field only for create */}
              {!editingUser && (
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700">Password</label>
                  <input
                    type="password"
                    value={newUser.password}
                    onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                    required={!editingUser} // Required only when creating
                  />
                </div>
              )}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">Roles</label>
                <select
                  multiple
                  value={newUser.roles}
                  onChange={(e) => setNewUser({ ...newUser, roles: Array.from(e.target.selectedOptions, option => option.value) })}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  disabled={rolesLoading} // Disable while loading roles
                >
                  {rolesLoading && <option>Loading roles...</option>}
                  {rolesError && <option>Error loading roles</option>}
                  {!rolesLoading && !rolesError && availableRoles.map((role) => (
                    // Authority has 'id' (optional) and 'roleName' properties
                    <option key={role.id || role.roleName} value={role.roleName}>
                      {role.roleName}
                    </option>
                  ))}
                </select>
                {rolesError && <p className="text-xs text-red-500 mt-1">{rolesError}</p>}
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={handleCloseModals} // Use unified close handler
                  className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
                  disabled={rolesLoading} // Disable submit while roles are loading
                >
                  {editingUser ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal for Reset Password */}
      {showResetPasswordModal && resettingUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96">
            <h2 className="text-xl font-bold mb-4">Reset Password for {resettingUser.username}</h2>
            {error && <p className="text-red-500 mb-4">Error: {error}</p>} {/* Display error within modal */}
            <form onSubmit={handleResetPassword}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">New Password</label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  required
                  autoFocus // Focus on the password field when modal opens
                />
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={handleCloseModals}
                  className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700"
                >
                  Reset Password
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
