import api from './axiosConfig';

export const requestRide = (data) => api.post('/api/rides/request', data);
export const getRideById = (id) => api.get(`/api/rides/${id}`);
export const getRidesByRider = (riderId) => api.get(`/api/rides/rider/${riderId}`);
export const acceptRide = (id) => api.post(`/api/rides/${id}/accept`);
export const startRide = (id) => api.post(`/api/rides/${id}/start`);
export const completeRide = (id) => api.post(`/api/rides/${id}/complete`);
export const cancelRide = (id) => api.post(`/api/rides/${id}/cancel`);
