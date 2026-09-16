import api from './axiosConfig';

export const getNotificationsByUser = (userId) => api.get(`/api/v1/notifications/user/${userId}`);
export const getNotificationById = (id) => api.get(`/api/v1/notifications/${id}`);
export const getAllNotifications = () => api.get('/api/v1/notifications');
