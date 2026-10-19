import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiClient } from '../../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('autotest_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [loading, setLoading] = useState(false);

  const login = async (username, password) => {
    setLoading(true);
    try {
      const data = await apiClient.post('/auth/login', { username, password });
      localStorage.setItem('autotest_token', data.token);
      const userData = {
        id: data.userId,
        username: data.username,
        email: data.email,
        role: data.role,
      };
      localStorage.setItem('autotest_user', JSON.stringify(userData));
      setUser(userData);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const register = async (username, email, password) => {
    setLoading(true);
    try {
      const data = await apiClient.post('/auth/register', { username, email, password });
      localStorage.setItem('autotest_token', data.token);
      const userData = {
        id: data.userId,
        username: data.username,
        email: data.email,
        role: data.role,
      };
      localStorage.setItem('autotest_user', JSON.stringify(userData));
      setUser(userData);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('autotest_token');
    localStorage.removeItem('autotest_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
