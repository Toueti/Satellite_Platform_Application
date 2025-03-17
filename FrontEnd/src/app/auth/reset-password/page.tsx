'use client'

import { useState } from 'react'
import Link from 'next/link'
import Image from 'next/image'

export default function ResetPassword() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      // TODO: Implement password reset logic with your backend API
      const response = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      })

      if (response.ok) {
        setMessage('Reset link has been sent to your email.')
        setError('')
      } else {
        setError('Failed to send reset link. Please try again.')
        setMessage('')
      }
    } catch (err) {
      setError('An error occurred. Please try again.')
      setMessage('')
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="max-w-md mx-auto">
          <div className="text-center mb-8">
            <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl">
              <span className="block">Reset Your</span>
              <span className="block text-primary-600">Password</span>
            </h1>
            <p className="mt-3 text-base text-gray-500 sm:text-lg">
              Enter your email and we'll send you instructions to reset your password.
            </p>
          </div>

          <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  Email Address
                </label>
                <input
                  id="email"
                  type="email"
                  required
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>

              {error && (
                <div className="text-red-600 text-sm text-center">{error}</div>
              )}

              {message && (
                <div className="text-green-600 text-sm text-center">{message}</div>
              )}

              <button
                type="submit"
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Send Reset Link
              </button>

              <div className="text-center">
                <Link href="/auth/login" className="font-medium text-primary-600 hover:text-primary-500">
                  Return to Sign In
                </Link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
