import React from 'react';
import { Paper, Tabs, Tab, Box } from '@mui/material';

interface ProjectTabsProps {
    activeTab: number;
    onTabChange: (event: React.SyntheticEvent, newValue: number) => void;
}

// Helper function for accessibility props
function a11yProps(index: number) {
    return {
        id: `project-tab-${index}`,
        'aria-controls': `project-tabpanel-${index}`,
    };
}

const ProjectTabs: React.FC<ProjectTabsProps> = ({ activeTab, onTabChange }) => {
    return (
        <Paper sx={{ width: '100%', mb: 4, borderBottom: 1, borderColor: 'divider' }}>
            <Tabs
                value={activeTab}
                onChange={onTabChange}
                indicatorColor="primary"
                textColor="primary"
                variant="fullWidth"
                aria-label="Project details tabs"
            >
                <Tab label="Project Details" {...a11yProps(0)} />
                <Tab label="Images" {...a11yProps(1)} />
                <Tab label="Analysis Results" {...a11yProps(2)} />
            </Tabs>
        </Paper>
    );
};

// Optional: Define a TabPanel component for consistent usage
interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

export const TabPanel: React.FC<TabPanelProps> = (props) => {
    const { children, value, index, ...other } = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`project-tabpanel-${index}`}
            aria-labelledby={`project-tab-${index}`}
            {...other}
        >
            {value === index && (
                <Box sx={{ pt: 3 }}> {/* Add padding top for content separation */}
                    {children}
                </Box>
            )}
        </div>
    );
};


export default ProjectTabs;
