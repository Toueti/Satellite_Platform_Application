import './globals.css';
import { Inter } from 'next/font/google';
import Header from '@/components/Header';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { AuthProvider } from '@/hooks/useAuth'; // Import AuthProvider

const inter = Inter({ subsets: ['latin'] })

export const metadata = {
  title: 'Satellite Image Processing Platform',
  description: 'A modern platform for satellite imagery analysis and geospatial data processing',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <AuthProvider> {/* Wrap with AuthProvider */}
          <ErrorBoundary>
            <Header title="SatelliteIP" /> {/* Added default title prop */}
            <main className="min-h-screen">
              {children}
            </main>
          </ErrorBoundary>
        </AuthProvider>
      </body>
    </html>
  )
}
