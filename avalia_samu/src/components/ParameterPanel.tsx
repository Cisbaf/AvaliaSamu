'use client';

import { Typography, Select, MenuItem, TextField, Input, Button } from '@mui/material';
import { useProjects } from '../context/ProjectContext';
import styles from './styles/ParametersPanel.module.css';
import { useState } from 'react';

export default function ParametersPanel() {
  const {
    projects,
    selectedProject,
    actions: { updateProject }
  } = useProjects();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);

  // Encontra o projeto selecionado usando o _id
  const project = projects.find(p => p._id === selectedProject);

  // Tipo importado do contexto
  type Parameters = typeof parameters;
  const parameters = project?.parameters || {
    pausa1: 0,
    pausa2: 0,
    pausa3: 0,
    pausa4: 0
  };

  const handleChangeParameter = (name: keyof Parameters, value: string) => {
    if (!selectedProject || !project) return;

    const newValue = Number(value);
    if (isNaN(newValue)) return;

    // Atualiza usando a nova ação do contexto
    updateProject(selectedProject, {
      ...project,
      parameters: {
        ...parameters,
        [name]: newValue
      }
    });
  };

  const handleFileUpload = async () => {
    if (!selectedFile) {
      alert('Selecione um arquivo primeiro!');
      return;
    }

    const formData = new FormData();
    formData.append('arquivo', selectedFile);

    setLoading(true);
    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/processar`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || 'Erro no upload');
      }

      alert('Arquivo processado com sucesso!');
      setSelectedFile(null);
    } catch (error) {
      alert(error instanceof Error ? error.message : 'Erro ao enviar arquivo');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.panel}>
      <Typography variant="h4" className={styles.title}>
        Parâmetros
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
            <Typography variant="caption" sx={{ ml: 2 }}>
              {selectedFile.name}
            </Typography>
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

      <Typography variant="subtitle1" className={styles.subtitle}>
        Configurações de Pausa
      </Typography>

      {([1, 2, 3, 4] as const).map((num) => {
        const paramName = `pausa${num}` as keyof Parameters;
        return (
          <div key={paramName} className={styles.inputContainer}>
            <TextField
              size="small"
              variant="filled"
              label={`Tempo de Pausa ${num}`}
              color="warning"
              fullWidth
              value={parameters[paramName]}
              onChange={(e) => handleChangeParameter(paramName, e.target.value)}
              className={styles.inputField}
              type="number"
            />
          </div>
        );
      })}
    </div>
  );
}