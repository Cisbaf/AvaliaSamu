'use client';

import React, { useEffect, useMemo, useState } from 'react';
import {
  Typography,
  Select,
  MenuItem,
  TextField,
  Input,
  Button
} from '@mui/material';
import { useProjects } from '@/context/ProjectContext';
import styles from './styles/ParametersPanel.module.css';
import api from '@/lib/api';
import { Project } from '@/types/project';

// Configuração dos parâmetros por role conforme planilha
const roleParametersConfig: Record<string, { key: string; label: string }[]> = {
  TARM: [
    { key: 'removidos', label: 'Quantidade de Removidos' },
    { key: 'tempoRegulacao', label: 'Tempo de Regulação (segundos)' },
    { key: 'pausasMensal', label: 'Pausas Mensais (segundos)' },
  ],
  FROTA: [
    { key: 'tempoSaidaVTR', label: 'Tempo de Saída VTR (segundos)' },
    { key: 'tempoRegulacaoFrota', label: 'Tempo de Regulação Frota (segundos)' },
    { key: 'pausasMensal', label: 'Pausas Mensais (segundos)' },
  ],
  MEDICO_REGULADOR_12H: [
    { key: 'tempoRegulacaoMedica', label: 'Tempo de Regulação Médica (segundos)' },
  ],
  MEDICO_REGULADOR_24H: [
    { key: 'tempoRegulacaoMedica', label: 'Tempo de Regulação Médica (segundos)' },
  ],
  MEDICO_LIDER_12H: [
    { key: 'tempoRegulacaoLider', label: 'Tempo de Regulação Líder (segundos)' },
  ],
  MEDICO_LIDER_24H: [
    { key: 'tempoRegulacaoLider', label: 'Tempo de Regulação Líder (segundos)' },
  ],
};

export default function ParametersPanel() {
  const {
    projects,
    selectedProject,
    actions: { updateProject }
  } = useProjects();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [project, setProject] = useState<Project>();
  const [selectedRole, setSelectedRole] = useState('TARM');
  const [formValues, setFormValues] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);

  const roles = useMemo(() => Object.keys(roleParametersConfig), []);

  // carrega projeto atual
  useEffect(() => {
    if (selectedProject) {
      const proj = projects.find(p => p.id === selectedProject);
      setProject(proj);
    }
  }, [projects, selectedProject]);

  // inicializa formValues quando muda role ou project
  useEffect(() => {
    if (!project) return;
    const existingParams = project.parameters || {};
    const initial: Record<string, number> = {};
    const params = roleParametersConfig[selectedRole] || [];
    params.forEach(({ key }) => {
      const flatKey = `${selectedRole}.${key}`;
      initial[key] = existingParams[flatKey] ?? 0;
    });
    setFormValues(initial);
  }, [project, selectedRole]);

  const handleInputChange = (key: string, value: string) => {
    const num = Number(value);
    if (isNaN(num)) return;
    setFormValues(prev => ({ ...prev, [key]: num }));
  };


  const handleFileUpload = async () => {
    if (!selectedFile || !selectedProject) {
      alert('Selecione um arquivo e um projeto primeiro!');
      return;
    }

    setLoading(true);

    try {
      const formData = new FormData();
      formData.append('arquivo', selectedFile); // <- nome do campo deve ser 'arquivo'

      await api.post('/processar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      alert('Planilha enviada e processada com sucesso!');
    } catch (error) {
      console.error('Erro ao enviar planilha:', error);
      alert('Erro ao enviar ou processar a planilha.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!project || !selectedProject) return;
    setLoading(true);
    try {
      const flat: Record<string, number> = {};
      Object.entries(formValues).forEach(([key, value]) => {
        flat[`${selectedRole}.${key}`] = value;
      });
      const newParameters = { ...project.parameters, ...flat };

      await updateProject(selectedProject, { parameters: newParameters });
    } catch (err) {
      console.error('Erro ao salvar parâmetros:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.panel}>
      <Typography variant="h4" className={styles.title}>
        Parâmetros do Projeto
      </Typography>

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

      {/* Select de Role */}
      <div className={styles.inputContainer}>
        <Typography>Função:</Typography>
        <Select
          fullWidth
          size="small"
          value={selectedRole}
          onChange={e => setSelectedRole(e.target.value)}
        >
          {roles.map(role => (
            <MenuItem key={role} value={role}>
              {role}
            </MenuItem>
          ))}
        </Select>
      </div>

      {/* Campos dinamicamente por roleParametersConfig */}
      {roleParametersConfig[selectedRole]?.map(({ key, label }) => (
        <div key={key} className={styles.inputContainer}>
          <TextField
            type="number"
            size="small"
            variant="outlined"
            label={label}
            fullWidth
            value={formValues[key] ?? ''}
            onChange={e => handleInputChange(key, e.target.value)}
          />
        </div>
      ))}

      {/* Botão de salvar */}
      <Button
        variant="contained"
        color="primary"
        onClick={handleSubmit}
        disabled={loading}
        sx={{ mt: 2 }}
      >
        {loading ? 'Salvando...' : 'Salvar Parâmetros'}
      </Button>
    </div>
  );
}
