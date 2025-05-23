'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper,
  TextField, Select, MenuItem, IconButton, Button, CircularProgress, Alert
} from '@mui/material';
import { Edit, Delete, Add, EditNote } from '@mui/icons-material';
import { useProjects } from '../context/ProjectContext';
import { GlobalCollaborator, MedicoRole, NestedScoringParameters, ShiftHours } from '@/types/project';
import CollaboratorModal from './AddCollaboratorModal';
import AddExistingCollaboratorModal from './AddExistingCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';
import ScoringParamsModal from './ParameterPanel';
import { useProjectCollaborators } from '@/context/project/hooks/useProjectCollaborators';
import DataForPointsModal from './DataForPointsModal';

export type CombinedCollaboratorData = Omit<GlobalCollaborator, 'isGlobal'> & {
  isGlobal: boolean;
  projectId?: string;
  medicoRole?: MedicoRole;
  shiftHours?: ShiftHours;
};

export default function CollaboratorsPanel() {
  const {
    selectedProject,
    projectCollaborators,
    globalCollaborators,
    actions: { addCollaboratorToProject, deleteCollaboratorFromProject, fetchProjectCollaborators, updateProjectParameters }
  } = useProjects();

  // Estados
  const [state, setState] = useState({
    searchTerm: '',
    filterRole: 'all' as 'all' | string,
    roles: [] as string[],
    error: null as string | null,
    loading: false,
    panelLoading: false,
    scoringParamsModalOpen: false,
    isAddExistingModalOpen: false,
  });

  const [scoringParams, setScoringParams] = useState<NestedScoringParameters>();
  const [editingCollaboratorInitialData, setEditingCollaboratorInitialData] = useState<CombinedCollaboratorData | undefined>();
  const [editingCollaboratorPointsData, setEditingCollaboratorPointsData] = useState<CombinedCollaboratorData | undefined>();

  // Atualização de estado simplificada
  const updateState = (newState: Partial<typeof state>) => setState(prev => ({ ...prev, ...newState }));

  // Efeitos
  useEffect(() => {
    if (selectedProject) {
      fetchProjectCollaborators(selectedProject)
        .catch(() => updateState({ error: 'Falha ao carregar colaboradores do projeto' }));
    }
  }, [selectedProject, fetchProjectCollaborators]);

  useEffect(() => {
    if (selectedProject) {
      const inProject = projectCollaborators[selectedProject] || [];
      updateState({ roles: Array.from(new Set(inProject.map(c => c.role))) });
    }
  }, [projectCollaborators, selectedProject]);

  // Dados processados
  const combinedCollaborators = useMemo(() => {
    if (!selectedProject) return [];

    return (projectCollaborators[selectedProject] || []).map(pc => {
      const gc = globalCollaborators?.find(g => g.id === pc.id);
      return {
        id: pc.id,
        nome: pc?.nome || gc?.nome || '—',
        cpf: gc?.cpf || '',
        idCallRote: gc?.idCallRote || '',
        role: pc.role,
        pontuacao: pc.pontuacao,
        isGlobal: gc?.isGlobal ?? false,
        projectId: selectedProject,

        quantity: pc.quantity,
        duration: pc.durationSeconds,
        pausaMensal: pc.pausaMensalSeconds,
        saidaVtr: pc.saidaVtr,

        medicoRole: pc.medicoRole || gc?.medicoRole,
        shiftHours: pc.shiftHours || gc?.shiftHours,

      };
    });
  }, [projectCollaborators, globalCollaborators, selectedProject]);

  const filteredCollaborators = useMemo(() =>
    combinedCollaborators.filter(c =>
      c.nome.toLowerCase().includes(state.searchTerm.toLowerCase()) &&
      (state.filterRole === 'all' || c.role === state.filterRole)
    ),
    [combinedCollaborators, state.searchTerm, state.filterRole]);

  const availableCollaborators = useMemo(() => {
    if (!globalCollaborators || !selectedProject) return [];
    const inProjectIds = new Set((projectCollaborators[selectedProject] || []).map(c => c.id));
    return globalCollaborators.filter(gc => !inProjectIds.has(gc.id));
  }, [globalCollaborators, projectCollaborators, selectedProject]);

  // Handlers
  const handleDelete = useCallback(async (id: string) => {
    if (!selectedProject) return;

    updateState({ panelLoading: true, error: null });

    try {
      await deleteCollaboratorFromProject(selectedProject, id);
      await fetchProjectCollaborators(selectedProject);
    } catch (err: any) {
      updateState({ error: err.response?.data?.message || 'Falha ao excluir colaborador' });
    } finally {
      updateState({ panelLoading: false });
    }
  }, [selectedProject, deleteCollaboratorFromProject, fetchProjectCollaborators]);

  const handleAddExisting = useCallback(async (id: string, role: string, medicoRole?: MedicoRole, shiftHours?: ShiftHours) => {
    if (!selectedProject) return;

    updateState({ panelLoading: true, error: null });

    try {
      const globalCollab = globalCollaborators?.find(gc => gc.id === id);
      if (!globalCollab) throw new Error('Colaborador global não encontrado');

      if (role === 'MEDICO' && (!medicoRole || !shiftHours)) {
        throw new Error('Para a função MEDICO, o Papel Médico e o Turno são obrigatórios.');
      }

      await addCollaboratorToProject(selectedProject, {
        id, nome: globalCollab.nome, role,
        medicoRole: medicoRole as MedicoRole,
        shiftHours: shiftHours as ShiftHours
      });

      await fetchProjectCollaborators(selectedProject);
      updateState({ isAddExistingModalOpen: false });
    } catch (err: any) {
      updateState({ error: err.response?.data?.message || err.message || 'Falha ao adicionar colaborador' });
    } finally {
      updateState({ panelLoading: false });
    }
  }, [selectedProject, addCollaboratorToProject, fetchProjectCollaborators, globalCollaborators]);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files?.length || !selectedProject) return;

    updateState({ loading: true, error: null });

    const formData = new FormData();
    formData.append('arquivo', e.target.files[0]);

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/${selectedProject}/processar`, {
        method: 'POST', body: formData
      });

      if (!response.ok) throw new Error(response.statusText);
      await fetchProjectCollaborators(selectedProject);
    } catch (err: any) {
      updateState({ error: err.message || 'Falha ao processar planilha' });
    } finally {
      updateState({ loading: false });
    }
  };

  const refreshCollaborators = async () => {
    if (selectedProject) {
      try {
        await fetchProjectCollaborators(selectedProject);
      } catch {
        updateState({ error: 'Falha ao atualizar colaboradores' });
      }
    }
  };

  // Handlers para modais
  const handleSaveParameters = async (params: NestedScoringParameters) => {
    if (!selectedProject) return;

    updateState({ panelLoading: true, error: null });

    try {
      await updateProjectParameters(selectedProject, params);
      await fetchProjectCollaborators(selectedProject);
      updateState({ scoringParamsModalOpen: false });
    } catch (err: any) {
      updateState({ error: err.response?.data?.message || 'Falha ao salvar parâmetros' });
    } finally {
      updateState({ panelLoading: false });
    }
  };

  const isTableLoading = selectedProject && !projectCollaborators[selectedProject];

  return (
    <div className={styles.panel}>
      {!selectedProject ? <p>Selecione um projeto.</p> : (
        <>
          {state.error && (
            <Alert severity="error" onClose={() => updateState({ error: null })} sx={{ mb: 2 }}>
              {state.error}
            </Alert>
          )}

          <div className={styles.filters}>
            <TextField
              placeholder="Pesquisar"
              size="small"
              fullWidth
              value={state.searchTerm}
              onChange={e => updateState({ searchTerm: e.target.value, error: null })}
            />

            <Select
              value={state.filterRole}
              size="small"
              onChange={e => updateState({ filterRole: e.target.value as string, error: null })}
            >
              <MenuItem value="all">Todas funções</MenuItem>
              {state.roles.map(role => (
                <MenuItem key={role} value={role}>{role}</MenuItem>
              ))}
            </Select>
          </div>

          <div className={styles.actionButtons}>
            <Button
              className={styles.chromeButton}
              variant="contained"
              color="inherit"
              startIcon={<Add />}
              onClick={() => updateState({ isAddExistingModalOpen: true })}
              sx={{ borderRadius: '20px' }}
              disabled={state.panelLoading}
            >
              Adicionar Existente
            </Button>

            <Button
              className={styles.chromeButton}

              variant="contained"
              color="inherit"
              onClick={() => updateState({ scoringParamsModalOpen: true })}
              sx={{ borderRadius: '20px' }}
              disabled={state.panelLoading}
            >
              Configurar Parâmetros
            </Button>

            <Button
              className={styles.chromeButton}
              variant="contained"
              color="inherit"
              component="label"
              sx={{ borderRadius: '20px' }}
              disabled={state.loading}
            >
              {state.loading ? 'Enviando...' : 'Enviar Planilha'}
              <input type="file" hidden accept=".xlsx,.xls" onChange={handleUpload} />
            </Button>
          </div>

          <TableContainer component={Paper} className={styles.tableContainer}>
            <Table stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Nome</TableCell>
                  <TableCell>Função</TableCell>
                  <TableCell>Pontuação</TableCell>
                  <TableCell>Ações</TableCell>
                </TableRow>
              </TableHead>

              <TableBody>
                {isTableLoading ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center"><CircularProgress /></TableCell>
                  </TableRow>
                ) : filteredCollaborators.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center">Nenhum colaborador encontrado</TableCell>
                  </TableRow>
                ) : (
                  filteredCollaborators.map(c => (
                    <TableRow key={c.id}>
                      <TableCell>{c.nome}</TableCell>
                      <TableCell>
                        {c.role}
                        {c.role === 'MEDICO' && c.medicoRole && c.shiftHours
                          ? ` (${c.medicoRole} - ${c.shiftHours})` : ''}
                      </TableCell>
                      <TableCell>{c.pontuacao}</TableCell>
                      <TableCell>
                        <IconButton
                          onClick={() => setEditingCollaboratorInitialData(c)}
                          disabled={state.panelLoading}
                          title="Editar colaborador"
                        >
                          <Edit color='primary' />
                        </IconButton>

                        <IconButton
                          onClick={() => setEditingCollaboratorPointsData(c)}
                          disabled={state.panelLoading}
                          title="Editar dados para pontuação"
                        >
                          <EditNote color='success' />
                        </IconButton>

                        <IconButton
                          onClick={() => handleDelete(c.id!)}
                          disabled={state.panelLoading}
                          title="Remover colaborador"
                        >
                          {state.panelLoading ? <CircularProgress size={24} /> : <Delete color="error" />}
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {/* Modais */}
          <ScoringParamsModal
            open={state.scoringParamsModalOpen}
            onClose={() => updateState({ scoringParamsModalOpen: false })}
            onSave={handleSaveParameters}
            initialParams={scoringParams}
          />

          <DataForPointsModal
            open={!!editingCollaboratorPointsData}
            onClose={() => setEditingCollaboratorPointsData(undefined)}
            onSuccess={() => { refreshCollaborators(); setEditingCollaboratorPointsData(undefined); }}
            initialData={editingCollaboratorPointsData}
            projectId={selectedProject}
          />

          <CollaboratorModal
            open={!!editingCollaboratorInitialData}
            onClose={() => setEditingCollaboratorInitialData(undefined)}
            onSuccess={() => { refreshCollaborators(); setEditingCollaboratorInitialData(undefined); }}
            initialData={editingCollaboratorInitialData}
            projectId={selectedProject}
          />

          <AddExistingCollaboratorModal
            open={state.isAddExistingModalOpen}
            onClose={() => updateState({ isAddExistingModalOpen: false })}
            collaborators={availableCollaborators}
            onAdd={handleAddExisting}
            loading={state.panelLoading}
          />
        </>
      )}
    </div>
  );
}
