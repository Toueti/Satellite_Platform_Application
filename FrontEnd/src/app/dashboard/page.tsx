'use client'

import { useEffect, useState } from 'react'
import { dashboardService, DashboardData, Notification, Activity } from '@/services/dashboard.service'
import { Project } from '@/types/project'
import { 
  Box, Paper, Typography, CircularProgress, Alert, Container,
  Button, Card, CardContent, List, ListItem, ListItemText, Divider,
  IconButton, LinearProgress, Tooltip, Badge, Chip, Stack
} from '@mui/material'
import { styled } from '@mui/material/styles'
import Header from '@/components/Header'
import Modal from '@/components/Modal'
import Link from 'next/link'
import { createTheme, ThemeProvider } from '@mui/material/styles';

// Import icons
import AddIcon from '@mui/icons-material/Add'
import UploadIcon from '@mui/icons-material/Upload'
import NotificationsIcon from '@mui/icons-material/Notifications'
import StorageIcon from '@mui/icons-material/Storage'
import FavoriteIcon from '@mui/icons-material/Favorite'
import AccessTimeIcon from '@mui/icons-material/AccessTime'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import InfoIcon from '@mui/icons-material/Info'
import WarningIcon from '@mui/icons-material/Warning'
import ErrorIcon from '@mui/icons-material/Error'
import CloseIcon from '@mui/icons-material/Close'
import RefreshIcon from '@mui/icons-material/Refresh'
import StarIcon from '@mui/icons-material/Star'
import StarBorderIcon from '@mui/icons-material/StarBorder'

const Item = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  textAlign: 'center',
  color: theme.palette.text.secondary,
}));

const StyledNotification = styled(ListItem)<{ $type: string }>(({ theme, $type }) => ({
  borderLeft: `4px solid ${
    $type === 'success' 
      ? theme.palette.success.main 
      : $type === 'error' 
        ? theme.palette.error.main 
        : $type === 'warning' 
          ? theme.palette.warning.main 
          : theme.palette.info.main
  }`,
  borderRadius: theme.shape.borderRadius,
  marginBottom: theme.spacing(1),
  backgroundColor: theme.palette.background.paper,
  '&:hover': {
    backgroundColor: theme.palette.action.hover,
  }
}));

const ActionButton = styled(Button)(({ theme }) => ({
  padding: theme.spacing(2),
  borderRadius: theme.shape.borderRadius,
  fontWeight: 500,
  textTransform: 'none',
  fontSize: '0.95rem',
  display: 'flex', 
  flexDirection: 'column',
  gap: theme.spacing(1)
}));

const ActivityItem = styled(ListItem)(({ theme }) => ({
  padding: theme.spacing(1.5),
  borderRadius: theme.shape.borderRadius,
  marginBottom: theme.spacing(0.5),
  backgroundColor: theme.palette.background.paper,
  '&:hover': {
    backgroundColor: theme.palette.action.hover,
  }
}));

