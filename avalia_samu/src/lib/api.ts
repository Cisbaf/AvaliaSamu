import { GlobalCollaborator, Project, ProjectCollaborator } from '@/types/project';
import axios from 'axios';

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL + '/api',
});
axios.defaults.baseURL = process.env.NEXT_PUBLIC_API_URL;

// Projetos
export const fetchProjectsApi = () => axios.get<Project[]>('/api/projetos');
export const createProjectApi = (data: { name: string; month: string }) =>
  axios.post('/api/projetos', data);
export const updateProjectApi = (id: string, updates: any) =>
  axios.put(`/api/projetos/${id}`, updates);
export const deleteProjectApi = (id: string) =>
  axios.delete(`/api/projetos/${id}`);

// Colaboradores globais
export const fetchGlobalCollaboratorsApi = () => axios.get<GlobalCollaborator[]>('/api/collaborator');

export const createGlobalCollaboratorApi = (data: any) =>
  axios.post('/api/collaborator', data);
export const updateGlobalCollaboratorApi = (id: string, data: any) =>
  axios.put(`/api/collaborator/${id}`, data);
export const deleteGlobalCollaboratorApi = (id: string) =>
  axios.delete(`/api/collaborator/${id}`);

// Colaboradores de projeto
export const fetchProjectCollaboratorsApi = (projectId: string) =>
  axios.get<ProjectCollaborator[]>(`/api/projetos/${projectId}/collaborators`);
export const addCollaboratorToProjectApi = (
  projectId: string,
  params: any
) =>
  axios.post(
    `/api/projetos/${projectId}/collaborators`,
    null,
    { params }
  );
export const updateProjectCollaboratorApi = (
  projectId: string,
  collabId: string,
  params: any
) =>
  axios.put(
    `/api/projetos/${projectId}/collaborators/${collabId}`,
    null,
    { params }
  );
export const deleteProjectCollaboratorApi = (
  projectId: string,
  collabId: string
) =>
  axios.delete(
    `/api/projetos/${projectId}/collaborators/${collabId}`
  );

export default api;

