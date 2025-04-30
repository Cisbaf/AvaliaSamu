import { useState, useCallback, useEffect } from 'react';
import {
    fetchGlobalCollaboratorsApi,
    createGlobalCollaboratorApi,
    updateGlobalCollaboratorApi,
    deleteGlobalCollaboratorApi
} from '@/lib/api';
import { GlobalCollaborator, Collaborator } from '@/types/project';

export function useGlobalCollaborators() {
    const [globalCollaborators, setGlobalCollaborators] =
        useState<GlobalCollaborator[]>([]);

    const fetchGlobalCollaborators = useCallback(async () => {
        const { data } = await fetchGlobalCollaboratorsApi();
        setGlobalCollaborators(data);
    }, []);

    const createGlobalCollaborator = useCallback(
        async (collab: Omit<Collaborator, 'id'>) => {
            await createGlobalCollaboratorApi(collab);
            await fetchGlobalCollaborators();
        },
        [fetchGlobalCollaborators]
    );

    const updateGlobalCollaborator = useCallback(
        async (id: string, updates: Partial<Collaborator>) => {
            await updateGlobalCollaboratorApi(id, updates);
            await fetchGlobalCollaborators();
        },
        [fetchGlobalCollaborators]
    );

    const deleteGlobalCollaborator = useCallback(
        async (id: string) => {
            await deleteGlobalCollaboratorApi(id);
            await fetchGlobalCollaborators();
        },
        [fetchGlobalCollaborators]
    );

    useEffect(() => {
        fetchGlobalCollaborators();
    }, [fetchGlobalCollaborators]);

    return {
        globalCollaborators,
        actions: {
            createGlobalCollaborator,
            updateGlobalCollaborator,
            deleteGlobalCollaborator
        }
    };
}