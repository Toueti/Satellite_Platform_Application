import { AUTH_ENDPOINTS } from '../config/api'
import { httpClient } from '../utils/http-client'

interface LoginCredentials {
  username: string
  password: string
}

interface RegisterData extends LoginCredentials {
  email: string // Keep email for registration
  fullName: string
}

class AuthService {
  async login(credentials: LoginCredentials) {
    try {
      const response = await httpClient.post(AUTH_ENDPOINTS.LOGIN, credentials, {
        requiresAuth: false,
      })
      
      // Store the JWT token from the response data structure
      if (response.data?.token) {
        localStorage.setItem('token', response.data.token);
      } else if (response.token) {
        localStorage.setItem('token', response.token);
      } else {
        throw new Error('No token received from server');
      }
      
      return response;
    } catch (error: any) {
      console.error('Login error:', error);
      throw new Error(error.message || 'Login failed');
    }
  }

  async register(email: string, password: string): Promise<void> {
    try {
      const response = await httpClient.post(AUTH_ENDPOINTS.REGISTER, {
        email,
        password,
        role: 'THEMATICIAN'  // Explicitly request THEMATICIAN role
      }, { requiresAuth: false });
      
      if (response.data?.token) {
        localStorage.setItem('token', response.data.token);
      }
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  }

  async resetPassword(email: string) {
    try {
      const response = await httpClient.post(
        AUTH_ENDPOINTS.RESET_PASSWORD,
        { email },
        { requiresAuth: false }
      )
      return response
    } catch (error: any) {
      throw new Error(error.message || 'Password reset request failed')
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token;
  }

  logout() {
    localStorage.removeItem('token');
    window.location.href = '/auth/login';
  }
}

export const authService = new AuthService()
