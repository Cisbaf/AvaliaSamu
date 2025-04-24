// src/app/page.tsx (atualizado)
'use client';

import { use, useEffect, useState } from 'react';
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
import { useProjectContext } from '../context/ProjectContext';
import ProjectModal from '../components/ProjectModal';

export default function HomePage() {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const { projects, selectProject }: { projects: { id: string; name: string; month: string }[]; selectProject: (id: string) => void } = useProjectContext();
  const { removeProject } = useProjectContext();



  useEffect(() => {
    setMounted(true);
  }, []);

  const handleProjectSelect = (projectId: string) => {
    selectProject(projectId);
    router.push(`/dashboard/${projectId}`);
  }

  if (!mounted) {
    return null;
  }

  return (
    <Box sx={{
      p: 4,
      maxWidth: 800,
      margin: '0 auto'
    }}>
      {/* Cabeçalho */}
      <Box sx={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        mb: 4
      }}>
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
        <List sx={{
          bgcolor: 'background.paper',
          borderRadius: 2,
          boxShadow: 1
        }}>
          {projects.map((project) => (
            <ListItem
              key={project.id}
              disablePadding
            >
              <ListItemButton
                onClick={() => handleProjectSelect(project.id)}
                sx={{
                  py: 2,
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  position: 'relative' // Adicione isto
                }}>
                <ListItemText
                  primary={project.name}
                  secondary={`Mês: ${project.month}`}
                  sx={{
                    '& .MuiListItemText-secondary': {
                      mt: 0.5
                    }
                  }}
                />

                <IconButton
                  size="small"
                  color="error"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeProject(project.id);
                  }}
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
        <Box sx={{
          textAlign: 'center',
          p: 4,
          border: '1px dashed',
          borderRadius: 2,
          borderColor: 'text.disabled'
        }}>
          <Typography variant="body1" color="text.secondary">
            Nenhum projeto criado ainda
          </Typography>
        </Box>
      )}

      <ProjectModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
      />
    </Box>
  );
}