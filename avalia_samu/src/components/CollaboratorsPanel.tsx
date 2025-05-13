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
import { GlobalCollaborator, MedicoRole, NestedScoringParameters, ProjectCollaborator, ShiftHours } from '@/types/project';
import CollaboratorModal from './AddCollaboratorModal';
import AddExistingCollaboratorModal from './AddExistingCollaboratorModal';
import styles from './styles/CollaboratorsPanel.module.css';
import ScoringParamsModal from './ParameterPanel';

export type CombinedCollaboratorData = GlobalCollaborator & {
  projectId?: string;
  medicoRole?: MedicoRole;
  shiftHours?: ShiftHours;
};

export default function CollaboratorsPanel() {
  const {
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
  const [scoringParams, setScoringParams] = useState<NestedScoringParameters>();
  const [panelLoading, setPanelLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRole, setFilterRole] = useState<'all' | string>('all');
  const [roles, setRoles] = useState<string[]>([]);
  const [editingCollaboratorInitialData, setEditingCollaboratorInitialData] = useState<CombinedCollaboratorData | undefined>(undefined);
  const [isAddExistingModalOpen, setIsAddExistingModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (selectedProject) {
      fetchProjectCollaborators(selectedProject);
    }
  }, [selectedProject, fetchProjectCollaborators]);

  useEffect(() => {
    const inProject = projectCollaborators[selectedProject || ''] || [];
    setRoles(Array.from(new Set(inProject.map(c => c.role))));
  }, [projectCollaborators, selectedProject]);

  const combinedProjectGlobalCollaborators: CombinedCollaboratorData[] = useMemo(() => {
    const pcs = projectCollaborators[selectedProject || ''] || [];
    return pcs.map(pc => {
      const gc = globalCollaborators?.find(g => g.id === pc.id)!;
      return {
        // base fields
        id: pc.id,
        nome: gc?.nome || '—',
        cpf: gc?.cpf || '',
        idCallRote: gc?.idCallRote || '',
        role: pc.role,
        pontuacao: pc.pontuacao,
        isGlobal: gc?.isGlobal ?? false,
        projectId: selectedProject || undefined,

        // **extras para edição**
        quantity: pc.quantity,
        durationSeconds: pc.durationSeconds,
        pausaMensalSeconds: pc.pausaMensalSeconds,
        medicoRole: pc.medicoRole,
        shiftHours: pc.shiftHours,
      };
    });
  }, [projectCollaborators, globalCollaborators, selectedProject]);

  const filtered = useMemo(() => {
    return combinedProjectGlobalCollaborators.filter(c => {
      const nameMatch = c.nome.toLowerCase().includes(searchTerm.toLowerCase());
      const roleMatch = filterRole === 'all' || c.role === filterRole;
      return nameMatch && roleMatch;
    });
  }, [combinedProjectGlobalCollaborators, searchTerm, filterRole]);

  const available = useMemo(() => {
    if (!globalCollaborators || !selectedProject) return [];
    const inProjectIds = new Set((projectCollaborators[selectedProject] || []).map(c => c.id));
    return globalCollaborators.filter(gc => !inProjectIds.has(gc.id));
  }, [globalCollaborators, projectCollaborators, selectedProject]);

  const handleDelete = useCallback(async (id: string) => {
    if (!selectedProject) return;
    setPanelLoading(true);
    try {
      await deleteCollaboratorFromProject(selectedProject, id);
      await fetchProjectCollaborators(selectedProject);
    } finally {
      setPanelLoading(false);
    }
  }, [selectedProject, deleteCollaboratorFromProject, fetchProjectCollaborators]);

  const handleOpenAddExisting = () => setIsAddExistingModalOpen(true);
  const handleCloseAddExisting = () => setIsAddExistingModalOpen(false);

  const handleAddExisting = useCallback(async (id: string, role: string) => {
    if (!selectedProject) return;
    setPanelLoading(true);
    try {
      await addCollaboratorToProject(selectedProject, { id, role });
      await fetchProjectCollaborators(selectedProject);
      handleCloseAddExisting();
    } finally {
      setPanelLoading(false);
    }
  }, [selectedProject, addCollaboratorToProject, fetchProjectCollaborators]);

  const handleOpenEdit = (collab: CombinedCollaboratorData) => {
    setEditingCollaboratorInitialData(collab);
  };
  const handleCloseEdit = () => setEditingCollaboratorInitialData(undefined);
  const handleEditSuccess = async () => {
    if (selectedProject) await fetchProjectCollaborators(selectedProject);
    handleCloseEdit();
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files?.length || !selectedProject) return;
    setLoading(true);
    const file = e.target.files[0];
    const fd = new FormData();
    fd.append('arquivo', file);
    try {
      const resp = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/${selectedProject}/processar`, {
        method: 'POST', body: fd
      });
      if (!resp.ok) throw new Error(resp.statusText);
      await fetchProjectCollaborators(selectedProject);
    } finally {
      setLoading(false);
    }
  };

  const isTableLoading = selectedProject && !projectCollaborators[selectedProject];

  return (
    <div className={styles.panel}>
      {!selectedProject && <p>Selecione um projeto.</p>}
      {selectedProject && (
        <>
          <div className={styles.filters}>
            <TextField
              placeholder="Pesquisar"
              size="small"
              fullWidth
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
            />
            <Select
              value={filterRole}
              size="small"
              onChange={e => setFilterRole(e.target.value as string)}
            >
              <MenuItem value="all">Todas funções</MenuItem>
              {roles.map(r => <MenuItem key={r} value={r}>{r}</MenuItem>)}
            </Select>
          </div>

          <Button
            variant="contained"
            color="warning"
            startIcon={<Add />}
            onClick={handleOpenAddExisting}
            disabled={panelLoading}
          >
            Adicionar Existente
          </Button>
          <Button
            variant="contained"
            color="warning"
            onClick={() => setScoringParamsModalOpen(true)}
            disabled={panelLoading}
            style={{ marginLeft: 8 }}
          >
            Configurar Parâmetros
          </Button>
          <Button
            variant="contained"
            color="success"
            component="label"
            disabled={loading}
            style={{ marginLeft: 8 }}
          >
            {loading ? 'Enviando...' : 'Enviar Planilha'}
            <input type="file" hidden accept=".xlsx,.xls" onChange={handleUpload} />
          </Button>

          <ScoringParamsModal
            open={scoringParamsModalOpen}
            onClose={() => setScoringParamsModalOpen(false)}
            onSave={async params => {
              setPanelLoading(true);
              try {
                if (selectedProject) await updateProjectParameters(selectedProject, params);
                await fetchProjectCollaborators(selectedProject!);
              } finally {
                setPanelLoading(false);
                setScoringParamsModalOpen(false);
              }
            }}
            initialParams={scoringParams}
          />

          <TableContainer component={Paper} style={{ marginTop: 16 }}>
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
                {isTableLoading
                  ? <TableRow><TableCell colSpan={4} align="center"><CircularProgress /></TableCell></TableRow>
                  : filtered.map(c => (
                    <TableRow key={c.id}>
                      <TableCell>{c.nome}</TableCell>
                      <TableCell>{c.role}</TableCell>
                      <TableCell>{c.pontuacao}</TableCell>
                      <TableCell>
                        <IconButton onClick={() => handleOpenEdit(c)}><Edit color='primary' /></IconButton>
                        <IconButton onClick={() => handleDelete(c.id!)}><Delete color="error" /></IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                }
              </TableBody>
            </Table>
          </TableContainer>

          <CollaboratorModal
            open={!!editingCollaboratorInitialData}
            onClose={handleCloseEdit}
            onSuccess={handleEditSuccess}
            initialData={editingCollaboratorInitialData}
            projectId={selectedProject}
          />
          <AddExistingCollaboratorModal
            open={isAddExistingModalOpen}
            onClose={handleCloseAddExisting}
            collaborators={available}
            onAdd={handleAddExisting}
            loading={panelLoading}
          />
        </>
      )}
    </div>
  );
}
