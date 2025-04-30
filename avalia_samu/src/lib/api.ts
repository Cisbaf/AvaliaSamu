import { GlobalCollaborator, Project, ProjectCollaborator } from '@/types/project';
import axios from 'axios';

// Configura uma instância do Axios com o baseURL correto
export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL + '/api',
});

// Projetos
export const fetchProjectsApi = () =>
  api.get<Project[]>('/projetos');

export const createProjectApi = (data: { name: string; month: string }) =>
  api.post<Project>('/projetos', data);

export const updateProjectApi = (id: string, updates: any) =>
  api.put<Project>(`/projetos/${id}`, updates);

export const deleteProjectApi = (id: string) =>
  api.delete<void>(`/projetos/${id}`);

// Colaboradores globais
export const fetchGlobalCollaboratorsApi = () =>
  api.get<GlobalCollaborator[]>('/collaborator');

export const createGlobalCollaboratorApi = (data: Omit<GlobalCollaborator, 'id'>) =>
  api.post<GlobalCollaborator>('/collaborator', data);

export const updateGlobalCollaboratorApi = (id: string, data: Partial<GlobalCollaborator>) =>
  api.put<GlobalCollaborator>(`/collaborator/${id}`, data);

export const deleteGlobalCollaboratorApi = (id: string) =>
  api.delete<void>(`/collaborator/${id}`);

// Colaboradores de projeto
export const fetchProjectCollaboratorsApi = (projectId: string) =>
  api.get<ProjectCollaborator[]>(`/projetos/${projectId}/collaborators`);



export const addCollaboratorToProjectApi = (
  projectId: string,
  collaboratorId: string | number,
  role: string
) =>
  api.post<ProjectCollaborator[]>(
    `/projetos/${projectId}/collaborators`,
    null,
    { params: { collaboratorId, role } }
  );

export const updateProjectCollaboratorApi = (
  projectId: string,
  collabId: string | number,
  role: string
) =>
  api.put<ProjectCollaborator[]>(
    `/projetos/${projectId}/collaborators/${collabId}`,
    null,
    { params: { role } }
  );

export const deleteProjectCollaboratorApi = (
  projectId: string,
  collabId: string | number
) =>
  api.delete<void>(
    `/projetos/${projectId}/collaborators/${collabId}`
  );

export default api;