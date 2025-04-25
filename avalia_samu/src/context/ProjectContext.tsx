// context/ProjectContext.tsx
'use client';
import { createContext, useContext, useState, useEffect } from 'react';
import { dataService } from './dataService';
import { Project, GlobalCollaborator, ProjectCollaborator } from '../types/project';

interface ProjectContextActions {
  createProject: (project: Omit<Project, '_id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateProject: (projectId: string, updates: Partial<Project>) => Promise<void>;
  deleteProject: (projectId: string) => Promise<void>;
  selectProject: (projectId: string | null) => void;

  createGlobalCollaborator: (collaborator: Omit<GlobalCollaborator, '_id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updateGlobalCollaborator: (collaboratorId: string, updates: Partial<GlobalCollaborator>) => Promise<void>;

  addCollaboratorToProject: (
    projectId: string,
    collaborator: Omit<ProjectCollaborator, '_id' | 'createdAt' | 'updatedAt'> | { globalCollaboratorId: string }
  ) => Promise<void>;

  deleteGlobalCollaborator: (collaboratorId: string) => Promise<void>;
  deleteCollaboratorFromProject: (projectId: string, collaboratorId: string) => Promise<void>;
  updateProjectCollaborator: (
    projectId: string,
    collaboratorId: string,
    updates: Partial<ProjectCollaborator>
  ) => Promise<void>;
}

interface ProjectContextType {
  projects: Project[];
  globalCollaborators: GlobalCollaborator[];
  selectedProject: string | null;
  actions: ProjectContextActions;
}

const ProjectContext = createContext<ProjectContextType>({} as ProjectContextType);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [globalCollaborators, setGlobalCollaborators] = useState<GlobalCollaborator[]>([]);
  const [selectedProject, setSelectedProject] = useState<string | null>(null);

  const loadData = async () => {
    try {
      const [loadedProjects, loadedCollaborators] = await Promise.all([
        dataService.projects.getAll(),
        dataService.collaborators.getGlobal()
      ]);

      // Filtrar dados corrompidos
      const validCollaborators = loadedCollaborators.filter(c =>
        c && c._id && c.name && c.function
      );

      setProjects(loadedProjects.filter(p => p && p._id));
      setGlobalCollaborators(validCollaborators);
    } catch (error) {
      console.error('Erro ao carregar dados:', error);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const value: ProjectContextType = {
    projects,
    globalCollaborators,
    selectedProject,
    actions: {
      createProject: async (newProject) => {
        const project: Project = {
          ...newProject,
          _id: Date.now().toString(),
          createdAt: new Date(),
          updatedAt: new Date(),
          collaborators: []
        };

        await dataService.projects.save(project);
        await loadData();
      },

      updateProject: async (projectId, updates) => {
        const project = projects.find(p => p._id === projectId);
        if (!project) return;

        const updatedProject = {
          ...project,
          ...updates,
          updatedAt: new Date()
        };

        await dataService.projects.save(updatedProject);
        await loadData();
      },

      deleteProject: async (projectId) => {
        await dataService.projects.delete(projectId);
        await loadData();
        if (selectedProject === projectId) setSelectedProject(null);
      },

      selectProject: setSelectedProject,

      createGlobalCollaborator: async (collaborator) => {
        try {
          const newCollaborator: GlobalCollaborator = {
            ...collaborator,
            _id: crypto.randomUUID(),
            createdAt: new Date(),
            updatedAt: new Date(),
            isGlobal: true
          };

          if (!newCollaborator._id || !newCollaborator.name) {
            throw new Error('Dados inválidos para novo colaborador');
          }

          // Correção: Salvar toda a lista de uma vez
          const updatedCollaborators = [...globalCollaborators, newCollaborator];
          await dataService.collaborators.saveGlobal(updatedCollaborators);

          // Atualizar estado local imediatamente
          setGlobalCollaborators(updatedCollaborators);

        } catch (error) {
          console.error('Erro ao criar colaborador:', error);
          throw error;
        }
      },
      updateGlobalCollaborator: async (collaboratorId, updates) => {
        const updatedCollaborators = globalCollaborators.map(c =>
          c._id === collaboratorId
            ? { ...c, ...updates, updatedAt: new Date() }
            : c
        );

        await dataService.collaborators.saveGlobal(updatedCollaborators);

        setGlobalCollaborators(updatedCollaborators);

      },

      deleteGlobalCollaborator: async (collaboratorId: string) => {
        try {
          const updatedCollaborators = globalCollaborators.filter(c => c._id !== collaboratorId);
          await dataService.collaborators.saveGlobal(updatedCollaborators);
          setGlobalCollaborators(updatedCollaborators);

          const updatedProjects = projects.map(project => ({
            ...project,
            collaborators: project.collaborators.filter(c => c.originalCollaboratorId !== collaboratorId),
            updatedAt: new Date()
          }));

          await dataService.projects.saveAll(updatedProjects);
          setProjects(updatedProjects);

        } catch (error) {
          console.error('Erro ao excluir colaborador global:', error);
          throw error;
        }
      },


      addCollaboratorToProject: async (projectId, collaborator) => {
        try {
          const project = projects.find(p => p._id === projectId);
          if (!project) throw new Error('Projeto não encontrado');

          let newCollaborator: ProjectCollaborator;

          if ('globalCollaboratorId' in collaborator) {
            const globalCollaborator = globalCollaborators.find(c => c._id === collaborator.globalCollaboratorId);
            if (!globalCollaborator) throw new Error('Colaborador global não encontrado');

            newCollaborator = {
              ...globalCollaborator,
              originalCollaboratorId: globalCollaborator._id,
              isGlobal: false,
              _id: crypto.randomUUID(), // Usar UUID aqui
              createdAt: new Date(),
              updatedAt: new Date()
            };
          } else {
            newCollaborator = {
              ...collaborator,
              isGlobal: false,
              _id: crypto.randomUUID(), // Usar UUID aqui
              createdAt: new Date(),
              updatedAt: new Date()
            };
          }

          const updatedProject = {
            ...project,
            collaborators: [...project.collaborators, newCollaborator],
            updatedAt: new Date()
          };

          // Atualização otimizada
          const newProjects = projects.map(p => p._id === projectId ? updatedProject : p);
          await dataService.projects.save(updatedProject);
          setProjects(newProjects);

        } catch (error) {
          console.error('Erro ao adicionar colaborador:', error);
          throw error;
        }
      },
      updateProjectCollaborator: async (projectId, collaboratorId, updates) => {
        const project = projects.find(p => p._id === projectId);
        if (!project) return;

        const updatedProject = {
          ...project,
          collaborators: project.collaborators.map(c =>
            c._id === collaboratorId ? { ...c, ...updates, updatedAt: new Date() } : c
          ),
          updatedAt: new Date()
        };

        await dataService.projects.save(updatedProject);
        await loadData();
      },

      deleteCollaboratorFromProject: async (projectId, collaboratorId) => {
        const project = projects.find(p => p._id === projectId);
        if (!project) return;

        const updatedProject = {
          ...project,
          collaborators: project.collaborators.filter(c => c._id !== collaboratorId),
          updatedAt: new Date()
        };

        await dataService.projects.save(updatedProject);
        await loadData();
      }
    }
  };

  return (
    <ProjectContext.Provider value={value} >
      {children}
    </ProjectContext.Provider >
  );
}

export const useProjects = () => useContext(ProjectContext);