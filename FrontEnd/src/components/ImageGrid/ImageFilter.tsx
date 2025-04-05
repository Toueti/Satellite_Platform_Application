import React, { useState } from 'react';
import { 
  Box, Paper, Typography, IconButton, Chip, Collapse,
  TextField, TextFieldProps, Slider, Select, MenuItem, FormControl,
  InputLabel, Button, Divider, Stack, FormControlLabel, Switch
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import FilterListIcon from '@mui/icons-material/FilterList';
import SortIcon from '@mui/icons-material/Sort';
import CloseIcon from '@mui/icons-material/Close';
import SearchIcon from '@mui/icons-material/Search';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import { ImageFilter as ImageFilterType } from '@/services/images.service';
import dayjs from 'dayjs';

interface ImageFilterProps {
  availableTags: string[];
  onFilterChange: (filters: ImageFilterType) => void;
  satellites?: string[];
  initialFilters?: Partial<ImageFilterType>;
}

interface ImageFilters {
  dateFrom: dayjs.Dayjs | null;
  dateTo: dayjs.Dayjs | null;
  cloudCoverageMax: number;
  tags: string[];
  satellite: string;
  sortBy: string;
  sortOrder: string;
}

const ImageFilter: React.FC<ImageFilterProps> = ({
  availableTags,
  onFilterChange,
  satellites = ['Landsat-8', 'Sentinel-2', 'MODIS', 'Worldview'],
  initialFilters
}) => {
  const [expanded, setExpanded] = useState(false);
  const [selectedTags, setSelectedTags] = useState<string[]>(initialFilters?.tags || []);
  const [dateFrom, setDateFrom] = useState<dayjs.Dayjs | null>(
    initialFilters?.dateFrom ? dayjs(initialFilters.dateFrom) : null
  );
  const [dateTo, setDateTo] = useState<dayjs.Dayjs | null>(
    initialFilters?.dateTo ? dayjs(initialFilters.dateTo) : null
  );
  const [satellite, setSatellite] = useState<string>(initialFilters?.satellite || '');
  const [cloudCoverage, setCloudCoverage] = useState<number>(initialFilters?.cloudCoverageMax || 100);
  const [sortBy, setSortBy] = useState<string>(initialFilters?.sortBy || 'captureDate');
  const [sortOrder, setSortOrder] = useState<string>(initialFilters?.sortOrder || 'desc');

  const handleToggleExpanded = () => {
    setExpanded(!expanded);
  };

  const handleTagToggle = (tag: string) => {
    setSelectedTags((prevTags) => {
      if (prevTags.includes(tag)) {
        return prevTags.filter(t => t !== tag);
      } else {
        return [...prevTags, tag];
      }
    });
  };

  const handleApplyFilters = () => {
    onFilterChange({
      tags: selectedTags.length > 0 ? selectedTags : undefined,
      dateFrom: dateFrom ? dateFrom.format('YYYY-MM-DD') : undefined,
      dateTo: dateTo ? dateTo.format('YYYY-MM-DD') : undefined,
      satellite: satellite || undefined,
      cloudCoverageMax: cloudCoverage < 100 ? cloudCoverage : undefined,
      sortBy: sortBy as any,
      sortOrder: sortOrder as any,
    });
  };

  const handleResetFilters = () => {
    setSelectedTags([]);
    setDateFrom(null);
    setDateTo(null);
    setSatellite('');
    setCloudCoverage(100);
    setSortBy('captureDate');
    setSortOrder('desc');
    
    onFilterChange({});
  };

  return (
    <Paper sx={{ p: 2, mb: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <FilterListIcon sx={{ mr: 1 }} />
          <Typography variant="subtitle1">Filter & Sort</Typography>
          {selectedTags.length > 0 && (
            <Chip 
              size="small" 
              label={`${selectedTags.length} tags`}
              sx={{ ml: 2 }}
            />
          )}
          {(dateFrom || dateTo) && (
            <Chip 
              size="small" 
              label="Date range"
              sx={{ ml: 1 }}
            />
          )}
          {satellite && (
            <Chip 
              size="small" 
              label={satellite}
              sx={{ ml: 1 }}
            />
          )}
          {cloudCoverage < 100 && (
            <Chip 
              size="small" 
              label={`Cloud < ${cloudCoverage}%`}
              sx={{ ml: 1 }}
            />
          )}
        </Box>
        <IconButton size="small" onClick={handleToggleExpanded}>
          {expanded ? <CloseIcon /> : <SearchIcon />}
        </IconButton>
      </Box>

      <Collapse in={expanded}>
        <Box sx={{ mt: 2 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Tags
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {availableTags.map((tag) => (
                  <Chip
                    key={tag}
                    label={tag}
                    onClick={() => handleTagToggle(tag)}
                    color={selectedTags.includes(tag) ? "primary" : "default"}
                  />
                ))}
              </Box>
            </Box>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Date Range
              </Typography>
              <LocalizationProvider dateAdapter={AdapterDayjs}>
                <Stack direction="row" spacing={2}>
                  <Box sx={{ width: '50%' }}>
                    <DatePicker
                      label="From"
                      value={dateFrom}
                      onChange={(newValue: dayjs.Dayjs | null) => setDateFrom(newValue)}
                      slotProps={{
                        textField: {
                          size: "small",
                          fullWidth: true
                        }
                      }}
                    />
                  </Box>
                  <Box sx={{ width: '50%' }}>
                    <DatePicker
                      label="To"
                      value={dateTo}
                      onChange={(newValue: dayjs.Dayjs | null) => setDateTo(newValue)}
                      slotProps={{
                        textField: {
                          size: "small",
                          fullWidth: true
                        }
                      }}
                    />
                  </Box>
                </Stack>
              </LocalizationProvider>
            </Box>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Satellite
              </Typography>
              <FormControl fullWidth size="small">
                <InputLabel id="satellite-select-label">Select Satellite</InputLabel>
                <Select
                  labelId="satellite-select-label"
                  value={satellite}
                  label="Select Satellite"
                  onChange={(e) => setSatellite(e.target.value)}
                >
                  <MenuItem value="">All Satellites</MenuItem>
                  {satellites.map((sat) => (
                    <MenuItem key={sat} value={sat}>{sat}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Cloud Coverage (Max %)
              </Typography>
              <Slider
                value={cloudCoverage}
                onChange={(_, newValue) => setCloudCoverage(newValue as number)}
                valueLabelDisplay="auto"
                step={5}
                marks
                min={0}
                max={100}
              />
            </Box>

            <Box>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <SortIcon sx={{ mr: 1 }} />
                <Typography variant="subtitle2">Sort Options</Typography>
              </Box>
              <Stack spacing={2} sx={{ mt: 1 }}>
                <Box>
                  <FormControl fullWidth size="small">
                    <InputLabel id="sort-by-label">Sort By</InputLabel>
                    <Select
                      labelId="sort-by-label"
                      value={sortBy}
                      label="Sort By"
                      onChange={(e) => setSortBy(e.target.value)}
                    >
                      <MenuItem value="captureDate">Capture Date</MenuItem>
                      <MenuItem value="uploadDate">Upload Date</MenuItem>
                      <MenuItem value="size">File Size</MenuItem>
                      <MenuItem value="name">Name</MenuItem>
                    </Select>
                  </FormControl>
                </Box>
                <Box>
                  <FormControl fullWidth size="small">
                    <InputLabel id="sort-order-label">Order</InputLabel>
                    <Select
                      labelId="sort-order-label"
                      value={sortOrder}
                      label="Order"
                      onChange={(e) => setSortOrder(e.target.value)}
                    >
                      <MenuItem value="asc">Ascending</MenuItem>
                      <MenuItem value="desc">Descending</MenuItem>
                    </Select>
                  </FormControl>
                </Box>
              </Stack>
            </Box>
            
            <Box>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 2 }}>
                <Button 
                  variant="outlined" 
                  startIcon={<RestartAltIcon />}
                  onClick={handleResetFilters}
                >
                  Reset
                </Button>
                <Button 
                  variant="contained" 
                  onClick={handleApplyFilters}
                >
                  Apply Filters
                </Button>
              </Box>
            </Box>
          </Stack>
        </Box>
      </Collapse>
    </Paper>
  );
};

export default ImageFilter;
