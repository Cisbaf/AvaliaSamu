'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box,
  Button,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
  IconButton
} from '@mui/material';
import Delete from '@mui/icons-material/Delete';
import { useProjects } from '../context/ProjectContext';
import ProjectModal from '../components/ProjectModal';

export default function HomePage() {
  const router = useRouter();
  const [modalOpen, setModalOpen] = useState(false);
  const {
    projects,
    setSelectedProject,
    actions: { deleteProject, }
  } = useProjects();

  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) return null;

  const handleProjectSelect = (projectId: string) => {
    setSelectedProject(projectId);
    router.push(`/dashboard/${projectId}`);
  };

  const handleDelete = async (projectId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await deleteProject(projectId);
    } catch (error) {
      console.error('Erro ao deletar projeto:', error);
    }
  };

  return (
    <Box sx={{ p: 4, maxWidth: 800, margin: '0 auto' }}>
      {/* Cabeçalho */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Gestão de Projetos
        </Typography>
        <Button
          variant="contained"
          onClick={() => setModalOpen(true)}
          sx={{ ml: 2 }}
        >
          Novo Projeto
        </Button>
      </Box>

      {/* Lista de Projetos */}
      {projects.length > 0 ? (
        <List sx={{ bgcolor: 'background.paper', borderRadius: 2, boxShadow: 1 }}>
          {projects.map((project) => (
            <ListItem key={project.id} disablePadding>
              <ListItemButton
                onClick={() => handleProjectSelect(project.id!)}
                sx={{
                  py: 2,
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  position: 'relative'
                }}
              >
                <ListItemText
                  primary={project.name}
                  secondary={`Mês: ${project.month}`}
                  sx={{ '& .MuiListItemText-secondary': { mt: 0.5 } }}
                />

                <IconButton
                  size="small"
                  color="error"
                  onClick={(e) => handleDelete(project.id!, e)}
                  sx={{
                    position: 'absolute',
                    right: 16,
                    top: '50%',
                    transform: 'translateY(-50%)'
                  }}
                >
                  <Delete fontSize="small" />
                </IconButton>
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      ) : (
        <Box sx={{ textAlign: 'center', p: 4, border: '1px dashed', borderRadius: 2, borderColor: 'text.disabled' }}>
          <Typography variant="body1" color="text.secondary">
            Nenhum projeto criado ainda
          </Typography>
        </Box>
      )}

      <ProjectModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </Box>
  );
}