export default function DashboardPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null)
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [selectedProject, setSelectedProject] = useState<Project | null>(null)
  const [notificationsPanelOpen, setNotificationsPanelOpen] = useState(false)
  const [favoriteProjects, setFavoriteProjects] = useState<string[]>([])

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await dashboardService.getDashboardData()
        setDashboardData(data)
        setError(null)
        
        // Load favorite projects from local storage
        const savedFavorites = localStorage.getItem('favoriteProjects')
        if (savedFavorites) {
          setFavoriteProjects(JSON.parse(savedFavorites))
        }
      } catch (err) {
        setError('Failed to load dashboard data')
        console.error('Error:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  const handleDeleteProject = async () => {
    if (!selectedProject) return

    try {
      await dashboardService.deleteProject(selectedProject.id)
      // Refresh dashboard data after deletion
      const data = await dashboardService.getDashboardData()
      setDashboardData(data)
      setDeleteModalOpen(false)
      setSelectedProject(null)
    } catch (err) {
      console.error('Error deleting project:', err)
      setError('Failed to delete project')
    }
  }
  
  const handleRefreshData = async () => {
    setLoading(true)
    try {
      const data = await dashboardService.getDashboardData()
      setDashboardData(data)
      setError(null)
    } catch (err) {
      setError('Failed to refresh dashboard data')
      console.error('Error:', err)
    } finally {
      setLoading(false)
    }
  }
  
  const handleReadNotification = async (id: string) => {
    try {
      await dashboardService.markNotificationAsRead(id)
      if (dashboardData) {
        const updatedNotifications = dashboardData.notifications.map(notif => 
          notif.id === id ? { ...notif, read: true } : notif
        )
        setDashboardData({
          ...dashboardData,
          notifications: updatedNotifications
        })
      }
    } catch (err) {
      console.error('Error marking notification as read:', err)
    }
  }
  
  const handleClearAllNotifications = async () => {
    try {
      await dashboardService.clearAllNotifications()
      if (dashboardData) {
        setDashboardData({
          ...dashboardData,
          notifications: []
        })
      }
    } catch (err) {
      console.error('Error clearing notifications:', err)
    }
  }
  
  const toggleFavoriteProject = (projectId: string) => {
    let newFavorites: string[]
    
    if (favoriteProjects.includes(projectId)) {
      newFavorites = favoriteProjects.filter(id => id !== projectId)
    } else {
      newFavorites = [...favoriteProjects, projectId]
    }
    
    setFavoriteProjects(newFavorites)
    localStorage.setItem('favoriteProjects', JSON.stringify(newFavorites))
  }
  
  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'success': return <CheckCircleIcon color="success" />
      case 'error': return <ErrorIcon color="error" />
      case 'warning': return <WarningIcon color="warning" />
      default: return <InfoIcon color="info" />
    }
  }
  
  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp)
    return new Intl.DateTimeFormat('en-US', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    )
  }

  const unreadNotificationsCount = dashboardData?.notifications.filter(n => !n.read).length || 0
  const favoriteProjectsList = dashboardData?.projects.filter(p => favoriteProjects.includes(p.id)) || []
  const recentProjects = [...(dashboardData?.projects || [])]
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 5)

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Box component="main">
        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
          {dashboardData && (
            <Stack spacing={3}>
              {/* Quick Actions and Storage Usage */}
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <Paper sx={{ p: 3, borderRadius: 2, mb: 3 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6" gutterBottom color="primary">
                      Quick Actions
                    </Typography>
                    <IconButton onClick={handleRefreshData} size="small">
                      <RefreshIcon />
                    </IconButton>
                  </Box>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <Link href="/projects/new" style={{ textDecoration: 'none' }}>
                      <ActionButton 
                        variant="contained" 
                        fullWidth
                        startIcon={<AddIcon fontSize="large" />}
                        sx={{ bgcolor: 'primary.main' }}
                      >
                        <Box>New Project</Box>
                      </ActionButton>
                    </Link>
                    <Link href="/upload" style={{ textDecoration: 'none' }}>
                      <ActionButton
                        variant="contained"
                        fullWidth
                        startIcon={<UploadIcon fontSize="large" />}
                        sx={{ bgcolor: 'secondary.main' }}
                      >
                        <Box>Upload Data</Box>
                      </ActionButton>
                    </Link>
                    <ActionButton
                      variant="outlined"
                      fullWidth
                      onClick={() => setNotificationsPanelOpen(!notificationsPanelOpen)}
                      startIcon={
                        <Badge badgeContent={unreadNotificationsCount} color="error">
                          <NotificationsIcon fontSize="large" />
                        </Badge>
                      }
                    >
                      <Box>Notifications</Box>
                    </ActionButton>
                  </Stack>
                </Paper>
                
                {/* Storage Usage */}
                <Paper sx={{ p: 3, borderRadius: 2, mb: 3 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <StorageIcon color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6" color="primary">
                      Storage Usage
                    </Typography>
                  </Box>
                  <Box sx={{ mt: 2, mb: 1 }}>
                    <LinearProgress 
                      variant="determinate" 
                      value={dashboardData.storage.percentage} 
                      sx={{ 
                        height: 10, 
                        borderRadius: 5,
                        backgroundColor: '#e0e0e0',
                        '& .MuiLinearProgress-bar': {
                          backgroundColor: dashboardData.storage.percentage > 90 
                            ? 'error.main' 
                            : dashboardData.storage.percentage > 70 
                              ? 'warning.main' 
                              : 'success.main'
                        }
                      }}
                    />
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                    <Typography variant="body2" color="text.secondary">
                      {(dashboardData.storage.used / 1024).toFixed(2)} GB used
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {(dashboardData.storage.total / 1024).toFixed(2)} GB total
                    </Typography>
                  </Box>
                </Paper>
                
                {/* Recent Projects */}
                <Paper sx={{ p: 3, borderRadius: 2 }}>
                  <Typography variant="h6" gutterBottom color="primary">
                    Recent Projects
                  </Typography>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    {recentProjects.map(project => (
                      <Paper 
                        elevation={1}
                        key={`recent-${project.id}`}
                        sx={{ 
                          p: 2, 
                          borderRadius: 1, 
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          transition: 'all 0.2s',
                          '&:hover': { boxShadow: 3 }
                        }}
                      >
                        <Box>
                          <Link href={`/projects/${project.id}`} style={{ textDecoration: 'none' }}>
                            <Typography variant="subtitle1" color="primary">
                              {project.name}
                            </Typography>
                          </Link>
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <AccessTimeIcon fontSize="inherit" />
                            Updated: {new Date(project.updatedAt).toLocaleDateString()}
                          </Typography>
                        </Box>
                        <IconButton 
                          size="small"
                          onClick={() => toggleFavoriteProject(project.id)}
                        >
                          {favoriteProjects.includes(project.id) ? 
                            <StarIcon fontSize="small" color="warning" /> : 
                            <StarBorderIcon fontSize="small" />
                          }
                        </IconButton>
                      </Paper>
                    ))}
                  </Stack>
                </Paper>
              </Stack>

              {/* Notifications and Activity Feed */}
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                {/* Notifications Panel */}
                <Paper sx={{ p: 3, borderRadius: 2, mb: 3, display: notificationsPanelOpen ? 'block' : { xs: 'none', md: 'block' } }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6" gutterBottom color="primary" sx={{ display: 'flex', alignItems: 'center' }}>
                      <NotificationsIcon sx={{ mr: 1 }} /> Notifications
                      {unreadNotificationsCount > 0 && (
                        <Chip 
                          label={unreadNotificationsCount} 
                          color="error" 
                          size="small" 
                          sx={{ ml: 1, height: 20, fontSize: '0.75rem' }} 
                        />
                      )}
                    </Typography>
                    <Button 
                      size="small" 
                      color="primary" 
                      onClick={handleClearAllNotifications}
                      disabled={!dashboardData.notifications.length}
                    >
                      Clear All
                    </Button>
                  </Box>
                  {dashboardData.notifications.length === 0 ? (
                    <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                      No notifications
                    </Typography>
                  ) : (
                    <List sx={{ maxHeight: '300px', overflow: 'auto' }}>
                      {dashboardData.notifications.map((notification) => (
                        <StyledNotification 
                          key={notification.id}
                          $type={notification.type}
                          sx={{ opacity: notification.read ? 0.7 : 1 }}
                        >
                          <Box sx={{ mr: 1 }}>
                            {getNotificationIcon(notification.type)}
                          </Box>
                          <ListItemText
                            primary={notification.message}
                            secondary={formatTimestamp(notification.timestamp)}
                          />
                          {!notification.read && (
                            <IconButton 
                              edge="end" 
                              size="small"
                              onClick={() => handleReadNotification(notification.id)}
                            >
                              <CloseIcon fontSize="small" />
                            </IconButton>
                          )}
                        </StyledNotification>
                      ))}
                    </List>
                  )}
                </Paper>
                
                {/* Favorite Projects */}
                <Paper sx={{ p: 3, borderRadius: 2, mb: 3 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <FavoriteIcon color="error" sx={{ mr: 1 }} />
                    <Typography variant="h6" color="primary">
                      Favorite Projects
                    </Typography>
                  </Box>
                  {favoriteProjectsList.length === 0 ? (
                    <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
                      No favorite projects yet. Click the star icon to add favorites.
                    </Typography>
                  ) : (
                    <List>
                      {favoriteProjectsList.map(project => (
                        <ListItem 
                          key={`fav-${project.id}`}
                          sx={{ 
                            px: 2, 
                            py: 1, 
                            borderRadius: 1, 
                            mb: 1,
                            '&:hover': { bgcolor: 'action.hover' }
                          }}
                          component={Link}
                          href={`/projects/${project.id}`}
                        >
                          <ListItemText 
                            primary={project.name}
                            secondary={project.description || 'No description'}
                            primaryTypographyProps={{ color: 'primary' }}
                          />
                          <IconButton 
                            size="small"
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              toggleFavoriteProject(project.id);
                            }}
                          >
                            <StarIcon fontSize="small" color="warning" />
                          </IconButton>
                        </ListItem>
                      ))}
                    </List>
                  )}
                </Paper>
                
                {/* Activity Feed */}
                <Paper sx={{ p: 3, borderRadius: 2 }}>
                  <Typography variant="h6" gutterBottom color="primary" sx={{ display: 'flex', alignItems: 'center' }}>
                    <AccessTimeIcon sx={{ mr: 1 }} /> Recent Activity
                  </Typography>
                  {dashboardData.activities.length === 0 ? (
                    <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                      No recent activities
                    </Typography>
                  ) : (
                    <List sx={{ maxHeight: '250px', overflow: 'auto' }}>
                      {dashboardData.activities.map((activity) => (
                        <ActivityItem key={activity.id} disablePadding>
                          <ListItemText
                            primary={
                              <Typography variant="body2">
                                <strong>{activity.action}</strong> {activity.entity}
                              </Typography>
                            }
                            secondary={
                              <Typography variant="caption" color="text.secondary">
                                {formatTimestamp(activity.timestamp)} by {activity.user}
                              </Typography>
                            }
                          />
                        </ActivityItem>
                      ))}
                    </List>
                  )}
                </Paper>
              </Stack>

              {/* Stats */}
              <Paper sx={{ p: 3, mb: 4 }}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                  <Box sx={{ flex: { xs: '1 1 100%', sm: '1 1 calc(50% - 8px)', md: '1 1 calc(25% - 12px)' } }}>
                    <Box sx={{ textAlign: 'center', p: 2 }}>
                      <Typography variant="h4" color="primary.main">
                        {dashboardData.stats.totalProjects}
                      </Typography>
                      <Typography variant="subtitle1" color="text.secondary">
                        Total Projects
                      </Typography>
                    </Box>
                  </Box>
                  <Box sx={{ flex: { xs: '1 1 100%', sm: '1 1 calc(50% - 8px)', md: '1 1 calc(25% - 12px)' } }}>
                    <Box sx={{ textAlign: 'center', p: 2 }}>
                      <Typography variant="h4" color="success.main">
                        {dashboardData.stats.activeProjects}
                      </Typography>
                      <Typography variant="subtitle1" color="text.secondary">
                        Active Projects
                      </Typography>
                    </Box>
                  </Box>
                  <Box sx={{ flex: { xs: '1 1 100%', sm: '1 1 calc(50% - 8px)', md: '1 1 calc(25% - 12px)' } }}>
                    <Box sx={{ textAlign: 'center', p: 2 }}>
                      <Typography variant="h4" color="info.main">
                        {dashboardData.stats.completedProjects}
                      </Typography>
                      <Typography variant="subtitle1" color="text.secondary">
                        Completed Projects
                      </Typography>
                    </Box>
                  </Box>
                  <Box sx={{ flex: { xs: '1 1 100%', sm: '1 1 calc(50% - 8px)', md: '1 1 calc(25% - 12px)' } }}>
                    <Box sx={{ textAlign: 'center', p: 2 }}>
                      <Typography variant="h4" color="warning.main">
                        {dashboardData.stats.archivedProjects}
                      </Typography>
                      <Typography variant="subtitle1" color="text.secondary">
                        Archived Projects
                      </Typography>
                    </Box>
                  </Box>
                </Box>
              </Paper>
            </Stack>
          )}
        </Container>
      </Box>

      <Modal
        open={deleteModalOpen}
        onClose={() => {
          setDeleteModalOpen(false);
          setSelectedProject(null);
        }}
        title="Delete Project"
        content={`Are you sure you want to delete ${selectedProject?.name}? This action cannot be undone.`}
        actions={[
          {
            label: 'Cancel',
            onClick: () => {
              setDeleteModalOpen(false);
              setSelectedProject(null);
            }
          },
          {
            label: 'Delete',
            onClick: handleDeleteProject,
            color: 'error'
          }
        ]}
      />
    </Box>
  )
}
