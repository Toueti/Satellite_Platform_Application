'use client';

import React, { useState, useEffect } from 'react';

interface StorageStats {
  totalSpace: number;
  usedSpace: number;
  availableSpace: number;
  usagePercentage: number;
}

export default function StorageManagement() {
  const [stats, setStats] = useState<StorageStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStorageStats();
  }, []);

  const fetchStorageStats = async () => {
    try {
      const response = await fetch('/api/admin/storage/usage', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      if (!response.ok) throw new Error('Failed to fetch storage stats');
      const data = await response.json();
      setStats(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const formatBytes = (bytes: number) => {
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes === 0) return '0 Byte';
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${Math.round(bytes / Math.pow(1024, i))} ${sizes[i]}`;
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div className="text-red-500">Error: {error}</div>;
  if (!stats) return <div>No storage statistics available</div>;

  return (
    <div className="container mx-auto px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Storage Management</h1>
        <p className="text-gray-600 mt-2">Monitor and manage system storage usage</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold text-gray-700">Total Space</h3>
          <p className="text-2xl font-bold text-indigo-600">{formatBytes(stats.totalSpace)}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold text-gray-700">Used Space</h3>
          <p className="text-2xl font-bold text-red-600">{formatBytes(stats.usedSpace)}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold text-gray-700">Available Space</h3>
          <p className="text-2xl font-bold text-green-600">{formatBytes(stats.availableSpace)}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold text-gray-700">Usage Percentage</h3>
          <p className="text-2xl font-bold text-blue-600">{stats.usagePercentage.toFixed(2)}%</p>
        </div>
      </div>

      <div className="mt-8 bg-white p-6 rounded-lg shadow-md">
        <h2 className="text-xl font-bold mb-4">Storage Usage Progress</h2>
        <div className="w-full bg-gray-200 rounded-full h-4">
          <div
            className={`h-4 rounded-full ${
              stats.usagePercentage > 90
                ? 'bg-red-600'
                : stats.usagePercentage > 70
                ? 'bg-yellow-600'
                : 'bg-green-600'
            }`}
            style={{ width: `${stats.usagePercentage}%` }}
          ></div>
        </div>
        <div className="mt-2 text-sm text-gray-600">
          {stats.usagePercentage > 90
            ? 'Critical: Storage space is almost full'
            : stats.usagePercentage > 70
            ? 'Warning: Storage space is getting low'
            : 'Storage space is healthy'}
        </div>
      </div>

      <div className="mt-8 bg-white p-6 rounded-lg shadow-md">
        <h2 className="text-xl font-bold mb-4">Storage Management Tips</h2>
        <ul className="list-disc list-inside space-y-2 text-gray-600">
          <li>Regularly clean up unused files and data</li>
          <li>Archive old data that is no longer frequently accessed</li>
          <li>Monitor storage usage trends to plan for future capacity needs</li>
          <li>Consider implementing data retention policies</li>
          <li>Set up automated cleanup tasks for temporary files</li>
        </ul>
      </div>
    </div>
  );
} 