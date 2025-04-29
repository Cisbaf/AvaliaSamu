// context/ProjectContext.tsx
'use client';
import { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import axios from 'axios';
import {
  Project,
  GlobalCollaborator,
  ProjectCollaborator,
  Collaborator
} from '../types/project';

axios.defaults.baseURL = process.env.NEXT_PUBLIC_API_URL;

interface ProjectContextActions {
  fetchProjects(): Promise<void>;
  createProject(data: { name: string; month: string }): Promise<void>;
  updateProject(projectId: string, updates: Partial<Project>): Promise<void>;
  deleteProject(projectId: string): Promise<void>;
  selectProject(projectId: string | null): void;
  fetchGlobalCollaborators(): Promise<void>;
  createGlobalCollaborator(data: Omit<Collaborator, 'id'>): Promise<void>;
  updateGlobalCollaborator(id: string, updates: Partial<Collaborator>): Promise<void>;
  deleteGlobalCollaborator(id: string): Promise<void>;
  fetchProjectCollaborators(projectId: string): Promise<void>;
  addCollaboratorToProject(projectId: string, data: { collaboratorId: string; role: string }): Promise<void>;
  updateProjectCollaborator(projectId: string, collaboratorId: string, updates: Partial<{ role: string; points: number }>): Promise<void>;
  deleteCollaboratorFromProject(projectId: string, collaboratorId: string): Promise<void>;
  updateProject(
    projectId: string,
    updates: Partial<{
      name: string;
      month: string;
      parameters: Record<string, number>
    }>
  ): Promise<void>;
}

interface ProjectContextType {
  projects: Project[];
  globalCollaborators: GlobalCollaborator[];
  projectCollaborators: Record<string, ProjectCollaborator[]>;
  selectedProject: string | null;
  actions: ProjectContextActions;
}

const ProjectContext = createContext<ProjectContextType>({} as ProjectContextType);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [globalCollaborators, setGlobalCollaborators] = useState<GlobalCollaborator[]>([]);
  const [projectCollaborators, setProjectCollaborators] = useState<Record<string, ProjectCollaborator[]>>({});
  const [selectedProject, setSelectedProject] = useState<string | null>(null);

  // --- Projects CRUD ---
  const fetchProjects = useCallback(async () => {
    const res = await axios.get<Project[]>('/api/projetos');
    setProjects(res.data.map(p => ({
      id: p.id?.toString() || '',
      name: p.name,
      month: p.month,
      parameters: p.parameters || {}, // Garante um objeto vazio se não existir
      collaborators: [],
      createdAt: p.createdAt,
      updatedAt: p.updatedAt
    })));
  }, []);
  const createProject = useCallback(async ({ name, month }: { name: string; month: string }) => {
    await axios.post('/api/projetos', { name, month });
    await fetchProjects();
  }, [fetchProjects]);

  const updateProject = useCallback(async (projectId: string, updates: Partial<{
    name: string;
    month: string;
    parameters: Record<string, number>;
  }>) => {
    await axios.put(`/api/projetos/${projectId}`, updates);
    await fetchProjects();
  }, [fetchProjects]);

  const deleteProject = useCallback(async (projectId: string) => {
    await axios.delete(`/api/projetos/${projectId}`);
    setSelectedProject(prev => (prev === projectId ? null : prev));
    await fetchProjects();
  }, [fetchProjects]);

  // --- Global Collaborators ---
  const fetchGlobalCollaborators = useCallback(async () => {
    const res = await axios.get<Collaborator[]>('/api/collaborator');
    const mapped = res.data.map(c => ({
      id: Number(c.id),
      name: c.nome,
      role: c.role,
      points: c.pontuacao,
      cpf: c.cpf,
      idCallRote: c.idCallRote,
      isGlobal: true as const,
      function: c.role
    }));
    setGlobalCollaborators(mapped);
  }, []);

  const createGlobalCollaborator = useCallback(async (data: Omit<Collaborator, 'id'>) => {
    await axios.post('/api/collaborator', data);
    await fetchGlobalCollaborators();
  }, [fetchGlobalCollaborators]);

  const updateGlobalCollaborator = useCallback(async (id: string, updates: Partial<Collaborator>) => {
    await axios.put(`/api/collaborator/${id}`, updates);
    await fetchGlobalCollaborators();
  }, [fetchGlobalCollaborators]);

  // --- Project Collaborators ---
  const fetchProjectCollaborators = useCallback(async (projectId: string) => {
    const res = await axios.get<Collaborator[]>(`/api/projetos/${projectId}/collaborators`);
    const raw = Array.isArray(res.data) ? res.data : [];
    const mapped: ProjectCollaborator[] = raw.map(c => ({
      id: Number(c.id),
      name: c.nome,
      role: c.role,
      points: c.pontuacao,
      cpf: c.cpf,
      idCallRote: c.idCallRote,
      isGlobal: false as false,
      originalCollaboratorId: c.id.toString(),
      function: c.role
    }));
    setProjectCollaborators(prev => ({ ...prev, [projectId]: mapped }));
  }, []);

  const deleteGlobalCollaborator = useCallback(async (id: string) => {
    await axios.delete(`/api/collaborator/${id}`);
    await fetchGlobalCollaborators();
    if (selectedProject) await fetchProjectCollaborators(selectedProject);
  }, [fetchGlobalCollaborators, selectedProject, fetchProjectCollaborators]);

  const addCollaboratorToProject = useCallback(async (projectId: string, data: { collaboratorId: string; role: string }) => {
    await axios.post(`/api/projetos/${projectId}/collaborators`, null, { params: data });
    await fetchProjectCollaborators(projectId);
  }, [fetchProjectCollaborators]);

  const updateProjectCollaborator = useCallback(async (
    projectId: string,
    collaboratorId: string,
    updates: Partial<{ role: string; points: number }>
  ) => {
    await axios.put(
      `/api/projetos/${projectId}/collaborators/${collaboratorId}`,
      null,
      { params: updates }
    );
    await fetchProjectCollaborators(projectId);
  }, [fetchProjectCollaborators]);

  const deleteCollaboratorFromProject = useCallback(async (projectId: string, collaboratorId: string) => {
    await axios.delete(`/api/projetos/${projectId}/collaborators/${collaboratorId}`);
    await fetchProjectCollaborators(projectId);
  }, [fetchProjectCollaborators]);

  // --- Select Project ---
  const selectProject = useCallback((projectId: string | null) => {
    setSelectedProject(projectId);
    if (projectId) fetchProjectCollaborators(projectId);
  }, [fetchProjectCollaborators]);

  // --- Initial Data Fetch ---
  useEffect(() => {
    const initializeData = async () => {
      await fetchProjects();
      await fetchGlobalCollaborators();
    };
    initializeData();
  }, [fetchProjects, fetchGlobalCollaborators]);

  // --- Memoized Context Value ---
  const contextValue = useMemo(() => ({
    projects,
    globalCollaborators,
    projectCollaborators,
    selectedProject,
    actions: {
      fetchProjects,
      createProject,
      updateProject,
      deleteProject,
      selectProject,
      fetchGlobalCollaborators,
      createGlobalCollaborator,
      updateGlobalCollaborator,
      deleteGlobalCollaborator,
      fetchProjectCollaborators,
      addCollaboratorToProject,
      updateProjectCollaborator,
      deleteCollaboratorFromProject
    }
  }), [
    projects,
    globalCollaborators,
    projectCollaborators,
    selectedProject,
    fetchProjects,
    createProject,
    updateProject,
    deleteProject,
    selectProject,
    fetchGlobalCollaborators,
    createGlobalCollaborator,
    updateGlobalCollaborator,
    deleteGlobalCollaborator,
    fetchProjectCollaborators,
    addCollaboratorToProject,
    updateProjectCollaborator,
    deleteCollaboratorFromProject
  ]);

  return (
    <ProjectContext.Provider value={contextValue}>
      {children}
    </ProjectContext.Provider>
  );
}

export const useProjects = () => useContext(ProjectContext);