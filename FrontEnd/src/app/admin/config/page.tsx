'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { ManageablePropertyDto, UpdatePropertyRequestDto } from '@/types/config'; // Import types
import { adminService } from '@/services/admin.service';

export default function ConfigManagement() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [properties, setProperties] = useState<ManageablePropertyDto[]>([]);
  const [showEditModal, setShowEditModal] = useState(false); // State for edit modal
  const [editingProperty, setEditingProperty] = useState<ManageablePropertyDto | null>(null); // Property being edited
  const [newValue, setNewValue] = useState<string | null>(''); // New value input state

  // Fetch properties
  const fetchProperties = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await adminService.getManageableProperties();
      setProperties(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch configuration properties');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProperties();
  }, [fetchProperties]);

  if (loading) return <div>Loading configuration...</div>;
  if (error) return <div className="text-red-500">Error: {error}</div>;

  return (
    <div className="container mx-auto px-4">
      <h1 className="text-2xl font-bold mb-6">Configuration Management</h1>

      {/* Configuration Table */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Property Key</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Current Value</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {properties.length === 0 && !loading && (
              <tr>
                <td colSpan={4} className="px-6 py-4 text-center text-gray-500">No manageable properties found.</td>
              </tr>
            )}
            {properties.map((prop) => (
              <tr key={prop.key}>
                <td className="px-6 py-4 whitespace-nowrap font-mono text-sm">{prop.key}</td>
                <td className="px-6 py-4 whitespace-nowrap">{prop.currentValue ?? <span className="text-gray-400 italic">Not Set</span>}</td>
                <td className="px-6 py-4 whitespace-normal text-sm text-gray-600">{prop.description}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    onClick={() => handleOpenEditModal(prop)} // Attach edit handler
                    className="text-indigo-600 hover:text-indigo-900"
                  >
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {/* Removed extra closing div here */}

      {/* Edit Property Modal */}
      {showEditModal && editingProperty && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96">
            {/* Added non-null assertion ! */}
            <h2 className="text-xl font-bold mb-4">Edit Property: {editingProperty!.key}</h2>
            <p className="text-sm text-gray-600 mb-4">{editingProperty!.description}</p>
            {/* Display edit-specific errors */}
            {error && <p className="text-red-500 mb-2">Error: {error}</p>}
            <form onSubmit={handleUpdateProperty}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700">New Value</label>
                <input
                  type="text" // Consider different input types based on property type if available
                  value={newValue ?? ''}
                  onChange={(e) => setNewValue(e.target.value)}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                  autoFocus
                  placeholder="Enter new value (leave blank to reset/use default)"
                />
                 <p className="text-xs text-gray-500 mt-1">Leaving the value blank might reset it to its default.</p>
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowEditModal(false);
                    setEditingProperty(null);
                    setNewValue('');
                    setError(null); // Clear errors
                  }}
                  className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
                >
                  Update
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );

  // Function to open the edit modal
  function handleOpenEditModal(property: ManageablePropertyDto) {
    setEditingProperty(property);
    setNewValue(property.currentValue); // Pre-fill with current value
    setShowEditModal(true);
    setError(null); // Clear previous errors when opening modal
  }

  // Handler function for updating a property
  async function handleUpdateProperty(e: React.FormEvent) {
    e.preventDefault();
    if (!editingProperty) return;

    setError(null); // Clear previous errors

    // Refined logic: Trim if newValue is a string, otherwise treat as null. If trimmed string is empty, treat as null.
    const trimmedValue = typeof newValue === 'string' ? newValue.trim() : null;
    const valueToUpdate: string | null = trimmedValue === '' ? null : trimmedValue;

    const updateRequest: UpdatePropertyRequestDto = {
      key: editingProperty.key, // editingProperty is guaranteed non-null here by the check above
      value: valueToUpdate,
    };

    try {
      await adminService.updateManageableProperty(updateRequest);
      setShowEditModal(false); // Close modal on success
      setEditingProperty(null);
      setNewValue('');
      await fetchProperties(); // Refresh the properties list
      alert(`Property "${editingProperty.key}" updated successfully.`); // Optional success message
    } catch (err) {
      console.error("Update property error:", err); // Log the full error
      let errorMessage = 'Failed to update property.';
      if (err instanceof Error) {
        errorMessage = err.message || errorMessage;
      }
      setError(errorMessage);
      // Keep modal open on error
    }
  }
}
