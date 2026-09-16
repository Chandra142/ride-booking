import api from './axiosConfig';

export const processPayment = (data) => api.post('/api/v1/payments/process', data);
export const getPaymentById = (id) => api.get(`/api/v1/payments/${id}`);
export const getPaymentsByUser = (userId) => api.get(`/api/v1/payments/user/${userId}`);
export const getPaymentsByRide = (rideId) => api.get(`/api/v1/payments/ride/${rideId}`);
