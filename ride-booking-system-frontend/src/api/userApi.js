import api from './axiosConfig';

export const getUserById = (id) => api.get(`/api/v1/users/${id}`);
export const getAllUsers = () => api.get('/api/v1/users');
export const updateUser = (id, data) => api.put(`/api/v1/users/${id}`, data);
export const deleteUser = (id) => api.delete(`/api/v1/users/${id}`);
