import { api } from '@/utils/api';
import { Project } from '@/types/project';

export interface DashboardData {
  projects: Project[];
  stats: {
    totalProjects: number;
    activeProjects: number;
    completedProjects: number;
    archivedProjects: number;
  };
  storage: {
    used: number; // in MB
    total: number; // in MB
    percentage: number;
  };
  notifications: Notification[];
  activities: Activity[];
}

export interface Notification {
  id: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  timestamp: string;
  read: boolean;
}

export interface Activity {
  id: string;
  action: string;
  entity: string;
  entityId: string;
  timestamp: string;
  user: string;
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
}

class DashboardService {
  private readonly baseUrl = '/thematician/projects';

  async getDashboardData(): Promise<DashboardData> {
    try {
      // Get statistics
      const statsResponse = await api.get<ApiResponse<{
        totalProjects: number;
        activeProjects: number;
        completedProjects: number;
        archivedProjects: number;
      }>>(`${this.baseUrl}/statistics`);

      // Get all projects
      const projectsResponse = await api.get<ApiResponse<Project[]>>(`${this.baseUrl}/all`);
      
      // Get storage usage
      const storageResponse = await api.get<ApiResponse<{
        used: number;
        total: number;
        percentage: number;
      }>>(`${this.baseUrl}/storage`);
      
      // Get notifications
      const notificationsResponse = await api.get<ApiResponse<Notification[]>>(`${this.baseUrl}/notifications`);
      
      // Get activities
      const activitiesResponse = await api.get<ApiResponse<Activity[]>>(`${this.baseUrl}/activities`);

      return {
        projects: projectsResponse.data,
        stats: statsResponse.data,
        storage: storageResponse.data,
        notifications: notificationsResponse.data || [],
        activities: activitiesResponse.data || []
      };
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      // Return partial data if some requests fail
      return {
        projects: [],
        stats: {
          totalProjects: 0,
          activeProjects: 0,
          completedProjects: 0,
          archivedProjects: 0
        },
        storage: {
          used: 0,
          total: 1000,
          percentage: 0
        },
        notifications: [],
        activities: []
      };
    }
  }

  async deleteProject(projectId: string): Promise<void> {
    try {
      await api.delete(`${this.baseUrl}/${projectId}`);
    } catch (error) {
      console.error('Error deleting project:', error);
      throw error;
    }
  }

  async markNotificationAsRead(notificationId: string): Promise<void> {
    try {
      await api.put(`${this.baseUrl}/notifications/${notificationId}/read`, {});
    } catch (error) {
      console.error('Error marking notification as read:', error);
      throw error;
    }
  }

  async clearAllNotifications(): Promise<void> {
    try {
      await api.delete(`${this.baseUrl}/notifications/clear`);
    } catch (error) {
      console.error('Error clearing notifications:', error);
      throw error;
    }
  }
}

export const dashboardService = new DashboardService();
