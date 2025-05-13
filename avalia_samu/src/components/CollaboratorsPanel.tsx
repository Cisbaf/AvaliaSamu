'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TextField,
  Select,
  MenuItem,
  IconButton,
  Button,
  CircularProgress
} from '@mui/material';
import { Edit, Delete, Add } from '@mui/icons-material';
import { useProjects } from '../context/ProjectContext';
import { GlobalCollaborator, NestedScoringParameters } from '@/types/project';
import CollaboratorModal from './AddCollaboratorModal';
import AddExistingCollaboratorModal from './AddExistingCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';
import ScoringParamsModal from './ParameterPanel';
type CombinedCollaboratorData = GlobalCollaborator & { projectId?: string };

export default function CollaboratorsPanel() {
  const {
    projects,
    selectedProject,
    projectCollaborators,
    globalCollaborators,
    actions: {
      addCollaboratorToProject,
      deleteCollaboratorFromProject,
      fetchProjectCollaborators,
      updateProjectParameters
    }
  } = useProjects();
  const [scoringParamsModalOpen, setScoringParamsModalOpen] = useState(false);
  const [scoringParams, setScoringParams] = useState<NestedScoringParameters>()



  const [panelLoading, setPanelLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRole, setFilterRole] = useState<'all' | string>('all');
  const [roles, setRoles] = useState<string[]>([]);
  const [editingCollaboratorInitialData, setEditingCollaboratorInitialData] = useState<GlobalCollaborator | undefined>(undefined);
  const [isAddExistingModalOpen, setIsAddExistingModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (selectedProject) {
      fetchProjectCollaborators(selectedProject);
    }
  }, [selectedProject, fetchProjectCollaborators]);

  useEffect(() => {
    const rolesInProject = projectCollaborators[selectedProject || '']?.map(c => c.role) || [];
    setRoles(Array.from(new Set(rolesInProject)).filter(role => typeof role === 'string') as string[]);
  }, [projectCollaborators, selectedProject]);

  const combinedProjectGlobalCollaborators: CombinedCollaboratorData[] = useMemo(() => {
    const projectCollabs = projectCollaborators[selectedProject || ''] || [];
    return projectCollabs.map(pc => {
      const globalCollab = globalCollaborators?.find(gc => gc.id === pc.id);
      return {
        id: pc.id,
        nome: globalCollab?.nome ?? 'Nome Desconhecido',
        cpf: globalCollab?.cpf ?? '',
        idCallRote: globalCollab?.idCallRote ?? '',
        role: pc.role,
        pontuacao: pc?.pontuacao,
        isGlobal: globalCollab?.isGlobal ?? false,
        projectId: selectedProject || undefined,
      } as CombinedCollaboratorData;
    });
  }, [projectCollaborators, selectedProject, globalCollaborators]);

  const filteredCollaborators = useMemo(() => {
    return combinedProjectGlobalCollaborators.filter(c => {
      const matchesName = c.nome.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesRole = filterRole === 'all' || (typeof c.role === 'string' && c.role === filterRole);
      return matchesName && matchesRole;
    });
  }, [combinedProjectGlobalCollaborators, searchTerm, filterRole]);

  const availableCollaborators = useMemo(() => {
    if (!globalCollaborators || !selectedProject) return [];
    const projectCollabIds = new Set((projectCollaborators[selectedProject] || []).map(pc => pc.id));
    return globalCollaborators.filter(gc => !projectCollabIds.has(gc.id));
  }, [globalCollaborators, selectedProject, projectCollaborators]);

  const handleDelete = useCallback(async (collaboratorId: string) => {
    if (!selectedProject) return;
    setPanelLoading(true);
    try {
      await deleteCollaboratorFromProject(selectedProject, collaboratorId);
      await fetchProjectCollaborators(selectedProject);
    } catch (error) {
      console.error('Erro ao remover colaborador do projeto:', error);
    } finally {
      setPanelLoading(false);
    }
  }, [deleteCollaboratorFromProject, selectedProject, fetchProjectCollaborators]);

  const handleOpenAddExistingModal = useCallback(() => {
    setIsAddExistingModalOpen(true);
  }, []);

  const handleCloseAddExistingModal = useCallback(() => {
    setIsAddExistingModalOpen(false);
  }, []);

  const handleAddExistingCollaboratorToProject = useCallback(async (collaboratorId: string, role: string) => {
    if (!selectedProject) return;
    setPanelLoading(true);
    try {
      await addCollaboratorToProject(selectedProject, { id: collaboratorId, role });
      await fetchProjectCollaborators(selectedProject);
      handleCloseAddExistingModal();
    } catch (error) {
      console.error('Erro ao adicionar colaborador existente:', error);
    } finally {
      setPanelLoading(false);
    }
  }, [selectedProject, addCollaboratorToProject, fetchProjectCollaborators, handleCloseAddExistingModal]);

  const handleOpenEditModal = useCallback((collab: CombinedCollaboratorData) => {
    const initialData: GlobalCollaborator = {
      id: collab.id,
      nome: collab.nome,
      cpf: collab.cpf,
      idCallRote: collab.idCallRote,
      role: collab.role,
      pontuacao: collab.pontuacao,
      isGlobal: collab.isGlobal,
    };
    setEditingCollaboratorInitialData(initialData);
  }, []);

  const handleCloseEditModal = useCallback(() => {
    setEditingCollaboratorInitialData(undefined);
  }, []);

  const handleEditModalSuccess = useCallback(async () => {
    if (selectedProject) {
      await fetchProjectCollaborators(selectedProject);
    }
    handleCloseEditModal();
  }, [selectedProject, fetchProjectCollaborators, handleCloseEditModal]);

  const isTableLoading = selectedProject ? !projectCollaborators[selectedProject] : false;


  const handleFileUpload = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files || event.target.files.length === 0) return;
    const file = event.target.files[0];

    if (!selectedProject) {
      console.error('Nenhum projeto selecionado.');
      alert('Selecione um projeto antes de enviar a planilha!');
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append('arquivo', file);

    try {
      const response = await fetch(process.env.NEXT_PUBLIC_API_URL + `/api/${selectedProject}/processar`, {
        method: 'POST',
        body: formData,
      });
      if (!response.ok) {
        throw new Error(`Erro HTTP: ${response.status}`);
      }

      console.log('Arquivo processado com sucesso!');
      await fetchProjectCollaborators(selectedProject);
    } catch (error) {
      console.error('Falha no upload:', error);
      alert('Erro ao processar planilha! Verifique o console.');
    } finally {
      setLoading(false);
    }
  }, [selectedProject, fetchProjectCollaborators]);

  return (
    <div className={styles.panel}>
      {!selectedProject && <p>Selecione um projeto para ver os colaboradores.</p>}

      {selectedProject && (
        <>
          <div className={styles.filters}>
            <TextField
              placeholder="Pesquisar por nome"
              size="small"
              fullWidth
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
            />
            <Select
              value={filterRole}
              size="small"
              onChange={e => setFilterRole(e.target.value)}
              className={styles.roleSelect}
              displayEmpty
            >
              <MenuItem value="all">Todas as funções</MenuItem>
              {roles.map(r => (
                <MenuItem key={r} value={r}>{r}</MenuItem>
              ))}
            </Select>
          </div>

          <Button
            variant="contained"
            color="warning"
            onClick={handleOpenAddExistingModal}
            className={styles.addExistingButton}
            startIcon={<Add />}
            style={{ marginBottom: '16px', borderRadius: '20px' }}
            disabled={panelLoading}
          >
            {panelLoading ? <CircularProgress size={24} /> : 'Adicionar Existente'}
          </Button>
          <Button
            variant="contained"
            color="warning"
            className={styles.addExistingButton}
            onClick={() => setScoringParamsModalOpen(true)}
            style={{ marginBottom: '16px', borderRadius: '20px', marginLeft: '8px' }}
          >
            Configurar Parâmetros
          </Button>
          <Button
            variant="contained"
            color="success"
            component="label"
            disabled={loading}
            style={{ marginBottom: '16px', borderRadius: '20px', marginLeft: '8px' }}
            sx={{ ml: 2 }}
          >
            {loading ? 'Enviando...' : 'Enviar Planilha'}
            <input
              type="file"
              hidden
              accept=".xlsx,.xls,.csv"
              onChange={handleFileUpload}
            />
          </Button>

          <ScoringParamsModal
            open={scoringParamsModalOpen}
            onClose={() => setScoringParamsModalOpen(false)}
            onSave={async params => {
              setScoringParams(params);
              if (selectedProject) {
                setPanelLoading(true);
                try {
                  await updateProjectParameters(selectedProject, params);
                  await fetchProjectCollaborators(selectedProject);
                } catch (error) {
                  console.error('Erro ao atualizar parâmetros do projeto:', error);
                } finally {
                  setPanelLoading(false);
                }
              }
              setScoringParamsModalOpen(false);
            }}
            initialParams={scoringParams}
          />


          <TableContainer component={Paper} className={styles.tableContainer}>
            <Table stickyHeader aria-label="project collaborators table">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 'bold' }}>Nome</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Função</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Pontuação</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {isTableLoading ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center">
                      <CircularProgress size={30} />
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredCollaborators.map(collab => (
                    <TableRow key={collab.id}>
                      <TableCell>{collab.nome}</TableCell>
                      <TableCell>{collab.role}</TableCell>
                      <TableCell>{collab.pontuacao}</TableCell>
                      <TableCell>
                        <IconButton
                          onClick={() => handleOpenEditModal(collab)}
                          aria-label={`editar ${collab.nome}`}
                          disabled={panelLoading}
                        >
                          <Edit color="primary" />
                        </IconButton>
                        <IconButton
                          onClick={() => handleDelete(collab.id!)}
                          aria-label={`remover ${collab.nome} do projeto`}
                          disabled={panelLoading}
                        >
                          {panelLoading ? <CircularProgress size={20} color="error" /> : <Delete color="error" />}
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {!isTableLoading && filteredCollaborators.length === 0 && (
            <p className={styles.noResults}>Nenhum colaborador encontrado.</p>
          )}

          <CollaboratorModal
            open={!!editingCollaboratorInitialData}
            onClose={handleCloseEditModal}
            onSuccess={handleEditModalSuccess}
            initialData={editingCollaboratorInitialData}
            projectId={selectedProject}
          />

          <AddExistingCollaboratorModal
            open={isAddExistingModalOpen}
            onClose={handleCloseAddExistingModal}
            collaborators={availableCollaborators}
            onAdd={handleAddExistingCollaboratorToProject}
            loading={panelLoading}
          />
        </>
      )}
    </div>
  );
}
