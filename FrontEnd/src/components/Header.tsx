'use client'

'use client'

import Link from 'next/link'
import Image from 'next/image'
import { useState, useEffect } from 'react';
import { authService } from '../services/auth.service';

export default function Header() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    setIsAuthenticated(authService.isAuthenticated());

    // Optional: Add event listener for logout to update header
    const logoutListener = () => setIsAuthenticated(false);
    window.addEventListener('logout', logoutListener);

    return () => {
      window.removeEventListener('logout', logoutListener);
    };
  }, []);

  return (
    <header className="header-background">
      <div className="header-content max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div className="flex justify-between items-center">
          <div className="flex items-center">
            <Link href="/" className="flex items-center space-x-2">
              <Image
                src="/images/logo.png"
                alt="Logo"
                width={40}
                height={40}
                className="w-10 h-10"
              />
              <span className="text-xl font-bold text-white">SatelliteIP</span>
            </Link>
          </div>
          
          <nav className="hidden md:flex space-x-8">
            <Link href="/" className="text-white hover:text-blue-200 transition-colors">
              Home
            </Link>
            <Link href="/dashboard" className="text-white hover:text-blue-200 transition-colors">
              Dashboard
            </Link>
            <Link href="/about-us" className="text-white hover:text-blue-200 transition-colors">
              About Us
            </Link>
            <Link href="/contact" className="text-white hover:text-blue-200 transition-colors">
              Contact
            </Link>
            {!isAuthenticated && (
              <Link 
                href="/auth/login" 
                className="text-white hover:text-blue-200 transition-colors"
              >
                Sign In
              </Link>
            )}
          </nav>

          {/* Mobile menu button */}
          <div className="md:hidden">
            <button
              type="button"
              title="Menu"
              className="text-white hover:text-blue-200 focus:outline-none"
            >
              <svg
                className="h-6 w-6"
                fill="none"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path d="M4 6h16M4 12h16M4 18h16"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </header>
  )
}
