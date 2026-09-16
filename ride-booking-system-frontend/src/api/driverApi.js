import api from './axiosConfig';

export const getDriverById = (id) => api.get(`/api/v1/drivers/${id}`);
export const getAllDrivers = () => api.get('/api/v1/drivers');
export const createDriver = (data) => api.post('/api/v1/drivers', data);
export const updateDriver = (id, data) => api.put(`/api/v1/drivers/${id}`, data);
export const updateAvailability = (id, status) =>
  api.patch(`/api/v1/drivers/${id}/availability`, null, { params: { status } });
export const updateDriverLocation = (id, data) =>
  api.put(`/api/v1/drivers/${id}`, data);
