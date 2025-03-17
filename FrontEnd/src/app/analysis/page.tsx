'use client';

import { useState } from 'react';

export default function AnalysisPage() {
    const [selectedProject, setSelectedProject] = useState('');
    const [analysisType, setAnalysisType] = useState('');
    const [parameters, setParameters] = useState({
        startDate: '',
        endDate: '',
        region: '',
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        // TODO: Implement analysis submission
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
                <div className="text-center mb-12">
                    <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
                        <span className="block">Image Analysis</span>
                        <span className="block text-primary-600">Processing Tools</span>
                    </h1>
                    <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
                        Process and analyze satellite imagery with advanced algorithms
                    </p>
                </div>

                <div className="max-w-4xl mx-auto">
                    <form onSubmit={handleSubmit} className="space-y-8 bg-white shadow-lg rounded-lg p-8">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Select Project
                            </label>
                            <select
                                value={selectedProject}
                                onChange={(e) => setSelectedProject(e.target.value)}
                                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary-600 focus:border-primary-600 rounded-md"
                            >
                                <option value="">Select a project</option>
                                {/* TODO: Add project options */}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-4">
                                Analysis Type
                            </label>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                <button
                                    type="button"
                                    onClick={() => setAnalysisType('vegetation')}
                                    className={`p-6 border-2 rounded-lg text-center hover:border-primary-600 transition-colors ${
                                        analysisType === 'vegetation' ? 'border-primary-600 bg-primary-50' : 'border-gray-200'
                                    }`}
                                >
                                    <div className="font-semibold text-lg mb-2">Vegetation Analysis</div>
                                    <div className="text-sm text-gray-500">NDVI, EVI, LAI</div>
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setAnalysisType('water')}
                                    className={`p-6 border-2 rounded-lg text-center hover:border-primary-600 transition-colors ${
                                        analysisType === 'water' ? 'border-primary-600 bg-primary-50' : 'border-gray-200'
                                    }`}
                                >
                                    <div className="font-semibold text-lg mb-2">Water Analysis</div>
                                    <div className="text-sm text-gray-500">NDWI, Water Bodies</div>
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setAnalysisType('land')}
                                    className={`p-6 border-2 rounded-lg text-center hover:border-primary-600 transition-colors ${
                                        analysisType === 'land' ? 'border-primary-600 bg-primary-50' : 'border-gray-200'
                                    }`}
                                >
                                    <div className="font-semibold text-lg mb-2">Land Analysis</div>
                                    <div className="text-sm text-gray-500">Land Use, Urban Areas</div>
                                </button>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Start Date
                                </label>
                                <input
                                    type="date"
                                    value={parameters.startDate}
                                    onChange={(e) => setParameters({ ...parameters, startDate: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-primary-600 focus:border-primary-600"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    End Date
                                </label>
                                <input
                                    type="date"
                                    value={parameters.endDate}
                                    onChange={(e) => setParameters({ ...parameters, endDate: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-primary-600 focus:border-primary-600"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Region of Interest
                            </label>
                            <textarea
                                value={parameters.region}
                                onChange={(e) => setParameters({ ...parameters, region: e.target.value })}
                                rows={4}
                                placeholder="Enter GeoJSON coordinates or draw on map"
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-primary-600 focus:border-primary-600"
                            />
                        </div>

                        <div className="flex justify-end">
                            <button
                                type="submit"
                                className="inline-flex items-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-600 md:py-4 md:text-lg md:px-10"
                            >
                                Start Analysis
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
