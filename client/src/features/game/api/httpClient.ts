import axios from 'axios';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/flip',
  timeout: 15000,
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;

    if (status === 502 || status === 503 || status === 504 || error?.code === 'ERR_NETWORK') {
      return Promise.reject(new Error('No se pudo conectar al backend. Verifica que el servidor esté corriendo en http://localhost:8080 y vuelve a intentar.'));
    }

    if (error?.response?.data?.message) {
      return Promise.reject(new Error(String(error.response.data.message)));
    }

    return Promise.reject(error);
  },
);
