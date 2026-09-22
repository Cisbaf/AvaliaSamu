'use client';

import { useState, useCallback } from 'react';
import {
    fetchProjectCollaboratorsApi,
    addCollaboratorToProjectApi,
    updateProjectCollaboratorApi,
    deleteProjectCollaboratorApi,
    updateProjectCollaboratorEquipeApi,
} from '@/lib/api';
import { MedicoRole, ProjectCollaborator, ShiftHours, UpdateProjectCollabDto, WorkPeriod } from '@/types/project';

export function useProjectCollaborators() {
    const [projectCollaborators, setProjectCollaborators] = useState<Record<string, ProjectCollaborator[]>>({});

    const fetchProjectCollaborators = useCallback(async (projectId: string) => {
        try {
            const res = await fetchProjectCollaboratorsApi(projectId);
            setProjectCollaborators(prev => ({
                ...prev,
                [projectId]: res.data.map(c => ({
                    ...c,
                    points: c.points as Record<string, number> | undefined,
                    medicoRole: c.medicoRole as MedicoRole | undefined,
                    shiftHours: c.shiftHours as ShiftHours | undefined,
                    workPeriod: (c.workPeriod || WorkPeriod.DIURNO) as WorkPeriod,
                    equipeIds: (c.equipeIds || []) as string[]
                }))
            }));
        } catch (err) {
            console.error('Erro ao buscar colaboradores:', err);
        }
    }, []);

    const addCollaboratorToProject = useCallback(async (
        projectId: string,
        payload: { id: string; role: string; durationSeconds?: number; quantity?: number; pausaMensalSeconds?: number; parametros?: Record<string, number>; medicoRole?: MedicoRole; shiftHours?: ShiftHours; workPeriod?: WorkPeriod }
    ) => {
        await addCollaboratorToProjectApi(
            projectId,
            payload.id,
            payload.role,
            payload.durationSeconds,
            payload.quantity,
            payload.pausaMensalSeconds,
            payload.parametros,
            payload.medicoRole,
            payload.shiftHours,
            payload.workPeriod
        );
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    const updateProjectCollaborator = useCallback(async (
        projectId: string,
        collabId: string,
        data: UpdateProjectCollabDto,
        wasEdited?: boolean
    ) => {
        await updateProjectCollaboratorApi(projectId, collabId, data, wasEdited || false);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    const deleteCollaboratorFromProject = useCallback(async (projectId: string, collabId: string) => {
        await deleteProjectCollaboratorApi(projectId, collabId);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    // Equipe do supervisor só neste projeto (não mexe no cadastro global).
    const updateProjectCollaboratorEquipe = useCallback(async (projectId: string, collabId: string, equipeIds: string[]) => {
        await updateProjectCollaboratorEquipeApi(projectId, collabId, equipeIds);
        await fetchProjectCollaborators(projectId);
    }, [fetchProjectCollaborators]);

    return {
        projectCollaborators,
        actions: {
            fetchProjectCollaborators,
            addCollaboratorToProject,
            updateProjectCollaborator,
            deleteCollaboratorFromProject,
            updateProjectCollaboratorEquipe
        }
    };
}
