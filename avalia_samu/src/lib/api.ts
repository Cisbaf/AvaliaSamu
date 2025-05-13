import axios from 'axios';
import { GlobalCollaborator, Project, ProjectCollaborator, NestedScoringParameters, UpdateProjectCollabDto } from '@/types/project';

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL + '/api',
  headers: { 'Content-Type': 'application/json' },
});

export const fetchProjectsApi = () => api.get<Project[]>('/projetos');
export const createProjectApi = (data: { name: string; month: string; parameters: NestedScoringParameters }) =>
  api.post<Project>('/projetos', data);

export const updateProjectApi = (id: string, updates: { name?: string; month?: string; parameters?: NestedScoringParameters }) =>
  api.put<Project>(`/projetos/${id}`, updates);
export const deleteProjectApi = (id: string) =>
  api.delete<void>(`/projetos/${id}`);

export const fetchGlobalCollaboratorsApi = () =>
  api.get<GlobalCollaborator[]>('/collaborator');
export async function createGlobalCollaboratorApi(
  data: Omit<GlobalCollaborator, 'id'>
): Promise<GlobalCollaborator> {
  const resp = await api.post<GlobalCollaborator>('/collaborator', data);
  return resp.data;
}
export const updateGlobalCollaboratorApi = (id: string, data: Partial<GlobalCollaborator>) =>
  api.put<GlobalCollaborator>(`/collaborator/${id}`, data);
export const deleteGlobalCollaboratorApi = (id: string) =>
  api.delete<void>(`/collaborator/${id}`);

export const fetchProjectCollaboratorsApi = (projectId: string) =>
  api.get<ProjectCollaborator[]>(`/projetos/${projectId}/collaborator`);

export const addCollaboratorToProjectApi = (
  projectId: string,
  collaboratorId: string,
  role: string,
  durationSeconds?: number,
  quantity?: number,
  pausaMensalSeconds?: number,
  parametros?: Record<string, number>
) =>
  api.post<void>(
    `/projetos/${projectId}/collaborator`,
    { collaboratorId, role, durationSeconds, quantity, pausaMensalSeconds, parametros } // Include all parameters
  );



export const updateProjectCollaboratorApi = (
  projectId: string,
  collaboratorId: string,
  dto: UpdateProjectCollabDto
) =>
  api.put<void>(
    `/projetos/${projectId}/collaborator/${collaboratorId}`,
    dto
  );
export const deleteProjectCollaboratorApi = (
  projectId: string,
  projectCollabId: string
) =>
  api.delete<void>(`/projetos/${projectId}/collaborator/${projectCollabId}`);

export default api;
