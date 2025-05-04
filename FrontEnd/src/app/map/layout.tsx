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
    // Removed the specific header for the map page
    <div className="min-h-screen flex flex-col">
      {/* The main application header from src/app/layout.tsx will be used */}
      <main className="flex-1 container mx-auto px-4 py-6" id="modal-root">
        {children}
      </main>
    </div>
  );
}
