import { useState, useCallback } from 'react'
import {
    fetchProjectCollaboratorsApi,
    deleteProjectCollaboratorApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi
} from '@/lib/api'
import { ProjectCollaborator } from '@/types/project'

export function useProjectCollaborators() {
    const [projectCollaborators, setProjectCollaborators] = useState<Record<string, ProjectCollaborator[]>>({});


    const fetchProjectCollaborators = useCallback(async (projectId: string) => {
        try {
            const res = await fetchProjectCollaboratorsApi(projectId);
            setProjectCollaborators(prev => ({
                ...prev,
                [projectId]: res.data
            }));
        } catch (err) {
            console.error('Erro ao buscar colaboradores do projeto:', err);
        }
    }, []);

    const addCollaboratorToProject = useCallback(async (projectId: string, { id, role }: { id: string; role: string }) => {
        await addCollaboratorToProjectApi(projectId, id, role);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    const updateProjectCollaborator = useCallback(async (projectId: string, collabId: string, { role }: { role: string }) => {
        await updateProjectCollaboratorApi(projectId, collabId, role);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    const deleteCollaboratorFromProject = useCallback(async (projectId: string, collabId: string) => {
        await deleteProjectCollaboratorApi(projectId, collabId);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    return {
        projectCollaborators,
        actions: {
            fetchProjectCollaborators,
            addCollaboratorToProject,
            updateProjectCollaborator,
            deleteCollaboratorFromProject
        }
    }
}
