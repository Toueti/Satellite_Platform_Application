import { AUTH_ENDPOINTS } from '../config/api';

interface LoginCredentials {
    email: string;
    password: string;
}

interface RegisterData extends LoginCredentials {
    email: string;
    fullName: string;
}

class AuthService {
    async login(credentials: LoginCredentials) {
        try {
            const response = await fetch(AUTH_ENDPOINTS.LOGIN, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(credentials),
            });

            if (!response.ok) {
                throw new Error('Login failed');
            }

            const data = await response.json();
            // Store the JWT token
            localStorage.setItem('token', data.token);
            return data;
        } catch (error) {
            throw error;
        }
    }

    async register(data: RegisterData) {
        try {
            const response = await fetch(AUTH_ENDPOINTS.REGISTER, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            });

            if (!response.ok) {
                throw new Error('Registration failed');
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    async resetPassword(email: string) {
        try {
            const response = await fetch(AUTH_ENDPOINTS.RESET_PASSWORD, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email }),
            });

            if (!response.ok) {
                throw new Error('Password reset request failed');
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    logout() {
        localStorage.removeItem('token');
    }

    getToken() {
        return localStorage.getItem('token');
    }

    isAuthenticated() {
        return !!this.getToken();
    }
}

export const authService = new AuthService();
