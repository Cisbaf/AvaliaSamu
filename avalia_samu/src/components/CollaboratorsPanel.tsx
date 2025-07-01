'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper,
  TextField, Select, MenuItem, IconButton, Button, CircularProgress, Alert,
  Typography
} from '@mui/material';
import { Edit, Delete, Add, EditNote } from '@mui/icons-material';
import { useProjects } from '../context/ProjectContext';
import { GlobalCollaborator, MedicoRole, NestedScoringParameters, ShiftHours } from '@/types/project';
import CollaboratorModal from './modal/AddCollaboratorModal';
import AddExistingCollaboratorModal from './modal/AddExistingCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';
import ScoringParamsModal from './modal/ScoringParamsModal';
import DataForPointsModal from './modal/DataForPointsModal';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';


export type CombinedCollaboratorData = Omit<GlobalCollaborator, 'isGlobal'> & {
  isGlobal: boolean;
  projectId?: string;
  medicoRole?: MedicoRole;
  shiftHours?: ShiftHours;
};

export default function CollaboratorsPanel() {
  const {
    selectedProject,
    projects,
    projectCollaborators,
    globalCollaborators,
    actions: { addCollaboratorToProject, deleteCollaboratorFromProject, fetchProjectCollaborators, updateProjectParameters }
  } = useProjects();

  // Estados
  const [state, setState] = useState({
    searchTerm: '',
    filterRole: 'all' as 'all' | string,
    roles: [] as string[],
    medicoRole: [] as MedicoRole[],
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
  const currentProject = projects.find(p => p.id === selectedProject);


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
      updateState({ medicoRole: Array.from(new Set(inProject.map(c => c.medicoRole))).filter((mr): mr is MedicoRole => mr !== undefined && mr !== ("NENHUM" as MedicoRole)) });
    }
  }, [projectCollaborators, selectedProject]);

  useEffect(() => {
    if (currentProject?.parameters) {
      setScoringParams(currentProject.parameters);
    }
  }, [currentProject]);

  // Dados processados
  const combinedCollaborators = useMemo(() => {
    if (!selectedProject) return [];

    return (projectCollaborators[selectedProject] || []).map(pc => {
      const gc = globalCollaborators?.find(g => g.id === pc.id);
      return {
        id: pc.id,
        nome: pc?.nome || gc?.nome || "-",
        cpf: pc?.cpf || gc?.cpf || "000-000-00-00",
        idCallRote: pc?.idCallRote || gc?.idCallRote || "0000",
        role: pc.role,
        pontuacao: pc.pontuacao,
        isGlobal: gc?.isGlobal ?? false,
        projectId: selectedProject,

        removidos: pc.removidos,
        duration: pc.durationSeconds,
        criticos: pc.criticos,
        pausaMensal: pc.pausaMensalSeconds,
        saidaVtr: pc.saidaVtr,

        medicoRole: pc.medicoRole || gc?.medicoRole,
        shiftHours: pc.shiftHours || gc?.shiftHours,

        points: pc.points || {},
      };
    });
  }, [projectCollaborators, globalCollaborators, selectedProject]);

  const filteredCollaborators = useMemo(() =>
    combinedCollaborators
      .filter(c =>
        c.nome.toLowerCase().includes(state.searchTerm.toLowerCase()) &&
        (state.filterRole === 'all' || c.role === state.filterRole || c.medicoRole === state.filterRole)
      )
      .sort((a, b) => a.nome.localeCompare(b.nome)),
    [combinedCollaborators, state.searchTerm, state.filterRole]
  );

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
    formData.append('arquivo', e.target.files[0], e.target.files[0].name);

    try {
      const response = await fetch(
        `/api/proxy/${selectedProject}/processar`,
        {
          method: 'POST',
          body: formData,
          headers: {
            'Accept': 'application/json',
          },
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Erro ${response.status}: ${response.statusText}`);
      }

      await fetchProjectCollaborators(selectedProject);
    } catch (err: any) {
      updateState({
        error: err.message.includes('Failed to fetch')
          ? 'Falha na conexão com o servidor'
          : err.message
      });
    } finally {
      updateState({ loading: false });
    }
  };

  const refreshCollaborators = async () => {
    if (selectedProject) {
      try {
        await fetchProjectCollaborators(selectedProject);
      } catch (err: any) {
        const errorMessage =
          err.response?.data?.message ||
          err.message ||
          'Erro ao salvar colaborador';
        updateState(errorMessage);
      }
    }
  };

  const handleSaveParameters = async (params: NestedScoringParameters) => {
    if (!selectedProject) return;

    updateState({ panelLoading: true, error: null });

    try {
      await updateProjectParameters(selectedProject, params);
      await fetchProjectCollaborators(selectedProject);
      setScoringParams(params);
      updateState({ scoringParamsModalOpen: false });
    } catch (err: any) {
      updateState({ error: err.response?.data?.message || 'Falha ao salvar parâmetros' });
    } finally {
      updateState({ panelLoading: false });
    }
  };

  function formatTime(seconds: number): string {
    if (!seconds && seconds !== 0) return '00:00:00';

    const h = Math.floor(seconds / 3600).toString().padStart(2, '0');
    const m = Math.floor((seconds % 3600) / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');

    return `${h}:${m}:${s}`;
  }

  const handleExport = () => {
    if (!combinedCollaborators || combinedCollaborators.length === 0) {
      console.error("Nenhum colaborador para exportar.");
      return;
    }

    // Agrupa por função, mas para médicos consideramos também o papel (LIDER ou REGULADOR)
    const collaboratorsByRole = combinedCollaborators
      .sort((a, b) => a.nome.localeCompare(b.nome))
      .reduce((acc, c) => {
        // define a chave da sheet
        let sheetKey = c.role;
        if (c.role === 'MEDICO' && c.medicoRole) {
          sheetKey = `MEDICO_${c.medicoRole}`; // ex: "MEDICO_LIDER" ou "MEDICO_REGULADOR"
        }

        if (!acc[sheetKey]) {
          acc[sheetKey] = [];
        }
        acc[sheetKey].push(c);
        return acc;
      }, {} as Record<string, typeof combinedCollaborators>);

    const wb = XLSX.utils.book_new();

    Object.entries(collaboratorsByRole).forEach(([sheetKey, collabs]) => {
      const sheetData = collabs.map(c => {
        const baseData: Record<string, any> = {
          'Nome': c.nome,
          'Função': c.role === 'MEDICO' && c.medicoRole && c.shiftHours
            ? `${c.role} (${c.medicoRole} - ${c.shiftHours})`
            : c.role,
          'Pausa Mensal': formatTime(c.pausaMensal!),
          'Pausa Pontos': c.points?.['Pausas'] || 0,

        };

        if (sheetKey.startsWith("FROTA")) {
          baseData['Saída VTR'] = formatTime(c.saidaVtr!);
          baseData['Saída VTR Pontos'] = c.points?.['SaidaVTR'] || 0;
        }

        if (c.role === 'MEDICO' && c.medicoRole === MedicoRole.LIDER) {
          baseData['Críticos'] = formatTime(c.criticos!);
          baseData['Críticos Pontos'] = c.points?.['Criticos'] || 0;
        }
        if (c.medicoRole !== MedicoRole.LIDER) {
          baseData['Regulação'] = formatTime(c.duration!);
          baseData['Regulação Pontos'] = c.points?.['Regulacao'] || 0;
        }

        if (sheetKey !== "FROTA") {
          baseData['Removidos'] = c.removidos;
          baseData['Removidos Pontos'] = c.points?.['Removidos'] || 0;
        }
        baseData['Pontuação'] = c.pontuacao;

        return baseData;
      });

      const ws = XLSX.utils.json_to_sheet(sheetData);
      // torna o nome seguro e curto (<=31 caracteres) para o Excel
      const safeSheetName = sheetKey.substring(0, 31).replace(/[:\\/?*\[\]]/g, '');
      XLSX.utils.book_append_sheet(wb, ws, safeSheetName);
    });

    const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
    const fileName = currentProject
      ? `${currentProject.name}_${currentProject.month}_colaboradores_por_funcao.xlsx`
      : 'colaboradores_por_funcao.xlsx';
    saveAs(new Blob([wbout]), fileName);
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
          <Typography sx={{ marginBottom: 3 }}>Projeto: {currentProject?.name || '—'}</Typography>

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
              {state.medicoRole.map(Mrole => (
                <MenuItem key={Mrole} value={Mrole}>{Mrole}</MenuItem>
              ))}

            </Select>
          </div>

          <div className={styles.actionButtons}>
            <Button
              className={styles.chromeButton}
              variant="contained"
              color="warning"
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
              color="warning"
              onClick={() => updateState({ scoringParamsModalOpen: true })}
              sx={{ borderRadius: '20px' }}
              disabled={state.panelLoading}
            >
              Configurar Parâmetros
            </Button>

            <Button
              className={styles.chromeButton}
              variant="contained"
              color="success"
              component="label"
              sx={{ borderRadius: '20px' }}
              disabled={state.loading}
            >
              {state.loading ? 'Enviando...' : 'Enviar Planilha'}
              <input type="file" hidden accept=".xlsx,.xls" onChange={handleUpload} />
            </Button>
            <Button
              className={styles.chromeButton}
              variant="contained"
              color="success"
              component="label"
              sx={{ borderRadius: '20px' }}
              disabled={state.loading}
              onClick={handleExport}
            >
              Exportar Excel
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

                        {/* NOVO: Atualize o onClick para abrir o diálogo */}
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
            initialParams={currentProject?.parameters}
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