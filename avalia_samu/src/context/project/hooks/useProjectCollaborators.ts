import { useState, useCallback } from 'react';
import {
    fetchProjectCollaboratorsApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi,
    deleteProjectCollaboratorApi
} from '@/lib/api';
import { ProjectCollaborator } from '@/types/project';

export function useProjectCollaborators() {
    const [projectCollaborators, setProjectCollaborators] =
        useState<Record<string, ProjectCollaborator[]>>({});

    const fetchProjectCollaborators = useCallback(
        async (projectId: string) => {
            const { data }: { data: ProjectCollaborator[] } = await fetchProjectCollaboratorsApi(projectId);
            setProjectCollaborators(prev => ({
                ...prev,
                [projectId]: data
            }));
        },
        []
    );

    const addCollaboratorToProject = useCallback(
        async (projectId: string, params: any) => {
            await addCollaboratorToProjectApi(projectId, params);
            await fetchProjectCollaborators(projectId);
        },
        [fetchProjectCollaborators]
    );

    const updateProjectCollaborator = useCallback(
        async (
            projectId: string,
            collabId: string,
            params: any
        ) => {
            await updateProjectCollaboratorApi(projectId, collabId, params);
            await fetchProjectCollaborators(projectId);
        },
        [fetchProjectCollaborators]
    );

    const deleteCollaboratorFromProject = useCallback(
        async (projectId: string, collabId: string) => {
            await deleteProjectCollaboratorApi(projectId, collabId);
            await fetchProjectCollaborators(projectId);
        },
        [fetchProjectCollaborators]
    );

    return {
        projectCollaborators,
        actions: {
            fetchProjectCollaborators,
            addCollaboratorToProject,
            updateProjectCollaborator,
            deleteCollaboratorFromProject
        }
    };
} 