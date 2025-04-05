'use client'

import { Fragment } from 'react'
import { Disclosure, Menu, Transition } from '@headlessui/react'
import { Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { Box, List, ListItem, ListItemIcon, ListItemText } from '@mui/material';
import { HomeIcon, FolderIcon, ChartBarIcon, Cog6ToothIcon } from '@heroicons/react/24/outline';

const navigation = [
  { name: 'Projects', href: '/projects' },
  { name: 'Analysis', href: '/analysis' },
  { name: 'Storage', href: '/storage' },
]

function classNames(...classes: string[]) {
  return classes.filter(Boolean).join(' ')
}

export default function Navigation() {
  const router = useRouter();
  const pathname = usePathname();

  const handleNavigation = (path: string) => {
    router.push(path);
  };

  const navigationItems = [
    { name: 'Dashboard', path: '/dashboard', icon: HomeIcon },
    { name: 'Projects', path: '/projects', icon: FolderIcon },
    { name: 'Analysis', path: '/analysis', icon: ChartBarIcon },
    { name: 'Settings', path: '/settings', icon: Cog6ToothIcon },
  ];

  return (
    <Disclosure as="nav" className="bg-white shadow">
      {({ open }) => (
        <>
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="flex h-16 justify-between">
              <div className="flex">
                <div className="flex flex-shrink-0 items-center">
                  <Link href="/" className="text-xl font-bold text-primary-600">
                    SatelliteIP
                  </Link>
                </div>
                <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                  {navigationItems.map((item) => (
                    <div
                      key={item.name}
                      onClick={() => handleNavigation(item.path)}
                      className={`cursor-pointer group flex gap-x-3 rounded-md p-2 text-sm leading-6 font-semibold ${
                        pathname === item.path
                          ? 'bg-gray-800 text-white'
                          : 'text-gray-400 hover:text-white hover:bg-gray-800'
                      }`}
                    >
                      <item.icon className="h-6 w-6 shrink-0" aria-hidden="true" />
                      {item.name}
                    </div>
                  ))}
                </div>
              </div>
              <div className="-mr-2 flex items-center sm:hidden">
                {/* Mobile menu button */}
                <Disclosure.Button className="inline-flex items-center justify-center rounded-md p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary-500">
                  <span className="sr-only">Open main menu</span>
                  {open ? (
                    <XMarkIcon className="block h-6 w-6" aria-hidden="true" />
                  ) : (
                    <Bars3Icon className="block h-6 w-6" aria-hidden="true" />
                  )}
                </Disclosure.Button>
              </div>
            </div>
          </div>

          <Disclosure.Panel className="sm:hidden">
            <div className="space-y-1 pb-3 pt-2">
              {navigationItems.map((item) => (
                <Disclosure.Button
                  key={item.name}
                  as={Link}
                  href={item.path}
                  className={classNames(
                    pathname === item.path
                      ? 'border-primary-500 bg-primary-50 text-primary-700'
                      : 'border-transparent text-gray-500 hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700',
                    'block border-l-4 py-2 pl-3 pr-4 text-base font-medium'
                  )}
                >
                  {item.name}
                </Disclosure.Button>
              ))}
            </div>
          </Disclosure.Panel>
        </>
      )}
    </Disclosure>
  )
}
