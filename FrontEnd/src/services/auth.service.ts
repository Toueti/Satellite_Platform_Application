import { AUTH_ENDPOINTS } from '../config/api'
import { httpClient } from '../utils/api/http-client'

interface LoginCredentials {
  username: string
  password: string
}

interface RegisterData extends LoginCredentials {
  email: string // Keep email for registration
  fullName: string
}

class AuthService {
  private loginAttempts = 0;
  private readonly maxLoginAttempts = 3;
  private loginCooldownEnd = 0;
  private readonly loginCooldownDuration = 60000; // 1 minute cooldown

  async login(credentials: LoginCredentials) {
    const now = Date.now();
    
    // Check if we're in a cooldown period
    if (now < this.loginCooldownEnd) {
      const remainingSeconds = Math.ceil((this.loginCooldownEnd - now) / 1000);
      throw new Error(`Too many login attempts. Please wait ${remainingSeconds} seconds before trying again.`);
    }

    try {
      const response = await httpClient.post(AUTH_ENDPOINTS.LOGIN, credentials, {
        requiresAuth: false,
      });
      
      // Reset login attempts on successful login
      this.loginAttempts = 0;
      this.loginCooldownEnd = 0;

      // Store the JWT token - Access directly from the response object
      const accessToken = response?.accessToken; // No .data
      if (accessToken) {
        localStorage.setItem('token', accessToken);
      } else {
        // If no token, we should probably throw an error earlier, but log for now
        // throw new Error('No token received from server');
      }

      // Store additional user information (legacy - can be removed later if 'user' object works)
      if (response?.roles) { // No .data
        localStorage.setItem('userRoles', JSON.stringify(response.roles));
      }
      // Log the raw response data from the login endpoint (using the already logged raw response)
      // console.log('[AuthService Login] Response Data:', response.data); // Redundant now

      // Store user object if available - Access directly from the response object
      if (response?.id && response?.username && response?.email && response?.roles) { // No .data
          const user = {
              id: response.id,
              username: response.username,
              email: response.email,
              roles: response.roles,
              // Add other relevant fields if needed
          };
          localStorage.setItem('user', JSON.stringify(user));
      } else {
          // Clear potentially stale user data if login response is incomplete
          localStorage.removeItem('user');
      }

      return response;
    } catch (error: any) {
      // Handle rate limiting specifically
      if (error.message.includes('Rate limit')) {
        this.loginAttempts++;
        
        // If we've exceeded max attempts, enforce a cooldown
        if (this.loginAttempts >= this.maxLoginAttempts) {
          this.loginCooldownEnd = now + this.loginCooldownDuration;
          throw new Error(`Too many login attempts. Please wait 60 seconds before trying again.`);
        }
        
        throw new Error('Login temporarily unavailable. Please try again in a few seconds.');
      }
      
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
      
      const accessToken = response.data?.accessToken || response.accessToken;
      if (accessToken) {
        localStorage.setItem('token', accessToken);

        // Store additional user information if available
        if (response.data?.roles) {
          localStorage.setItem('userRoles', JSON.stringify(response.data.roles));
        }
        if (response.data?.username) {
          localStorage.setItem('username', response.data.username);
        }
        if (response.data?.email) {
          localStorage.setItem('email', response.data.email);
        }
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
    // Check if running in a browser environment
    if (typeof window !== 'undefined') {
      return localStorage.getItem('token');
    }
    return null; // Return null if not in a browser (e.g., SSR)
  }

  isAuthenticated(): boolean {
    // Check if running in a browser environment before getting token
    if (typeof window === 'undefined') {
      return false; // Cannot be authenticated on the server without a token check mechanism
    }
    const token = this.getToken();
    return !!token;
  }

  logout() {
    // Check for window existence before accessing localStorage and window
    if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        localStorage.removeItem('user'); // Remove the stored user object
        // Clear legacy items if they exist
        localStorage.removeItem('userRoles');
        localStorage.removeItem('username');
        localStorage.removeItem('email');
        window.location.href = '/auth/login';
    }
  }
}

export const authService = new AuthService()
