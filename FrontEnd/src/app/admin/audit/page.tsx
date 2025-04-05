'use client';

import React, { useState, useEffect } from 'react';

interface AuditLog {
  timestamp: string;
  username: string;
  action: string;
  details: string;
}

export default function AuditLogs() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState({
    username: '',
    action: '',
    startDate: '',
    endDate: '',
  });
  const [availableDates, setAvailableDates] = useState<string[]>([]);

  useEffect(() => {
    fetchAvailableDates();
    fetchLogs();
  }, []);

  const fetchAvailableDates = async () => {
    try {
      const response = await fetch('/api/admin/audit/available-dates', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      if (!response.ok) throw new Error('Failed to fetch available dates');
      const data = await response.json();
      setAvailableDates(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    }
  };

  const fetchLogs = async () => {
    try {
      let url = '/api/admin/audit/latest';
      const params = new URLSearchParams();
      
      if (filters.username) params.append('username', filters.username);
      if (filters.action) params.append('action', filters.action);
      if (filters.startDate && filters.endDate) {
        url = '/api/admin/audit/by-range';
        params.append('startDate', filters.startDate);
        params.append('endDate', filters.endDate);
      } else if (filters.startDate) {
        url = `/api/admin/audit/by-date/${filters.startDate}`;
      }

      const response = await fetch(`${url}?${params.toString()}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      if (!response.ok) throw new Error('Failed to fetch audit logs');
      const data = await response.json();
      setLogs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
  };

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchLogs();
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div className="text-red-500">Error: {error}</div>;

  return (
    <div className="container mx-auto px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-4">Audit Logs</h1>
        
        <form onSubmit={handleFilterSubmit} className="bg-white p-4 rounded-lg shadow-md mb-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Username</label>
              <input
                type="text"
                name="username"
                value={filters.username}
                onChange={handleFilterChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Action</label>
              <input
                type="text"
                name="action"
                value={filters.action}
                onChange={handleFilterChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Start Date</label>
              <input
                type="date"
                name="startDate"
                value={filters.startDate}
                onChange={handleFilterChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">End Date</label>
              <input
                type="date"
                name="endDate"
                value={filters.endDate}
                onChange={handleFilterChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
              />
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <button
              type="submit"
              className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
            >
              Apply Filters
            </button>
          </div>
        </form>
      </div>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Timestamp</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Details</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {logs.map((log, index) => (
              <tr key={index}>
                <td className="px-6 py-4 whitespace-nowrap">{new Date(log.timestamp).toLocaleString()}</td>
                <td className="px-6 py-4 whitespace-nowrap">{log.username}</td>
                <td className="px-6 py-4 whitespace-nowrap">{log.action}</td>
                <td className="px-6 py-4 whitespace-nowrap">{log.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {availableDates.length > 0 && (
        <div className="mt-4">
          <h2 className="text-lg font-semibold mb-2">Available Log Dates</h2>
          <div className="flex flex-wrap gap-2">
            {availableDates.map((date) => (
              <button
                key={date}
                onClick={() => {
                  setFilters(prev => ({ ...prev, startDate: date, endDate: '' }));
                  fetchLogs();
                }}
                className="bg-gray-100 hover:bg-gray-200 text-gray-800 px-3 py-1 rounded"
              >
                {date}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
} 