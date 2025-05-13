'use client';

import { useState, useCallback } from 'react';
import {
    fetchProjectCollaboratorsApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi,
    deleteProjectCollaboratorApi,
} from '@/lib/api';
import { ProjectCollaborator, UpdateProjectCollabDto } from '@/types/project';

export function useProjectCollaborators() {
    const [projectCollaborators, setProjectCollaborators] = useState<Record<string, ProjectCollaborator[]>>({});

    const fetchProjectCollaborators = useCallback(async (projectId: string) => {
        const res = await fetchProjectCollaboratorsApi(projectId);
        setProjectCollaborators(prev => ({
            ...prev,
            [projectId]: res.data
        }));
    }, []);

    const addCollaboratorToProject = useCallback(async (
        projectId: string,
        payload: { id: string; role: string; durationSeconds?: number; quantity?: number; pausaMensalSeconds?: number; parametros?: Record<string, number> }
    ) => {
        await addCollaboratorToProjectApi(
            projectId,
            payload.id,
            payload.role,
            payload.durationSeconds,
            payload.quantity,
            payload.pausaMensalSeconds,
            payload.parametros
        );
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    const updateProjectCollaborator = useCallback(async (
        projectId: string,
        collabId: string,
        data: UpdateProjectCollabDto
    ) => {
        await updateProjectCollaboratorApi(projectId, collabId, data);
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
    };
}
