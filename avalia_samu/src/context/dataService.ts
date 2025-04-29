// services/dataService.ts
import { Project, GlobalCollaborator } from '@/types/project'; // Adjust the path to where the Project type is defined
interface ApiClient {
  get: <T>(key: string) => Promise<T | null>;
  set: <T>(key: string, value: T) => Promise<void>;
  delete: (key: string) => Promise<void>;
}

class LocalStorageClient implements ApiClient {
  async get<T>(key: string): Promise<T | null> {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
  }

  async set<T>(key: string, value: T): Promise<void> {
    localStorage.setItem(key, JSON.stringify(value));
  }

  async delete(key: string): Promise<void> {
    localStorage.removeItem(key);
  }
}

const client = new LocalStorageClient();

export const dataService = {
  projects: {
    getAll: async (): Promise<Project[]> => {
      const data = await client.get<Project[]>('projects') || [];
      return data.filter(p => p && p.id);
    },

    save: async (project: Project) => {
      const projects = await client.get<Project[]>('projects') || [];
      const index = projects.findIndex(p => p.id === project.id);
      
      if (index >= 0) {
        projects[index] = project;
      } else {
        projects.push(project);
      }
      
      await client.set('projects', projects);
    },

    saveAll: async (projects: Project[]) => {
      await client.set('projects', projects);
    },

    delete: async (projectId: string) => {
      const projects = await client.get<Project[]>('projects') || [];
      const filtered = projects.filter(p => p.id !== projectId);
      await client.set('projects', filtered);
    }
  },

  collaborators: {
    getGlobal: async (): Promise<GlobalCollaborator[]> => {
      const data = await client.get<GlobalCollaborator[]>('globalCollaborators') || [];
      
      return data.map(c => ({
        id: typeof c.id === 'number' ? c.id : parseInt(crypto.randomUUID(), 10),
        name: c.name || 'Nome não definido',
        function: c.function || 'Função não definida',
        points: Number(c.points) || 0,
        isGlobal: true,
        createdAt: c.createdAt ? new Date(c.createdAt) : new Date(),
        updatedAt: c.updatedAt ? new Date(c.updatedAt) : new Date(),
        cpf: c.cpf || 'CPF não definido',
        idCallRote: c.idCallRote || 'ID não definido'
      }));
    },
    
    saveGlobal: async (collaborators: GlobalCollaborator[]) => {
      const validData = collaborators.filter(c => c.id && c.name);
      await client.set('globalCollaborators', validData);
    }
  }
};