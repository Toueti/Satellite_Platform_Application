// Type for Admin Signup Request based on usage in users/page.tsx
export interface AdminSignupRequest {
  id: string;
  username: string;
  email: string;
  // Add other relevant fields if known from the backend API, e.g., requestDate
}

// Type for Admin Dashboard Summary Data
export interface DashboardSummary {
  totalUsers: number;
  totalProjects: number;
  pendingSignups: number;
  // Add other summary stats as needed (e.g., activeUsers, completedProjects)
}

// Add other admin-related types here as needed
