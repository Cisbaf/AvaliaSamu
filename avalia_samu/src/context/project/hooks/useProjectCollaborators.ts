'use client';

import React, { useState, useCallback } from 'react';
import {
    fetchProjectCollaboratorsApi,
    deleteProjectCollaboratorApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi
} from '@/lib/api';
import { ProjectCollaborator } from '@/types/project';

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

    const addCollaboratorToProject = useCallback(async (projectId: string, { id, role, durationSeconds, quantity, pausaMensalSeconds, parametros }: { id: string; role: string; durationSeconds?: number; quantity?: number; pausaMensalSeconds?: number; parametros?: Record<string, number> }) => {
        await addCollaboratorToProjectApi(projectId, id, role, durationSeconds, quantity, pausaMensalSeconds, parametros);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    const updateProjectCollaborator = useCallback(async (projectId: string, collabId: string, { role, durationSeconds, quantity, pausaMensalSeconds, parametros }: { role: string; durationSeconds?: number; quantity?: number; pausaMensalSeconds?: number; parametros?: Record<string, number> }) => {
        await updateProjectCollaboratorApi(projectId, collabId, role, durationSeconds, quantity, pausaMensalSeconds, parametros);
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