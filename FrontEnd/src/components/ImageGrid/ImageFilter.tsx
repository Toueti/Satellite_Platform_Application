import React, { useState, useEffect } from 'react';
import {
    Box,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    OutlinedInput,
    Chip,
    SelectChangeEvent,
    Button,
    Paper,
    // Grid removed
    IconButton,
    Collapse,
    Tooltip,
    InputAdornment,
} from '@mui/material';
// Assuming HeroIcons are correctly set up
import { FunnelIcon, MagnifyingGlassIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { ImageFilter } from '@/services/images.service';
import { useDebouncedCallback } from 'use-debounce';

interface ImageFilterProps {
    availableTags: string[];
    onFilterChange: (filters: ImageFilter) => void;
    initialFilters?: ImageFilter;
}

const ImageFilterComponent: React.FC<ImageFilterProps> = ({
    availableTags,
    onFilterChange,
    initialFilters = {},
}) => {
    const [searchTerm, setSearchTerm] = useState<string>(initialFilters.searchTerm || '');
    const [selectedTags, setSelectedTags] = useState<string[]>(initialFilters.tags || []);
    const [sortBy, setSortBy] = useState<ImageFilter['sortBy']>(initialFilters.sortBy || 'uploadDate');
    const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>(initialFilters.sortOrder || 'desc');
    const [showFilters, setShowFilters] = useState(false);

    const debouncedApplyFilters = useDebouncedCallback((filters: ImageFilter) => {
        onFilterChange(filters);
    }, 300);

    useEffect(() => {
        const filters: ImageFilter = {
            searchTerm: searchTerm || undefined,
            tags: selectedTags.length > 0 ? selectedTags : undefined,
            sortBy: sortBy,
            sortOrder: sortOrder,
        };
        debouncedApplyFilters(filters);
    }, [searchTerm, selectedTags, sortBy, sortOrder, debouncedApplyFilters]);

    useEffect(() => {
        setSearchTerm(initialFilters.searchTerm || '');
        setSelectedTags(initialFilters.tags || []);
        setSortBy(initialFilters.sortBy || 'uploadDate');
        setSortOrder(initialFilters.sortOrder || 'desc');
    }, [initialFilters]);

    const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSearchTerm(event.target.value);
    };

    const handleTagChange = (event: SelectChangeEvent<typeof selectedTags>) => {
        const { target: { value } } = event;
        setSelectedTags(typeof value === 'string' ? value.split(',') : value);
    };

    const handleSortByChange = (event: SelectChangeEvent) => {
        setSortBy(event.target.value as ImageFilter['sortBy']);
    };

    const handleSortOrderChange = (event: SelectChangeEvent) => {
        setSortOrder(event.target.value as 'asc' | 'desc');
    };

    const clearFilters = () => {
        const clearedFilters: ImageFilter = {
             searchTerm: undefined, tags: undefined, sortBy: 'uploadDate', sortOrder: 'desc',
        };
        setSearchTerm('');
        setSelectedTags([]);
        setSortBy('uploadDate');
        setSortOrder('desc');
        setShowFilters(false);
        onFilterChange(clearedFilters);
        debouncedApplyFilters.cancel();
    };

    return (
        <Paper sx={{ p: 2, mb: 3, overflow: 'hidden' }}>
            {/* Top Row: Search and Filter Toggle */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: showFilters ? 2 : 0 }}>
                <Box sx={{ flexGrow: 1 }}> {/* Search takes remaining space */}
                    <TextField
                        fullWidth
                        label="Search Images..."
                        variant="outlined"
                        size="small"
                        value={searchTerm}
                        onChange={handleSearchChange}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <MagnifyingGlassIcon style={{ width: 20, height: 20, color: 'grey' }} />
                                </InputAdornment>
                            ),
                        }}
                    />
                </Box>
                <Box> {/* Filter toggle button */}
                    <Tooltip title={showFilters ? "Hide Filters" : "Show Filters"}>
                        <IconButton onClick={() => setShowFilters(!showFilters)} color={showFilters ? "primary" : "default"}>
                             <FunnelIcon style={{ width: 24, height: 24 }} />
                        </IconButton>
                    </Tooltip>
                </Box>
            </Box>

            {/* Collapsible Filter Section */}
            <Collapse in={showFilters} timeout="auto" unmountOnExit>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                    {/* Tags Filter */}
                    <Box sx={{ flexBasis: { xs: '100%', sm: 'calc(50% - 8px)', md: 'calc(41.66% - 12.8px)' }, flexGrow: 1 }}> {/* Approx md={5} */}
                        <FormControl fullWidth size="small">
                            <InputLabel id="tags-filter-label">Filter by Tags</InputLabel>
                            <Select
                                labelId="tags-filter-label"
                                multiple
                                value={selectedTags}
                                onChange={handleTagChange}
                                input={<OutlinedInput id="select-multiple-chip" label="Filter by Tags" />}
                                renderValue={(selected) => (
                                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                        {selected.map((value) => <Chip key={value} label={value} size="small" />)}
                                    </Box>
                                )}
                                MenuProps={{ PaperProps: { style: { maxHeight: 224 } } }}
                            >
                                {availableTags.map((tag) => <MenuItem key={tag} value={tag}>{tag}</MenuItem>)}
                            </Select>
                        </FormControl>
                    </Box>

                    {/* Sort By */}
                    <Box sx={{ flexBasis: { xs: 'calc(50% - 8px)', sm: 'calc(25% - 12px)', md: 'calc(25% - 12px)' }, flexGrow: 1 }}> {/* Approx md={3} */}
                        <FormControl fullWidth size="small">
                            <InputLabel id="sort-by-label">Sort By</InputLabel>
                            <Select labelId="sort-by-label" value={sortBy} label="Sort By" onChange={handleSortByChange}>
                                <MenuItem value="uploadDate">Upload Date</MenuItem>
                                <MenuItem value="captureDate">Capture Date</MenuItem>
                                <MenuItem value="name">Name</MenuItem>
                                <MenuItem value="size">Size</MenuItem>
                            </Select>
                        </FormControl>
                    </Box>

                    {/* Sort Order */}
                    <Box sx={{ flexBasis: { xs: 'calc(50% - 8px)', sm: 'calc(25% - 12px)', md: 'calc(16.66% - 13.3px)' }, flexGrow: 1 }}> {/* Approx md={2} */}
                        <FormControl fullWidth size="small">
                            <InputLabel id="sort-order-label">Order</InputLabel>
                            <Select labelId="sort-order-label" value={sortOrder} label="Order" onChange={handleSortOrderChange}>
                                <MenuItem value="desc">Desc</MenuItem>
                                <MenuItem value="asc">Asc</MenuItem>
                            </Select>
                        </FormControl>
                    </Box>

                     {/* Clear Button */}
                    <Box sx={{ flexBasis: { xs: '100%', sm: '100%', md: 'calc(16.66% - 13.3px)' }, display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}> {/* Approx md={2} */}
                         <Button variant="text" onClick={clearFilters} size="small" startIcon={<XMarkIcon style={{ width: 16, height: 16 }} />}>
                             Clear Filters
                         </Button>
                    </Box>
                </Box>
            </Collapse>
        </Paper>
    );
};

export default ImageFilterComponent;
