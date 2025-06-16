'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { DashboardSummary } from '@/types/admin'; // Import DashboardSummary type
import { adminService } from '@/services/admin.service'; // Import adminService

export default function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dashboardData, setDashboardData] = useState<DashboardSummary | null>(null); // State for dashboard data

  // Fetch dashboard data
  const fetchDashboardData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await adminService.getDashboardSummary();
      setDashboardData(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch dashboard data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  if (loading) return <div>Loading dashboard data...</div>;
  if (error) return <div className="text-red-500">Error: {error}</div>;

  return (
    <div className="container mx-auto px-4">
      <h1 className="text-2xl font-bold mb-6">Admin Dashboard</h1>

      {/* Dashboard Widgets/Cards will go here (AD4) */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Placeholder Card 1 */}
        {/* Card 1: Total Users */}
        <div className="bg-white shadow-md rounded-lg p-6">
          <h2 className="text-lg font-semibold text-gray-700 mb-2">Total Users</h2>
          <p className="text-3xl font-bold text-gray-900">{dashboardData?.totalUsers ?? '--'}</p>
        </div>
        {/* Card 2: Total Projects */}
        <div className="bg-white shadow-md rounded-lg p-6">
          <h2 className="text-lg font-semibold text-gray-700 mb-2">Total Projects</h2>
          <p className="text-3xl font-bold text-gray-900">{dashboardData?.totalProjects ?? '--'}</p>
        </div>
        {/* Card 3: Pending Signups */}
        <div className="bg-white shadow-md rounded-lg p-6">
          <h2 className="text-lg font-semibold text-gray-700 mb-2">Pending Signups</h2>
          <p className="text-3xl font-bold text-gray-900">{dashboardData?.pendingSignups ?? '--'}</p>
        </div>
        {/* Add more placeholder cards as needed */}
      </div>

      {/* Other dashboard sections (e.g., recent activity, charts) can go here */}
    </div>
  );
}
