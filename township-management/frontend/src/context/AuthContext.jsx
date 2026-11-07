import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi, getAuthToken, setAuthToken, removeAuthToken } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => getAuthToken());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Validate session on mount or token change
  useEffect(() => {
    let isMounted = true;

    const verifySession = async () => {
      const storedToken = getAuthToken();
      if (!storedToken) {
        if (isMounted) {
          setUser(null);
          setLoading(false);
        }
        return;
      }

      try {
        const userData = await authApi.getMe();
        if (isMounted) {
          setUser(userData);
          setToken(storedToken);
          setError(null);
        }
      } catch (err) {
        // If token expired or invalid, clear auth state
        if (isMounted) {
          removeAuthToken();
          setToken(null);
          setUser(null);
          if (err.status !== 401) {
            console.warn('Session verification notice:', err.message);
          }
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    verifySession();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = useCallback(async (identifier, password) => {
    setError(null);
    try {
      const authRes = await authApi.login(identifier, password);
      setAuthToken(authRes.token);
      setToken(authRes.token);
      setUser(authRes.user);
      return authRes.user;
    } catch (err) {
      const message = err.message || 'Login failed. Please verify your credentials.';
      setError(message);
      throw err;
    }
  }, []);

  const register = useCallback(async (registerData) => {
    setError(null);
    try {
      const authRes = await authApi.register(registerData);
      setAuthToken(authRes.token);
      setToken(authRes.token);
      setUser(authRes.user);
      return authRes.user;
    } catch (err) {
      const message = err.message || 'Registration failed. Please try again.';
      setError(message);
      throw err;
    }
  }, []);

  const logout = useCallback(() => {
    removeAuthToken();
    setToken(null);
    setUser(null);
    setError(null);
  }, []);

  const value = {
    user,
    token,
    loading,
    error,
    isAuthenticated: Boolean(token && user),
    isCommittee: user?.role === 'COMMITTEE',
    canAccessCommittee: user?.role === 'COMMITTEE' || user?.role === 'HELP_DESK',
    isHelpDesk: user?.role === 'HELP_DESK',
    isGuard: user?.role === 'GUARD',
    isResident: user?.role === 'RESIDENT',
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

