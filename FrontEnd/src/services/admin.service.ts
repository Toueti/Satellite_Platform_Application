import { httpClient } from '@/utils/api/http-client';
import { User } from '@/types/user';
import { Authority } from '@/types/auth';
import { ManageablePropertyDto, UpdatePropertyRequestDto } from '@/types/config';
import { AdminSignupRequest, DashboardSummary } from '@/types/admin'; // Import DashboardSummary

const BASE_URL = '/api/admin';

// --- User Management ---

export const getAllUsers = async (): Promise<User[]> => {
  const response = await httpClient.get(`${BASE_URL}/users`);
  // Assuming the backend returns the user list directly or within a 'data' field
  return response.data || response;
};

export const createUser = async (userData: { username: string; email: string; password: string; roles: string[] }): Promise<User> => {
  // Backend expects form data (RequestParam), but let's try sending JSON first.
  // If this fails, we might need to adjust httpClient or the backend.
  // A cleaner approach would be a DTO on the backend accepting JSON.
  // For now, constructing query params as per backend controller signature.
  const params = new URLSearchParams({
    username: userData.username,
    email: userData.email,
    password: userData.password,
    // roles needs special handling if it's a Set or List in query params
  });
  userData.roles.forEach(role => params.append('roles', role));

  const response = await httpClient.post(`${BASE_URL}/users?${params.toString()}`, {}); // Body might be empty if using RequestParam
  return response.data || response;
};

export const updateUser = async (userId: string, userData: { username: string; email: string; roles: string[] }): Promise<User> => {
  const params = new URLSearchParams({
    username: userData.username,
    email: userData.email,
  });
  userData.roles.forEach(role => params.append('roles', role));

  const response = await httpClient.put(`${BASE_URL}/users/${userId}?${params.toString()}`, {}); // Body might be empty
  return response.data || response;
};

export const deleteUser = async (userId: string): Promise<any> => {
  const response = await httpClient.delete(`${BASE_URL}/users/${userId}`);
  return response.data || response;
};

export const lockUnlockUser = async (userId: string, lock: boolean): Promise<any> => {
  const response = await httpClient.post(`${BASE_URL}/users/${userId}/${lock}`, {});
  return response.data || response;
};

export const resetUserPassword = async (userId: string, newPassword: string): Promise<any> => {
  const params = new URLSearchParams({ newPassword });
  const response = await httpClient.post(`${BASE_URL}/users/${userId}/reset-password?${params.toString()}`, {});
  return response.data || response;
};

// --- Admin Signup Requests ---

export const getPendingAdminRequests = async (): Promise<AdminSignupRequest[]> => {
  const response = await httpClient.get(`${BASE_URL}/signup-requests/pending`);
  return response.data || response;
};

export const approveAdminRequest = async (requestId: string): Promise<User> => {
  const response = await httpClient.post(`${BASE_URL}/signup-requests/${requestId}/approve`, {});
  return response.data || response;
};

export const rejectAdminRequest = async (requestId: string): Promise<any> => {
  const response = await httpClient.post(`${BASE_URL}/signup-requests/${requestId}/reject`, {});
  return response.data || response;
};

// --- Role Management ---

export const getAllRoles = async (): Promise<Authority[]> => {
  const response = await httpClient.get(`${BASE_URL}/roles`);
  // Assuming response structure { status: '...', message: '...', data: [...] }
  return response.data || response;
};

export const createRole = async (roleName: string): Promise<Authority> => {
  const response = await httpClient.post(`${BASE_URL}/roles`, { roleName });
  return response.data || response;
};

export const deleteRole = async (roleName: string): Promise<any> => {
  const response = await httpClient.delete(`${BASE_URL}/roles/${roleName}`);
  return response.data || response;
};

// --- Configuration Management ---

export const getManageableProperties = async (): Promise<ManageablePropertyDto[]> => {
  const response = await httpClient.get(`${BASE_URL}/config/manageable`);
  return response.data || response;
};

export const updateManageableProperty = async (updateRequest: UpdatePropertyRequestDto): Promise<any> => {
  const response = await httpClient.put(`${BASE_URL}/config/manageable`, updateRequest);
  return response.data || response;
};

// Note: Reset is handled by sending null value in updateManageableProperty

// --- Dashboard ---

export const getDashboardSummary = async (): Promise<DashboardSummary> => {
  const response = await httpClient.get(`${BASE_URL}/dashboard/summary`);
  // Assuming the backend returns the summary data directly or within a 'data' field
  return response.data || response;
};

export const adminService = {
  getAllUsers,
  createUser,
  updateUser,
  deleteUser,
  lockUnlockUser,
  resetUserPassword,
  getPendingAdminRequests,
  approveAdminRequest,
  rejectAdminRequest,
  getAllRoles,
  createRole,
  deleteRole,
  getManageableProperties,
  updateManageableProperty,
  getDashboardSummary, // Add the new function here
};
