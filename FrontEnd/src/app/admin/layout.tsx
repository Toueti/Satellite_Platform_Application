'use client'; // Add this directive

import React from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, loading } = useAuth();
  const router = useRouter();

  React.useEffect(() => {
    if (!loading && (!user || !user.roles.includes('ADMIN'))) {
      router.push('/auth/signin');
    }
  }, [user, loading, router]);

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!user || !user.roles.includes('ADMIN')) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="flex">
        <nav className="w-64 min-h-screen bg-gray-800 text-white p-4">
          <h2 className="text-xl font-bold mb-4">Admin Dashboard</h2>
          <ul className="space-y-2">
            <li>
              <a href="/admin/users" className="block py-2 px-4 hover:bg-gray-700 rounded">
                User Management
              </a>
            </li>
            <li>
              <a href="/admin/roles" className="block py-2 px-4 hover:bg-gray-700 rounded">
                Role Management
              </a>
            </li>
            <li>
              <a href="/admin/config" className="block py-2 px-4 hover:bg-gray-700 rounded">
                System Configuration
              </a>
            </li>
            <li>
              <a href="/admin/audit" className="block py-2 px-4 hover:bg-gray-700 rounded">
                Audit Logs
              </a>
            </li>
            <li>
              <a href="/admin/storage" className="block py-2 px-4 hover:bg-gray-700 rounded">
                Storage Management
              </a>
            </li>
            <li>
              <a href="/admin/tasks" className="block py-2 px-4 hover:bg-gray-700 rounded">
                Task Management
              </a>
            </li>
          </ul>
        </nav>
        <main className="flex-1 p-8">
          {children}
        </main>
      </div>
    </div>
  );
}
