/**
 * Centralized API Client for AutoTest AI
 */
const BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

class ApiClient {
  async request(endpoint, options = {}) {
    const token = localStorage.getItem('autotest_token');
    const headers = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...options.headers,
    };

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const config = {
      ...options,
      headers,
    };

    try {
      const response = await fetch(`${BASE_URL}${endpoint}`, config);

      if (response.status === 401) {
        localStorage.removeItem('autotest_token');
      }

      const contentType = response.headers.get('content-type');
      let data = null;
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (!response.ok) {
        const error = new Error(
          (data && data.message) || response.statusText || 'An error occurred'
        );
        error.status = response.status;
        error.data = data;
        throw error;
      }

      return data;
    } catch (err) {
      throw err;
    }
  }

  get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  async getBlob(endpoint, accept = 'image/png') {
    const token = localStorage.getItem('autotest_token');
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      headers: {
        Accept: accept,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (response.status === 401) {
      localStorage.removeItem('autotest_token');
    }

    if (!response.ok) {
      const contentType = response.headers.get('content-type');
      const data = contentType?.includes('application/json')
        ? await response.json()
        : await response.text();
      const error = new Error((data && data.message) || response.statusText || 'Failed to load file');
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return response.blob();
  }

  post(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(body),
    });
  }

  put(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(body),
    });
  }

  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
