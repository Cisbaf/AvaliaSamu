// components/CollaboratorsPanel.tsx
'use client';

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
  IconButton
} from '@mui/material';
import { Search, Edit, Delete } from '@mui/icons-material';
import { useProjectContext } from '../context/ProjectContext';
import { useState, useEffect } from 'react';
import CollaboratorModal from './AddCollaboratorModal'; // Adjust the path if necessary
import styles from './styles/CollaboratorsPanel.module.css';


export default function CollaboratorsPanel() {
  const { projects, selectedProject, deleteCollaborator, updateCollaborator } = useProjectContext();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRole, setFilterRole] = useState('all');
  const [roles, setRoles] = useState<string[]>([]);
  const [editingCollaborator, setEditingCollaborator] = useState<{ id: string; name: string; function: string } | null>(null);

  const project = projects.find(p => p.id === selectedProject);

  useEffect(() => {
    if (project) {
      const uniqueRoles = Array.from(new Set(project.collaborators.map(c => c.function)));
      setRoles(uniqueRoles);
    }
  }, [project]);

  const filteredCollaborators = project?.collaborators.filter(c => {
    const matchesSearch = c.name.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = filterRole === 'all' || c.function === filterRole;
    return matchesSearch && matchesRole;
  }) || [];

  const handleSaveEdit = (data: { name: string; function: string }) => {
    if (editingCollaborator && selectedProject) {
      updateCollaborator(selectedProject, editingCollaborator.id, data);
      setEditingCollaborator(null);
    }
  };

  return (
    <div className={styles.panel}>
      <div className={styles.filters}>
        <TextField
          placeholder="Pesquisar por nome"
          variant="outlined"
          size="small"
          fullWidth
          slotProps={{
            input: {
              startAdornment: <Search sx={{ color: 'action.active', mr: 1 }} />
            }
          }}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />

        <Select
          value={filterRole}
          onChange={(e) => setFilterRole(e.target.value)}
          size="small"
          className={styles.roleSelect}
        >
          <MenuItem value="all">Todas as funções</MenuItem>
          {roles.map(role => (
            <MenuItem key={role} value={role}>{role}</MenuItem>
          ))}
        </Select>
      </div>

      <TableContainer component={Paper} className={styles.tableContainer}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Nome</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Função</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Pontos</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Ações</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {filteredCollaborators.map((collab) => (
              <TableRow key={collab.id}>
                <TableCell>{collab.name}</TableCell>
                <TableCell>{collab.function}</TableCell>
                <TableCell>{collab.points}</TableCell>
                <TableCell>
                  <IconButton onClick={() => setEditingCollaborator(collab)}>
                    <Edit color="primary" />
                  </IconButton>
                  <IconButton onClick={() => deleteCollaborator(selectedProject!, collab.id)}>
                    <Delete color="error" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Modal de Edição */}
      <CollaboratorModal
        open={!!editingCollaborator}
        onClose={() => setEditingCollaborator(null)}
        onSave={handleSaveEdit}
        initialData={editingCollaborator || undefined}
      />
    </div>
  );
}