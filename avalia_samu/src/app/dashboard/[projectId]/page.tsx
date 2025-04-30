'use client';

import { useEffect } from 'react';
import { Box } from '@mui/material';
import { useProjects } from '@/context/ProjectContext';
import CollaboratorsPanel from '@/components/CollaboratorsPanel';
import ParametersPanel from '@/components/ParameterPanel';
import { useParams } from 'next/navigation';

export default function DashboardPage() {
  const { projectId } = useParams();
  const { projects, setSelectedProject } = useProjects();
  useEffect(() => {
    if (typeof projectId === 'string') {
      setSelectedProject(projectId);
    }
  }, [projectId, setSelectedProject]);

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
