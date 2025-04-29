'use client';

import { useEffect } from 'react';
import { Box } from '@mui/material';
import { useProjects } from '@/context/ProjectContext';
import CollaboratorsPanel from '@/components/CollaboratorsPanel';
import ParametersPanel from '@/components/ParameterPanel';
import { useParams } from 'next/navigation';

export default function DashboardPage() {
  const { projectId } = useParams();           // vem da rota
  const { projects, actions } = useProjects();

  // informa ao context qual projeto deve carregar
  useEffect(() => {
    if (typeof projectId === 'string') actions.selectProject(projectId);
  }, [projectId, actions]);

  // agora projet.id é a string correta
  const project = projects.find(p => p.id === projectId);

  if (!project) {
    return <div>Projeto não encontrado</div>;
  }

  return (
    <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <ParametersPanel />
      <CollaboratorsPanel />
    </Box>
  );
}
