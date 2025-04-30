'use client';

import { Typography, Select, MenuItem, TextField, Input, Button } from '@mui/material';
import { useProjects } from '../context/ProjectContext';
import styles from './styles/ParametersPanel.module.css';
import { useEffect, useMemo, useState } from 'react';
import { Project } from '@/types/project';

const roleParametersConfig: Record<string, string[]> = {
  Colaborador: ['pausaGeral'],
  Tarma: ['pausa1', 'pausa2'],
  Frota: ['pausa3', 'pausa4'],
  Médico: ['pausa5'],
};

export default function ParametersPanel() {
  const {
    projects,
    selectedProject,
    actions: { updateProject },
    globalCollaborators
  } = useProjects();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [project, setProject] = useState<Project>();
  const [selectedRole, setSelectedRole] = useState('Colaborador');
  const [formValues, setFormValues] = useState<Record<string, number>>({});

  const roles = useMemo(() => [
    'Colaborador',
    ...Array.from(new Set(globalCollaborators.map(c => c.role)))
  ], [globalCollaborators]);



  useEffect(() => {
    const project = selectedProject
      ? projects.find(p => p.id === selectedProject)
      : undefined;
    setProject(project);
  }, [])

  useEffect(() => {
    if (project && selectedRole) {
      const roleParams = typeof project.parameters?.[selectedRole] === 'object' && !Array.isArray(project.parameters?.[selectedRole])
        ? (project.parameters?.[selectedRole] as Record<string, number>)
        : {};
      const initialValues: Record<string, number> = {};
      roleParametersConfig[selectedRole]?.forEach(param => {
        initialValues[param] = roleParams[param] || 0;
      });
      setFormValues(initialValues);
    }
  }, [project, selectedRole]);

  const handleInputChange = (param: string, value: string) => {
    const newValue = Number(value);
    if (isNaN(newValue)) return;
    setFormValues(prev => ({ ...prev, [param]: newValue }));
  };

  const handleSubmit = () => {
    if (!project || !selectedProject || !selectedRole) return;

    const newParameters = {
      ...project.parameters,
      ...Object.fromEntries(
        Object.entries(formValues).map(([key, value]) => [`${selectedRole}.${key}`, value])
      )
    };

    updateProject(selectedProject, { parameters: newParameters });
  };

  const handleFileUpload = async () => {
    if (!selectedFile || !selectedProject) {
      alert('Selecione um arquivo e um projeto primeiro!');
      return;
    }
    if (!project) {
      return (
        <div className={styles.panel}>
          <Typography variant="h6">Selecione um projeto para configurar os parâmetros</Typography>
        </div>
      );
    }
  }

  return (
    <div className={styles.panel}>
      <Typography variant="h4" className={styles.title}>Parâmetros</Typography>

      <div className={styles.uploadSection}>
        <Input
          type="file"
          inputProps={{ accept: '.xlsx, .xls' }}
          onChange={(e) => setSelectedFile((e.target as HTMLInputElement).files?.[0] || null)}
          style={{ display: 'none' }}
          id="file-upload"
        />

        <label htmlFor="file-upload">
          <Button variant="contained" component="span" color="primary">
            Selecionar Arquivo
          </Button>
        </label>

        {selectedFile && (
          <>
            <Typography variant="caption" sx={{ ml: 2 }}>{selectedFile.name}</Typography>
            <Button
              variant="contained"
              color="success"
              onClick={handleFileUpload}
              disabled={loading}
              sx={{ ml: 2 }}
            >
              {loading ? 'Enviando...' : 'Enviar Planilha'}
            </Button>
          </>
        )}
      </div>
      <div className={styles.inputContainer}>
        <Typography variant="subtitle1">Função:</Typography>
        <Select
          value={selectedRole}
          onChange={(e) => setSelectedRole(e.target.value)}
          fullWidth
          size="small"
        >
          {roles.map((role) => (
            <MenuItem key={role} value={role}>
              {role}
            </MenuItem>
          ))}
        </Select>
      </div>

      {roleParametersConfig[selectedRole]?.map((param) => (
        <div key={param} className={styles.inputContainer}>
          <TextField
            size="small"
            variant="filled"
            label={`Parâmetro ${param.toUpperCase()}`}
            color="warning"
            fullWidth
            value={formValues[param] ?? ''}
            onChange={(e) => handleInputChange(param, e.target.value)}
            className={styles.inputField}
            type="number"
          />
        </div>
      ))}

      <Button
        variant="contained"
        color="primary"
        onClick={handleSubmit}
        sx={{ mt: 3 }}
      >
        Salvar Parâmetros
      </Button>
    </div>
  )
}
