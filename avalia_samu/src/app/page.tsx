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
  IconButton,
  Checkbox,
  CircularProgress
} from '@mui/material';
import Delete from '@mui/icons-material/Delete';
import DownloadIcon from '@mui/icons-material/Download';
import { useProjects } from '../context/ProjectContext';
import ProjectModal from '../components/modal/ProjectModal';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import { GlobalCollaborator, ProjectCollaborator } from '@/types/project';
import { DEFAULT_PARAMS } from '@/components/utils/scoring-params';


export default function HomePage() {
  const router = useRouter();
  const [modalOpen, setModalOpen] = useState(false);
  const {
    projects,
    projectCollaborators,
    globalCollaborators,
    actions: { deleteProject, fetchProjectCollaborators, updateProjectParameters }
  } = useProjects();

  const [selectedProjectIds, setSelectedProjectIds] = useState<string[]>([]);
  const [isExporting, setIsExporting] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  useEffect(() => {
    if (mounted) {
      projects.forEach(project => {
        if (project.id && !projectCollaborators[project.id]) {
          fetchProjectCollaborators(project.id).catch(error => {
            console.error(`Erro ao buscar colaboradores do projeto ${project.id}:`, error);
          });
        }
        if (project.parameters.colab.pausas?.length == 0) {
          updateProjectParameters(project.id!, DEFAULT_PARAMS!)
        }
      });
    }
  }, [mounted, projects, fetchProjectCollaborators, projectCollaborators]);

  if (!mounted) return null;

  const handleProjectSelect = (projectId: string) => {
    router.push(`/dashboard/${projectId}`);
  };

  const handleDelete = async (projectId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedProjectIds(prev => prev.filter(id => id !== projectId));
    try {
      await deleteProject(projectId);
    } catch (error) {
      console.error('Erro ao deletar projeto:', error);
    }
  };

  const handleCheckboxChange = (projectId: string, checked: boolean) => {
    setSelectedProjectIds(prev =>
      checked ? [...prev, projectId] : prev.filter(id => id !== projectId)
    );
  };

  const formatarFuncao = (colaborador: ProjectCollaborator, globalCollab?: GlobalCollaborator): string => {
    const role = colaborador.role || globalCollab?.role;
    const medicoRole = colaborador.medicoRole || globalCollab?.medicoRole;
    const shiftHours = colaborador.shiftHours || globalCollab?.shiftHours;

    if (role === 'MEDICO' && medicoRole && shiftHours) {
      return `${role} (${medicoRole} - ${shiftHours})`;
    }
    return role || 'Função Desconhecida';
  };

  const handleExportSelectedMonthly = async () => {
    if (selectedProjectIds.length === 0) {
      alert('Selecione pelo menos um projeto para exportar.');
      return;
    }

    setIsExporting(true);

    try {
      const pontosConsolidados: {
        [key: string]: {
          nome: string;
          funcao: string;
          pontos_por_mes: { [mes: string]: number };
          pontuacao_total: number;
        }
      } = {};
      const mesesPresentes = new Set<string>();

      for (const projectId of selectedProjectIds) {
        const projetoAtual = projects.find(p => p.id === projectId);
        if (!projetoAtual) {
          console.warn(`Projeto com ID ${projectId} não encontrado.`);
          continue;
        }
        const mesProjeto = `${projetoAtual.month || 'Mês Desconhecido'} - ${projetoAtual.name}`;
        mesesPresentes.add(mesProjeto);

        if (!projectCollaborators[projectId]) {
          console.warn(`Colaboradores para o projeto ${projectId} não carregados. Tentando buscar...`);
          try {
            await fetchProjectCollaborators(projectId);
            await new Promise(resolve => setTimeout(resolve, 100));
          } catch (error) {
            console.error(`Falha ao buscar colaboradores para ${projectId} durante exportação:`, error);
            alert(`Não foi possível carregar os dados do projeto ${projetoAtual.name} (${mesProjeto}). A exportação pode estar incompleta.`);
            continue;
          }
        }

        const colaboradoresDoProjeto = projectCollaborators[projectId] || [];

        for (const colab of colaboradoresDoProjeto) {
          const globalColab = globalCollaborators?.find(gc => gc.id === colab.id);
          const nome = globalColab?.nome || colab.nome || 'Nome Desconhecido';
          const funcaoFormatada = formatarFuncao(colab, globalColab);
          const chave = `${nome}#${funcaoFormatada}`;
          const pontuacao = Number(colab.pontuacao) || 0;

          if (!pontosConsolidados[chave]) {
            pontosConsolidados[chave] = {
              nome: nome,
              funcao: funcaoFormatada,
              pontos_por_mes: {},
              pontuacao_total: 0
            };
          }

          const pontosMesAtual = pontosConsolidados[chave].pontos_por_mes[mesProjeto] || 0;
          pontosConsolidados[chave].pontos_por_mes[mesProjeto] = pontosMesAtual + pontuacao;
          pontosConsolidados[chave].pontuacao_total += pontuacao;
        }
      }

      const mesesOrdenados = Array.from(mesesPresentes).sort();

      const dadosFinais = Object.values(pontosConsolidados).map(item => {
        const linha: { [key: string]: string | number } = {
          'Nome': item.nome,
          'Função': item.funcao
        };
        for (const mes of mesesOrdenados) {
          linha[`Pontos ${mes}`] = item.pontos_por_mes[mes] || 0;
        }
        linha['Pontuação Total'] = item.pontuacao_total;
        return linha;
      }).sort((a, b) => String(a.Nome).localeCompare(String(b.Nome)) || String(a.Função).localeCompare(String(b.Função)));

      if (dadosFinais.length === 0) {
        alert('Nenhum colaborador encontrado nos projetos selecionados.');
        return;
      }

      const ws = XLSX.utils.json_to_sheet(dadosFinais);
      const header = ['Nome', 'Função', ...mesesOrdenados.map(mes => ` ${mes}`), 'Pontuação Total'];
      XLSX.utils.sheet_add_aoa(ws, [header], { origin: 'A1' });

      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Pontos Consolidados por Mês');

      const dateStr = new Date().toISOString().split('T')[0];
      const fileName = `pontos_consolidados_por_mes_${selectedProjectIds.length}_projetos_${dateStr}.xlsx`;

      const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
      saveAs(new Blob([wbout]), fileName);

    } catch (error) {
      console.error('Erro ao exportar dados consolidados por mês:', error);
      alert('Ocorreu um erro ao gerar a planilha por mês.');
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <Box sx={{ p: 4, maxWidth: 800, margin: '0 auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Gestão de Projetos
        </Typography>
        <Box>
          <Button
            variant="contained"
            onClick={() => setModalOpen(true)}
            sx={{ mr: 2 }}
          >
            Novo Projeto
          </Button>
          <Button
            variant="contained"
            color="secondary"
            startIcon={isExporting ? <CircularProgress size={20} color="inherit" /> : <DownloadIcon />}
            onClick={handleExportSelectedMonthly}
            disabled={selectedProjectIds.length === 0 || isExporting}
          >
            {isExporting ? 'Exportando...' : 'Exportar por Mês'}
          </Button>
        </Box>
      </Box>

      {projects.length > 0 ? (
        <List sx={{ bgcolor: 'background.paper', borderRadius: 2, boxShadow: 1, border: '1px solid', borderColor: 'divider' }}>
          {projects.map((project) => (
            <ListItem
              sx={{ borderBottom: '1px solid', borderColor: 'divider' }}
              key={project.id}
              disablePadding
              secondaryAction={
                <IconButton
                  edge="end"
                  aria-label="delete"
                  onClick={(e) => handleDelete(project.id!, e)}
                  color="error"
                >
                  <Delete fontSize="small" />
                </IconButton>
              }
            >
              <ListItemButton
                role={undefined}
                onClick={() => handleProjectSelect(project.id!)}
                dense
                sx={{ pr: 8 }}
              >
                <ListItemText
                  id={`checkbox-list-label-${project.id}`}
                  primary={project.name}
                  secondary={`Data: ${project.month || 'N/A'}`}
                  sx={{ '& .MuiListItemText-secondary': { mt: 0.5 } }}
                />
              </ListItemButton>
              <Checkbox
                edge="start"
                checked={selectedProjectIds.includes(project.id!)}
                onChange={(e) => handleCheckboxChange(project.id!, e.target.checked)}
                inputProps={{ 'aria-labelledby': `checkbox-list-label-${project.id}` }}
                sx={{ marginRight: 5 }}
              />
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