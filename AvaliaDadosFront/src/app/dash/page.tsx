'use client';

import { useProjects } from '@/context/ProjectContext';
import { is24hCollaborator } from '@/components/utils';
import { MedicoRole, ProjectCollaborator, ShiftHours, WorkPeriod } from '@/types/project';
import {
  AccessTimeOutlined,
  AllInclusiveOutlined,
  EmojiEventsOutlined,
  GroupsOutlined,
  LocalHospitalOutlined,
  LocalShippingOutlined,
  NightsStayOutlined,
  SupportAgentOutlined,
  WbSunnyOutlined,
} from '@mui/icons-material';
import {
  Alert,
  alpha,
  Avatar,
  Box,
  Chip,
  FormControl,
  Grid,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

// ---------------------------------------------------------------------------
// Paleta
// ---------------------------------------------------------------------------

const COLORS = {
  ink: '#12323b',
  teal: '#0f766e',
  mint: '#2dd4bf',
  amber: '#f59e0b',
  coral: '#ef6a5b',
  blue: '#4f7cac',
  violet: '#8b6dd6',
  sand: '#f4efe5',
};

// Cor usada especificamente pra marcar quem é 24h (não é Diurno nem Noturno)
const COLOR_24H = COLORS.violet;

type GroupKey = 'TARM' | 'FROTA' | 'MEDICO_LIDER' | 'MEDICO_REGULADOR';

const GROUP_META: Record<GroupKey, { title: string; short: string; color: string; icon: React.ReactNode }> = {
  TARM: { title: 'TARM', short: 'TARM', color: COLORS.teal, icon: <SupportAgentOutlined fontSize="small" /> },
  FROTA: { title: 'Frota', short: 'Frota', color: COLORS.violet, icon: <LocalShippingOutlined fontSize="small" /> },
  MEDICO_LIDER: { title: 'Médico Líder', short: 'Méd. Líder', color: COLORS.coral, icon: <LocalHospitalOutlined fontSize="small" /> },
  MEDICO_REGULADOR: { title: 'Médico Regulador', short: 'Méd. Regulador', color: COLORS.amber, icon: <LocalHospitalOutlined fontSize="small" /> },
};

const GROUP_ORDER: GroupKey[] = ['TARM', 'FROTA', 'MEDICO_LIDER', 'MEDICO_REGULADOR'];

function groupKeyFor(role: string, medicoRole?: MedicoRole): GroupKey | null {
  if (role === 'MEDICO') {
    if (medicoRole === MedicoRole.LIDER) return 'MEDICO_LIDER';
    if (medicoRole === MedicoRole.REGULADOR) return 'MEDICO_REGULADOR';
    return null;
  }
  if (role === 'TARM') return 'TARM';
  if (role === 'FROTA') return 'FROTA';
  return null;
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const decimalFormatter = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

// ---------------------------------------------------------------------------
// Summary card (estilo hero, com decoração)
// ---------------------------------------------------------------------------

interface SummaryCardProps {
  eyebrow: string;
  value: string;
  detail: string;
  color: string;
  icon: React.ReactNode;
  delay: number;
}

function SummaryCard({ eyebrow, value, detail, color, icon, delay }: SummaryCardProps) {
  return (
    <Paper
      elevation={0}
      sx={{
        height: '100%',
        p: 2.5,
        borderRadius: '18px',
        border: '1px solid',
        borderColor: alpha(color, 0.2),
        background: `linear-gradient(145deg, ${alpha(color, 0.08)}, ${alpha('#ffffff', 0.94)})`,
        position: 'relative',
        overflow: 'hidden',
        animation: 'dash-rise .5s ease both',
        animationDelay: `${delay}ms`,
        '&::after': {
          content: '""',
          position: 'absolute',
          width: 80,
          height: 80,
          borderRadius: '50%',
          right: -30,
          top: -30,
          background: alpha(color, 0.1),
        },
      }}
    >
      <Stack direction="row" sx={{ alignItems: 'flex-start', justifyContent: 'space-between', gap: 2 }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="overline" sx={{ color: 'text.secondary', letterSpacing: '0.11em', fontWeight: 800 }}>
            {eyebrow}
          </Typography>
          <Typography sx={{ fontSize: { xs: '1.6rem', lg: '1.9rem' }, lineHeight: 1.1, fontWeight: 900, mt: 0.5 }}>
            {value}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.8 }} noWrap>
            {detail}
          </Typography>
        </Box>
        <Box
          sx={{
            width: 42,
            height: 42,
            display: 'grid',
            placeItems: 'center',
            borderRadius: '13px',
            color,
            bgcolor: alpha(color, 0.12),
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
      </Stack>
    </Paper>
  );
}

// ---------------------------------------------------------------------------
// Painel de um grupo (função) selecionado
// ---------------------------------------------------------------------------

function GroupPanel({ groupKey, collaborators }: { groupKey: GroupKey; collaborators: ProjectCollaborator[] }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const meta = GROUP_META[groupKey];
  const isMedico = groupKey === 'MEDICO_LIDER' || groupKey === 'MEDICO_REGULADOR';
  const border = alpha(theme.palette.text.primary, 0.09);

  // 24h (Médico Líder/Regulador com turno H24, ou Supervisor) não é Diurno nem Noturno —
  // fica de fora dos dois buckets e ganha uma tabela própria.
  const h24 = collaborators.filter(c => is24hCollaborator(c.role, c.medicoRole, c.shiftHours));
  const day = collaborators.filter(c => !is24hCollaborator(c.role, c.medicoRole, c.shiftHours) && (c.workPeriod || WorkPeriod.DIURNO) === WorkPeriod.DIURNO);
  const night = collaborators.filter(c => !is24hCollaborator(c.role, c.medicoRole, c.shiftHours) && (c.workPeriod || WorkPeriod.DIURNO) === WorkPeriod.NOTURNO);

  const ranked = [...collaborators].sort((a, b) => (Number(b.pontuacao) || 0) - (Number(a.pontuacao) || 0));
  const total = ranked.reduce((sum, c) => sum + (Number(c.pontuacao) || 0), 0);
  const average = ranked.length ? total / ranked.length : 0;

  // Pódio: só o top 5 vira gráfico (com ~30 pessoas por grupo, um gráfico com todo mundo fica ilegível)
  const podiumData = ranked.slice(0, 5).map(c => ({
    nome: c.nome.split(' ')[0],
    pontos: Number(c.pontuacao) || 0,
    periodo: c.workPeriod,
    is24h: is24hCollaborator(c.role, c.medicoRole, c.shiftHours),
  }));

  return (
    <Box>
      <Grid container spacing={8} sx={{ mb: 2.5, justifyContent: "center" }}>
        <Grid size={{ xs: 6, sm: 3 }}>
          <SummaryCard
            eyebrow="Profissionais"
            value={String(collaborators.length)}
            detail={h24.length > 0 ? `${day.length} diurno · ${night.length} noturno · ${h24.length} 24h` : `${day.length} diurno · ${night.length} noturno`}
            color={meta.color}
            icon={<GroupsOutlined />}
            delay={0}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <SummaryCard eyebrow="Média por pessoa" value={decimalFormatter.format(average)} detail="pontos / profissional" color={COLORS.blue} icon={<AccessTimeOutlined />} delay={120} />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <SummaryCard eyebrow="Líder do grupo" value={ranked[0]?.nome.split(' ')[0] || '—'} detail={ranked[0] ? `${ranked[0].pontuacao} pontos` : 'sem dados'} color={COLORS.coral} icon={<EmojiEventsOutlined />} delay={180} />
        </Grid>
      </Grid>

      {collaborators.length === 0 ? (
        <Alert severity="info" sx={{ borderRadius: '14px' }}>Nenhum profissional cadastrado neste grupo ainda.</Alert>
      ) : (
        <>
          <Paper elevation={0} sx={{ p: { xs: 2, md: 3 }, borderRadius: '22px', border: `1px solid ${border}`, mb: 2.5 }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 1, mb: 1 }}>
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 900 }}>Pódio · Top 5</Typography>
                <Typography variant="body2" color="text.secondary">
                  Os cinco profissionais com mais pontos em {meta.title.toLowerCase()}.
                </Typography>
              </Box>
              <Stack direction="row" spacing={2}>
                <Stack direction="row" spacing={0.75} alignItems="center">
                  <Box sx={{ width: 10, height: 10, borderRadius: '3px', bgcolor: COLORS.amber }} />
                  <Typography variant="caption" color="text.secondary">Diurno</Typography>
                </Stack>
                <Stack direction="row" spacing={0.75} alignItems="center">
                  <Box sx={{ width: 10, height: 10, borderRadius: '3px', bgcolor: COLORS.blue }} />
                  <Typography variant="caption" color="text.secondary">Noturno</Typography>
                </Stack>
                {h24.length > 0 && (
                  <Stack direction="row" spacing={0.75} alignItems="center">
                    <Box sx={{ width: 10, height: 10, borderRadius: '3px', bgcolor: COLOR_24H }} />
                    <Typography variant="caption" color="text.secondary">24h</Typography>
                  </Stack>
                )}
              </Stack>
            </Stack>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={podiumData} layout="vertical" margin={{ top: 5, right: 24, left: isMobile ? 0 : 8, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 5" stroke={border} horizontal={false} />
                <XAxis type="number" tick={{ fill: theme.palette.text.secondary, fontSize: 11 }} allowDecimals={false} />
                <YAxis dataKey="nome" type="category" width={isMobile ? 70 : 90} tick={{ fill: theme.palette.text.secondary, fontSize: 12, fontWeight: 700 }} />
                <Tooltip contentStyle={{ borderRadius: 14, border: `1px solid ${border}` }} />
                <Bar dataKey="pontos" name="Pontos" radius={[0, 6, 6, 0]} maxBarSize={26}>
                  {podiumData.map((entry, index) => (
                    <Cell key={index} fill={entry.is24h ? COLOR_24H : entry.periodo === WorkPeriod.NOTURNO ? COLORS.blue : COLORS.amber} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </Paper>

          <Grid container spacing={2.5}>
            <Grid size={{ xs: 12, md: h24.length > 0 ? 4 : 6 }}>
              <PeriodTable label="Diurno" icon={<WbSunnyOutlined fontSize="small" />} accent={COLORS.amber} isMedico={isMedico} collaborators={day} border={border} />
            </Grid>
            <Grid size={{ xs: 12, md: h24.length > 0 ? 4 : 6 }}>
              <PeriodTable label="Noturno" icon={<NightsStayOutlined fontSize="small" />} accent={COLORS.blue} isMedico={isMedico} collaborators={night} border={border} />
            </Grid>
            {h24.length > 0 && (
              <Grid size={{ xs: 12, md: 4 }}>
                <PeriodTable label="24h" icon={<AllInclusiveOutlined fontSize="small" />} accent={COLOR_24H} isMedico={isMedico} collaborators={h24} border={border} />
              </Grid>
            )}
          </Grid>
        </>
      )}
    </Box>
  );
}

function PeriodTable({
  label,
  icon,
  accent,
  isMedico,
  collaborators,
  border,
}: {
  label: string;
  icon: React.ReactNode;
  accent: string;
  isMedico: boolean;
  collaborators: ProjectCollaborator[];
  border: string;
}) {
  const [search, setSearch] = useState('');
  const ranked = [...collaborators].sort((a, b) => (Number(b.pontuacao) || 0) - (Number(a.pontuacao) || 0));
  const filtered = search.trim()
    ? ranked.filter(c => c.nome.toLowerCase().includes(search.trim().toLowerCase()))
    : ranked;

  return (
    <Paper elevation={0} sx={{ borderRadius: '22px', border: `1px solid ${border}`, overflow: 'hidden', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2.5, py: 2, borderBottom: `1px solid ${border}` }}>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: ranked.length > 8 ? 1.5 : 0 }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Box sx={{ color: accent, display: 'flex' }}>{icon}</Box>
            <Typography sx={{ fontWeight: 900, fontSize: 15 }}>{label}</Typography>
          </Stack>
          <Chip size="small" label={`${ranked.length} pessoas`} sx={{ bgcolor: alpha(accent, 0.12), color: accent, fontWeight: 800 }} />
        </Stack>
        {ranked.length > 8 && (
          <TextField
            size="small"
            fullWidth
            placeholder="Buscar por nome..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px', fontSize: 13.5 } }}
          />
        )}
      </Box>
      {ranked.length === 0 ? (
        <Box sx={{ p: 3 }}>
          <Typography color="text.secondary" variant="body2">Ninguém neste período.</Typography>
        </Box>
      ) : filtered.length === 0 ? (
        <Box sx={{ p: 3 }}>
          <Typography color="text.secondary" variant="body2">Nenhum nome encontrado para &quot;{search}&quot;.</Typography>
        </Box>
      ) : (
        <TableContainer sx={{ maxHeight: 620, flex: 1 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 900, width: 36 }}>#</TableCell>
                <TableCell sx={{ fontWeight: 900 }}>Profissional</TableCell>
                {isMedico && <TableCell sx={{ fontWeight: 900 }}>Turno</TableCell>}
                <TableCell sx={{ fontWeight: 900 }} align="right">Pontos</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((c, index) => (
                <TableRow key={c.id} hover>
                  <TableCell>
                    <Typography variant="caption" sx={{ color: 'text.disabled', fontWeight: 700 }}>{index + 1}</Typography>
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1.25} alignItems="center">
                      <Avatar sx={{ width: 26, height: 26, fontSize: 11, bgcolor: alpha(accent, 0.15), color: accent, fontWeight: 800 }}>
                        {initials(c.nome)}
                      </Avatar>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>{c.nome}</Typography>
                    </Stack>
                  </TableCell>
                  {isMedico && (
                    <TableCell>
                      {c.shiftHours && (
                        <Chip
                          size="small"
                          label={c.shiftHours === ShiftHours.H24 ? '24h' : '12h'}
                          sx={{
                            height: 20,
                            fontSize: 10.5,
                            fontWeight: 800,
                            color: c.shiftHours === ShiftHours.H24 ? COLORS.amber : COLORS.teal,
                            bgcolor: alpha(c.shiftHours === ShiftHours.H24 ? COLORS.amber : COLORS.teal, 0.12),
                          }}
                        />
                      )}
                    </TableCell>
                  )}
                  <TableCell align="right">
                    <Typography sx={{ fontWeight: 900, color: accent }}>{c.pontuacao}</Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Paper>
  );
}

// ---------------------------------------------------------------------------
// Página principal
// ---------------------------------------------------------------------------

export default function ReadOnlyDashboardPage() {
  const { projects, projectCollaborators, actions: { fetchProjectCollaborators } } = useProjects();
  const [projectId, setProjectId] = useState('');
  const [activeGroup, setActiveGroup] = useState<GroupKey>('TARM');

  const orderedProjects = useMemo(() => [...projects].sort((a, b) =>
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  ), [projects]);

  useEffect(() => {
    if (!projectId && orderedProjects[0]?.id) setProjectId(orderedProjects[0].id);
  }, [orderedProjects, projectId]);

  useEffect(() => {
    if (projectId && !projectCollaborators[projectId]) {
      fetchProjectCollaborators(projectId).catch(console.error);
    }
  }, [projectId, projectCollaborators, fetchProjectCollaborators]);

  const collaborators = useMemo(() => projectCollaborators[projectId] || [], [projectCollaborators, projectId]);

  const byGroup = useMemo(() => {
    const map = new Map<GroupKey, ProjectCollaborator[]>();
    for (const c of collaborators) {
      const key = groupKeyFor(c.role, c.medicoRole);
      if (!key) continue;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(c);
    }
    return map;
  }, [collaborators]);

  const groupCount = (key: GroupKey) => (byGroup.get(key) || []).length;
  const availableGroups = GROUP_ORDER.filter(key => groupCount(key) > 0);

  useEffect(() => {
    if (availableGroups.length > 0 && !availableGroups.includes(activeGroup)) {
      setActiveGroup(availableGroups[0]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, availableGroups.join(',')]);

  const outros = collaborators.filter(c => !groupKeyFor(c.role, c.medicoRole));
  const handleTabChange = (_: SyntheticEvent, value: GroupKey) => setActiveGroup(value);
  const currentProject = orderedProjects.find(p => p.id === projectId);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: '#f7f5f0',
        p: { xs: 2, md: 4 },
        '@keyframes dash-rise': {
          from: { opacity: 0, transform: 'translateY(12px)' },
          to: { opacity: 1, transform: 'translateY(0)' },
        },
      }}
    >
      <Box sx={{ maxWidth: 1320, mx: 'auto' }}>
        {/* Hero */}
        <Paper
          elevation={0}
          sx={{
            position: 'relative',
            overflow: 'hidden',
            p: { xs: 2.5, md: 4 },
            mb: 3,
            borderRadius: '26px',
            color: '#f7fffd',
            background: `linear-gradient(120deg, ${COLORS.amber} 0%, ${COLORS.blue} 62%, #169c8e 100%)`,
            '&::before': {
              content: '""',
              position: 'absolute',
              width: 300,
              height: 300,
              borderRadius: '50%',
              right: -95,
              top: -175,
              border: `42px solid ${alpha('#ffffff', 0.08)}`,
            },
            '&::after': {
              content: '""',
              position: 'absolute',
              width: 180,
              height: 180,
              borderRadius: '50%',
              right: 140,
              bottom: -145,
              bgcolor: alpha(COLORS.amber, 0.22),
            },
          }}
        >
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            sx={{ alignItems: { xs: 'stretch', md: 'flex-end' }, justifyContent: 'space-between', gap: 3, position: 'relative', zIndex: 1 }}
          >
            <Box>
              <Stack direction="row" sx={{ alignItems: 'center', gap: 1, mb: 1.2 }}>
                <EmojiEventsOutlined sx={{ color: COLORS.mint }} />
                <Typography variant="overline" sx={{ color: alpha('#ffffff', 0.78), letterSpacing: '0.14em', fontWeight: 900 }}>
                  Avaliação de desempenho
                </Typography>
              </Stack>
              <Typography component="h1" sx={{ fontFamily: "Georgia, 'Times New Roman', serif", fontSize: { xs: '2rem', md: '3rem' }, lineHeight: 1, fontWeight: 700 }}>
                Dashboard SAMU
              </Typography>
              <Typography sx={{ mt: 1.5, maxWidth: 650, color: alpha('#ffffff', 0.76) }}>
                Ranking por função e período, com o detalhamento de pontos de cada equipe.
              </Typography>
            </Box>

            <Box sx={{ minWidth: { md: 280 } }}>
              <Typography variant="caption" sx={{ display: 'block', mb: 0.7, color: alpha('#ffffff', 0.72), fontWeight: 800 }}>
                Projeto
              </Typography>
              <FormControl fullWidth size="small">
                <Select
                  value={projectId}
                  onChange={event => setProjectId(event.target.value)}
                  displayEmpty
                  sx={{
                    color: '#fff',
                    fontWeight: 800,
                    bgcolor: alpha('#ffffff', 0.12),
                    borderRadius: '12px',
                    '& .MuiOutlinedInput-notchedOutline': { borderColor: alpha('#ffffff', 0.28) },
                    '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: alpha('#ffffff', 0.5) },
                    '& .MuiSvgIcon-root': { color: '#fff' },
                  }}
                >
                  {orderedProjects.map(project => (
                    <MenuItem key={project.id} value={project.id}>{project.name} — {project.month}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          </Stack>
        </Paper>

        {!currentProject ? (
          <Alert severity="info" sx={{ borderRadius: '14px' }}>Selecione um projeto para visualizar o dashboard.</Alert>
        ) : availableGroups.length === 0 ? (
          <Alert severity="info" sx={{ borderRadius: '14px' }}>Nenhum profissional cadastrado neste projeto ainda.</Alert>
        ) : (
          <>
            <Paper elevation={0} sx={{ borderRadius: '18px', border: '1px solid', borderColor: alpha(COLORS.ink, 0.09), mb: 2.5, overflow: 'hidden' }}>
              <Tabs
                value={activeGroup}
                onChange={handleTabChange}
                variant="scrollable"
                scrollButtons="auto"
                sx={{
                  bgcolor: alpha(COLORS.sand, 0.5),
                  px: 1,
                  minHeight: 56,
                  '& .MuiTabs-indicator': { height: 3, borderRadius: '3px 3px 0 0', bgcolor: GROUP_META[activeGroup].color },
                }}
              >
                {availableGroups.map(key => {
                  const meta = GROUP_META[key];
                  return (
                    <Tab
                      key={key}
                      value={key}
                      label={
                        <Stack direction="row" spacing={0.75} alignItems="center">
                          <Box sx={{ display: 'flex', color: activeGroup === key ? meta.color : 'text.disabled' }}>{meta.icon}</Box>
                          <span>{meta.short}</span>
                          <Chip
                            size="small"
                            label={groupCount(key)}
                            sx={{
                              height: 19,
                              fontSize: 10.5,
                              fontWeight: 800,
                              bgcolor: activeGroup === key ? alpha(meta.color, 0.15) : alpha(COLORS.ink, 0.06),
                              color: activeGroup === key ? meta.color : 'text.secondary',
                            }}
                          />
                        </Stack>
                      }
                      sx={{ textTransform: 'none', fontWeight: 800, fontSize: 13.5, minHeight: 56, '&.Mui-selected': { color: meta.color } }}
                    />
                  );
                })}
              </Tabs>
            </Paper>

            <GroupPanel groupKey={activeGroup} collaborators={byGroup.get(activeGroup) || []} />
          </>
        )}

        {outros.length > 0 && (
          <Paper elevation={0} sx={{ mt: 2.5, borderRadius: '18px', border: '1px solid', borderColor: alpha(COLORS.ink, 0.09), p: 2.5 }}>
            <Typography sx={{ fontWeight: 800, fontSize: 13.5, color: 'text.secondary', mb: 1.25 }}>
              Sem função classificada ({outros.length})
            </Typography>
            <Stack direction="row" flexWrap="wrap" gap={1}>
              {outros.map(c => (
                <Chip key={c.id} label={`${c.nome} · ${c.role} · ${c.pontuacao} pts`} sx={{ bgcolor: alpha(COLORS.ink, 0.06) }} />
              ))}
            </Stack>
          </Paper>
        )}
      </Box>
    </Box>
  );
}
