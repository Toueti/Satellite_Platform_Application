"use client";

import { useEffect } from 'react';
import dynamic from 'next/dynamic';
import Link from 'next/link';

// Fix for Leaflet map rendering issues with SSR
const MapPage = dynamic(() => import('./page'), { ssr: false });

export default function MapLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  useEffect(() => {
    // Fix for Leaflet icons in Next.js
    const L = require('leaflet');
    delete L.Icon.Default.prototype._getIconUrl;
    L.Icon.Default.mergeOptions({
      iconRetinaUrl: '/leaflet/marker-icon-2x.png',
      iconUrl: '/leaflet/marker-icon.png',
      shadowUrl: '/leaflet/marker-shadow.png',
    });
  }, []);

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-blue-600 shadow-md">
        <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div className="flex items-center">
              <Link href="/" className="text-xl font-bold text-white">
                Satellite Platform
              </Link>
            </div>
            <div className="flex space-x-4">
              <Link href="/map" className="text-white hover:text-blue-100">
                Map
              </Link>
              <Link href="/dashboard" className="text-white hover:text-blue-100">
                Dashboard
              </Link>
            </div>
          </div>
        </nav>
      </header>
      <main className="flex-1 container mx-auto px-4 py-6" id="modal-root">
        {children}
      </main>
    </div>
  );
}
