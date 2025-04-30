
import { useState, useCallback, useEffect } from 'react';
import {
    fetchProjectsApi,
    createProjectApi,
    updateProjectApi,
    deleteProjectApi
} from '@/lib/api';
import { Project } from '@/types/project';

export function useProjectActions() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProject, setSelectedProject] = useState<string | null>(null);

    const fetchProjects = useCallback(async () => {
        const { data } = await fetchProjectsApi();
        setProjects(data);
    }, []);

    const createProject = useCallback(
        async (data: { name: string; month: string }) => {
            await createProjectApi(data);
            await fetchProjects();
        },
        [fetchProjects]
    );

    const updateProject = useCallback(
        async (id: string, updates: any) => {
            await updateProjectApi(id, updates);
            await fetchProjects();
        },
        [fetchProjects]
    );

    const deleteProject = useCallback(
        async (id: string) => {
            await deleteProjectApi(id);
            if (selectedProject === id) setSelectedProject(null);
            await fetchProjects();
        },
        [fetchProjects, selectedProject]
    );

    useEffect(() => {
        fetchProjects();
    }, [fetchProjects]);

    return {
        projects,
        selectedProject,
        setSelectedProject,
        actions: { createProject, updateProject, deleteProject }
    };
}