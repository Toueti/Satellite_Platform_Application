'use client';

import { useState } from 'react';

interface StorageItem {
    id: string;
    name: string;
    type: 'image' | 'data' | 'result';
    size: number;
    createdAt: string;
    project: string;
}

export default function StoragePage() {
    const [view, setView] = useState<'grid' | 'list'>('grid');
    const [filter, setFilter] = useState('all');
    const [searchQuery, setSearchQuery] = useState('');
    const [items, setItems] = useState<StorageItem[]>([]);
    const [uploading, setUploading] = useState(false);

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) return;

        setUploading(true);
        try {
            // TODO: Implement file upload
            console.log('Uploading files:', files);
        } finally {
            setUploading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
                <div className="text-center mb-12">
                    <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
                        <span className="block">Storage</span>
                        <span className="block text-primary-600">Management</span>
                    </h1>
                    <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
                        Manage and organize your satellite imagery and analysis results
                    </p>
                </div>

                <div className="bg-white shadow-lg rounded-lg p-8">
                    <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
                        <div className="flex-1">
                            <input
                                type="text"
                                placeholder="Search files..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-primary-600 focus:border-primary-600"
                            />
                        </div>
                        <div className="flex items-center space-x-4">
                            <select
                                value={filter}
                                onChange={(e) => setFilter(e.target.value)}
                                className="px-4 py-2 border border-gray-300 rounded-md focus:ring-primary-600 focus:border-primary-600"
                            >
                                <option value="all">All Files</option>
                                <option value="image">Images</option>
                                <option value="data">Data</option>
                                <option value="result">Results</option>
                            </select>
                            <label className="inline-flex items-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-600 cursor-pointer">
                                <span>{uploading ? 'Uploading...' : 'Upload Files'}</span>
                                <input
                                    type="file"
                                    multiple
                                    className="hidden"
                                    onChange={handleFileUpload}
                                    disabled={uploading}
                                />
                            </label>
                        </div>
                    </div>

                    <div className="flex justify-end mb-6">
                        <div className="inline-flex rounded-md shadow-sm">
                            <button
                                onClick={() => setView('grid')}
                                className={`px-4 py-2 text-sm font-medium rounded-l-md border ${
                                    view === 'grid'
                                        ? 'bg-primary-600 text-white border-primary-600'
                                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                                }`}
                            >
                                Grid View
                            </button>
                            <button
                                onClick={() => setView('list')}
                                className={`px-4 py-2 text-sm font-medium rounded-r-md border-t border-r border-b ${
                                    view === 'list'
                                        ? 'bg-primary-600 text-white border-primary-600'
                                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                                }`}
                            >
                                List View
                            </button>
                        </div>
                    </div>

                    {view === 'grid' ? (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                            {items.length === 0 ? (
                                <div className="col-span-full text-center py-12">
                                    <p className="text-gray-500 text-lg">No files found</p>
                                    <p className="text-gray-400 mt-2">Upload files to get started</p>
                                </div>
                            ) : (
                                items.map((item) => (
                                    <div
                                        key={item.id}
                                        className="border-2 rounded-lg p-4 hover:border-primary-600 transition-colors"
                                    >
                                        <div className="aspect-w-16 aspect-h-9 bg-gray-100 rounded-md mb-2">
                                            {/* TODO: Add preview */}
                                        </div>
                                        <h3 className="font-medium truncate">{item.name}</h3>
                                        <p className="text-sm text-gray-500">
                                            {new Date(item.createdAt).toLocaleDateString()}
                                        </p>
                                    </div>
                                ))
                            )}
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead>
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Name
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Type
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Size
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Created
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Project
                                        </th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {items.length === 0 ? (
                                        <tr>
                                            <td colSpan={5} className="px-6 py-12 text-center">
                                                <p className="text-gray-500 text-lg">No files found</p>
                                                <p className="text-gray-400 mt-2">Upload files to get started</p>
                                            </td>
                                        </tr>
                                    ) : (
                                        items.map((item) => (
                                            <tr key={item.id} className="hover:bg-gray-50">
                                                <td className="px-6 py-4 whitespace-nowrap">{item.name}</td>
                                                <td className="px-6 py-4 whitespace-nowrap">{item.type}</td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    {(item.size / 1024).toFixed(2)} KB
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    {new Date(item.createdAt).toLocaleDateString()}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">{item.project}</td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
