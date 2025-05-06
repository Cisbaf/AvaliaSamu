import axios from 'axios';
import { GlobalCollaborator, Project, ProjectCollaborator } from '@/types/project';

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL + '/api', // ex: https://meuservidor.com/api
  headers: { 'Content-Type': 'application/json' },
});

export const fetchProjectsApi = () => api.get<Project[]>('/projetos');
export const createProjectApi = (data: { name: string; month: string }) =>
  api.post<Project>('/projetos', data);
export const updateProjectApi = (id: string, updates: any) =>
  api.put<Project>(`/projetos/${id}`, updates);
export const deleteProjectApi = (id: string) =>
  api.delete<void>(`/projetos/${id}`);

export const fetchGlobalCollaboratorsApi = () =>
  api.get<GlobalCollaborator[]>('/collaborator');
export const createGlobalCollaboratorApi = (data: Omit<GlobalCollaborator, 'id'>) =>
  api.post<GlobalCollaborator>('/collaborator', data);
export const updateGlobalCollaboratorApi = (id: string, data: Partial<GlobalCollaborator>) =>
  api.put<GlobalCollaborator>(`/collaborator/${id}`, data);
export const deleteGlobalCollaboratorApi = (id: string) =>
  api.delete<void>(`/collaborator/${id}`);

export const fetchProjectCollaboratorsApi = (projectId: string) =>
  api.get<ProjectCollaborator[]>(`/projetos/${projectId}/collaborators`);

export const addCollaboratorToProjectApi = (
  projectId: string,
  collaboratorId: string,
  role: string
) =>
  api.post<void>(
    `/projetos/${projectId}/collaborators`,
    { collaboratorId, role }
  );

export const updateProjectCollaboratorApi = (
  projectId: string,
  projectCollabId: string,
  role: string
) =>
  api.put<void>(
    `/projetos/${projectId}/collaborators/${projectCollabId}`,
    { role }
  );

export const deleteProjectCollaboratorApi = (
  projectId: string,
  projectCollabId: string
) =>
  api.delete<void>(`/projetos/${projectId}/collaborators/${projectCollabId}`);

export default api;
