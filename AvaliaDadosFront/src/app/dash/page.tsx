'use client';

import { is24hCollaborator } from '@/components/utils';
import { useProjects } from '@/context/ProjectContext';
import { MedicoRole, ProjectCollaborator, WorkPeriod } from '@/types/project';
import {
  AllInclusiveOutlined,
  BarChartOutlined,
  CalendarMonthOutlined,
  EmojiEventsOutlined,
  GroupsOutlined,
  InsightsOutlined,
  LocalHospitalOutlined,
  LocalShippingOutlined,
  NightsStayOutlined,
  SupportAgentOutlined,
  WbSunnyOutlined,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Checkbox,
  FormControl,
  MenuItem,
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
  type SelectChangeEvent,
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
// Identidade visual: mesmo padrão do site do Protocolo CISBAF (protocolo.cisbaf.org.br)
// e do site institucional (cisbaf.org.br): azul, vermelho e amarelo da marca sobre tons slate.
// ---------------------------------------------------------------------------

const COLORS = {
  navy: '#0f172a', // slate-900: títulos e texto forte
  ink: '#1e293b', // slate-800: texto corrente
  muted: '#475569', // slate-600: texto secundário
  line: '#e2e8f0', // slate-200: bordas
  input: '#cbd5e1', // slate-300: bordas de campos
  soft: '#f1f5f9', // slate-100: fundos suaves
  page: '#f3f4f6', // gray-100: fundo da página, igual ao painel admin do Protocolo
  gray50: '#f9fafb',
  gray500: '#6b7280',
  blueLight: '#3b82f6', // blue-500: selo do cabeçalho do painel
  paper: '#ffffff',
  blue: '#2563eb', // azul principal do Protocolo
  blueDark: '#1a4a8e', // azul do site institucional
  bluePale: '#eff6ff',
  red: '#991919', // título "Protocolo Cisbaf"
  yellow: '#ca8a04', // subtítulo em maiúsculas
  day: '#ca8a04',
  night: '#2563eb',
  h24: '#991919',
};

const FONT_STACK = 'var(--font-geist-sans), Inter, "Segoe UI", Arial, sans-serif';
const TABULAR = { fontVariantNumeric: 'tabular-nums' } as const;
const CONTENT_WIDTH = 1240;

// Painel padrão: branco, borda fina, cantos de 16px e sombra leve, como os cards do Protocolo
const panelSx = {
  bgcolor: COLORS.paper,
  border: `1px solid ${COLORS.line}`,
  borderRadius: '24px',
  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.06)',
  overflow: 'hidden',
} as const;

type GroupKey = 'TARM' | 'FROTA' | 'MEDICO_LIDER' | 'MEDICO_REGULADOR';

const GROUP_META: Record<GroupKey, { title: string; short: string; icon: React.ReactNode }> = {
  TARM: { title: 'TARM', short: 'TARM', icon: <SupportAgentOutlined fontSize="small" /> },
  FROTA: { title: 'Frota', short: 'Frota', icon: <LocalShippingOutlined fontSize="small" /> },
  MEDICO_LIDER: { title: 'Médico Líder', short: 'Médico Líder', icon: <LocalHospitalOutlined fontSize="small" /> },
  MEDICO_REGULADOR: { title: 'Médico Regulador', short: 'Médico Regulador', icon: <LocalHospitalOutlined fontSize="small" /> },
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

const decimalFormatter = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

// ---------------------------------------------------------------------------
// Trimestral: soma dos pontos de até 3 meses (projetos), por profissional
// ---------------------------------------------------------------------------

const MAX_MESES = 3;

// Profissional já consolidado. Com um único mês é o próprio registro do projeto (sem porMes).
type ColaboradorConsolidado = ProjectCollaborator & {
  porMes?: Array<number | null>; // pontos em cada mês selecionado (null = sem registro naquele mês)
  mesesComRegistro?: number;
};

const NOMES_DOS_MESES = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];

// O mês do projeto é gravado como "MM-AAAA" (ex.: 07-2026); "AAAA-MM" também é aceito.
function parseMes(month: string): { ano: number; mes: number } | null {
  const partes = (month || '').split('-');
  if (partes.length !== 2) return null;
  const [a, b] = partes.map(Number);
  if (!a || !b) return null;
  const ano = partes[0].length === 4 ? a : b;
  const mes = partes[0].length === 4 ? b : a;
  return mes >= 1 && mes <= 12 ? { ano, mes } : null;
}

// Chave para ordenar cronologicamente (ano primeiro).
function chaveMes(month: string): number {
  const p = parseMes(month);
  return p ? p.ano * 100 + p.mes : 0;
}

function formatMes(month: string): string {
  const p = parseMes(month);
  return p ? `${String(p.mes).padStart(2, '0')}/${p.ano}` : month || '—';
}

function formatMesExtenso(month: string): string {
  const p = parseMes(month);
  return p ? `${NOMES_DOS_MESES[p.mes - 1]} de ${p.ano}` : month || '—';
}

function listaEmPortugues(itens: string[]): string {
  if (itens.length <= 1) return itens.join('');
  return `${itens.slice(0, -1).join(', ')} e ${itens[itens.length - 1]}`;
}

// Média mensal = soma dos pontos ÷ quantidade de meses em que o profissional tem registro.
function mediaMensal(c: ColaboradorConsolidado): number {
  return (Number(c.pontuacao) || 0) / (c.mesesComRegistro || 1);
}

// idsOrdenados: projetos selecionados em ordem cronológica. A chave é o id do colaborador (o mesmo
// em todos os projetos); função e período vêm do mês mais recente em que ele aparece.
function consolidar(
  idsOrdenados: string[],
  porProjeto: Record<string, ProjectCollaborator[] | undefined>,
): ColaboradorConsolidado[] {
  if (idsOrdenados.length <= 1) return porProjeto[idsOrdenados[0]] || [];

  const mapa = new Map<string, ColaboradorConsolidado>();
  idsOrdenados.forEach((projetoId, indice) => {
    for (const c of porProjeto[projetoId] || []) {
      const chave = c.id ?? c.nome;
      const anterior = mapa.get(chave);
      const pontos = Number(c.pontuacao) || 0;
      const porMes = anterior?.porMes ?? idsOrdenados.map(() => null as number | null);
      porMes[indice] = pontos;
      mapa.set(chave, {
        ...c,
        pontuacao: (Number(anterior?.pontuacao) || 0) + pontos,
        porMes,
        mesesComRegistro: (anterior?.mesesComRegistro || 0) + 1,
      });
    }
  });
  return [...mapa.values()];
}

// ---------------------------------------------------------------------------
// Indicador (uma célula da faixa de indicadores)
// ---------------------------------------------------------------------------

function Indicator({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: string; detail: string }) {
  return (
    <Box
      sx={{
        ...panelSx,
        borderRadius: '20px',
        p: 3,
        minWidth: 0,
        transition: 'transform 0.2s ease, box-shadow 0.2s ease',
        '&:hover': { transform: 'translateY(-2px)', boxShadow: '0 10px 15px -3px rgba(15,23,42,0.1), 0 4px 6px -4px rgba(15,23,42,0.08)' },
      }}
    >
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', mb: 1.5 }}>
        <Box sx={{ display: 'flex', p: 1, borderRadius: '12px', bgcolor: COLORS.bluePale, color: COLORS.blue }}>{icon}</Box>
        <Typography sx={{ fontSize: 11.5, fontWeight: 900, letterSpacing: '0.06em', textTransform: 'uppercase', color: COLORS.gray500 }}>{label}</Typography>
      </Stack>
      <Typography
        sx={{ fontSize: { xs: 28, md: 32 }, fontWeight: 900, lineHeight: 1.15, letterSpacing: '-0.02em', color: COLORS.navy, ...TABULAR }}
        noWrap
        title={value}
      >
        {value}
      </Typography>
      <Typography sx={{ mt: 0.5, fontSize: 13, color: COLORS.muted }} noWrap title={detail}>{detail}</Typography>
    </Box>
  );
}

// ---------------------------------------------------------------------------
// Painel de um grupo (função) selecionado
// ---------------------------------------------------------------------------

function GroupPanel({ groupKey, collaborators, meses = [] }: { groupKey: GroupKey; collaborators: ColaboradorConsolidado[]; meses?: string[] }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const meta = GROUP_META[groupKey];
  const trimestral = meses.length > 1;

  // 24h (somente Supervisor) não é Diurno nem Noturno — fica de fora dos dois buckets e ganha uma tabela própria.
  // Médicos são 12h: entram em Diurno ou Noturno pelo período escolhido no cadastro.
  const h24 = collaborators.filter(c => is24hCollaborator(c.role, c.medicoRole, c.shiftHours));
  const day = collaborators.filter(c => !is24hCollaborator(c.role, c.medicoRole, c.shiftHours) && (c.workPeriod || WorkPeriod.DIURNO) !== WorkPeriod.NOTURNO);
  const night = collaborators.filter(c => !is24hCollaborator(c.role, c.medicoRole, c.shiftHours) && c.workPeriod === WorkPeriod.NOTURNO);

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

  const periodos = [
    { label: 'Diurno', color: COLORS.day },
    { label: 'Noturno', color: COLORS.night },
    ...(h24.length > 0 ? [{ label: '24h', color: COLORS.h24 }] : []),
  ];

  return (
    <Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' }, gap: 2.5, mb: 3 }}>
        <Indicator
          icon={<GroupsOutlined />}
          label="Profissionais"
          value={String(collaborators.length)}
          detail={h24.length > 0 ? `${day.length} diurno, ${night.length} noturno e ${h24.length} 24h` : `${day.length} diurno e ${night.length} noturno`}
        />
        <Indicator
          icon={<InsightsOutlined />}
          label="Média por pessoa"
          value={decimalFormatter.format(average)}
          detail={trimestral ? 'soma do período por profissional' : 'pontos por profissional'}
        />
        <Indicator
          icon={<EmojiEventsOutlined />}
          label="Maior pontuação"
          value={ranked[0]?.nome || '—'}
          detail={ranked[0] ? `${ranked[0].pontuacao} pontos${trimestral ? ' somados' : ''}` : 'sem dados'}
        />
      </Box>

      {collaborators.length === 0 ? (
        <Alert severity="info" variant="outlined" sx={{ borderRadius: '12px' }}>Nenhum profissional cadastrado neste grupo ainda.</Alert>
      ) : (
        <>
          <Box sx={{ ...panelSx, p: { xs: 2, md: 3 }, mb: 3 }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 1, mb: 1.5 }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                <Box sx={{ display: 'flex', p: 1, borderRadius: '12px', bgcolor: COLORS.bluePale, color: COLORS.blue }}><BarChartOutlined /></Box>
                <Box>
                  <Typography component="h2" sx={{ fontSize: 17, fontWeight: 800, color: COLORS.navy }}>Cinco maiores pontuações</Typography>
                  <Typography sx={{ mt: 0.25, fontSize: 13.5, color: COLORS.muted }}>
                    {meta.title}, em pontos {trimestral ? 'somados no período' : 'no mês'}.
                  </Typography>
                </Box>
              </Stack>
              <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                {periodos.map(periodo => (
                  <Stack key={periodo.label} direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: '3px', bgcolor: periodo.color }} />
                    <Typography sx={{ fontSize: 12.5, color: COLORS.muted }}>{periodo.label}</Typography>
                  </Stack>
                ))}
              </Stack>
            </Stack>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={podiumData} layout="vertical" margin={{ top: 5, right: 24, left: isMobile ? 0 : 8, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 4" stroke={COLORS.line} horizontal={false} />
                <XAxis type="number" tick={{ fill: COLORS.muted, fontSize: 11 }} allowDecimals={false} axisLine={{ stroke: COLORS.line }} tickLine={false} />
                <YAxis dataKey="nome" type="category" width={isMobile ? 70 : 90} tick={{ fill: COLORS.ink, fontSize: 12.5, fontWeight: 600 }} axisLine={{ stroke: COLORS.line }} tickLine={false} />
                <Tooltip cursor={{ fill: COLORS.soft }} contentStyle={{ borderRadius: 12, border: `1px solid ${COLORS.line}`, boxShadow: '0 4px 12px rgba(15,23,42,0.08)' }} />
                <Bar dataKey="pontos" name="Pontos" radius={[0, 6, 6, 0]} maxBarSize={24}>
                  {podiumData.map((entry, index) => (
                    <Cell key={index} fill={entry.is24h ? COLORS.h24 : entry.periodo === WorkPeriod.NOTURNO ? COLORS.night : COLORS.day} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </Box>

          <Stack direction={{ xs: 'column', md: trimestral ? 'column' : 'row' }} spacing={2.5} sx={{ alignItems: 'stretch' }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <PeriodTable label="Diurno" icon={<WbSunnyOutlined fontSize="small" />} accent={COLORS.day} collaborators={day} meses={meses} />
            </Box>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <PeriodTable label="Noturno" icon={<NightsStayOutlined fontSize="small" />} accent={COLORS.night} collaborators={night} meses={meses} />
            </Box>
            {h24.length > 0 && (
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <PeriodTable label="24h" icon={<AllInclusiveOutlined fontSize="small" />} accent={COLORS.h24} collaborators={h24} meses={meses} />
              </Box>
            )}
          </Stack>
          {trimestral && (
            <Typography sx={{ mt: 1.5, fontSize: 12.5, color: COLORS.muted }}>
              Trimestral ({meses.join(', ')}): <strong>Soma</strong> é o total de pontos nos meses selecionados.{' '}
              <strong>Média mensal</strong> é a soma dividida pelos meses em que o profissional tem registro (&quot;—&quot; indica mês sem registro).
            </Typography>
          )}
        </>
      )}
    </Box>
  );
}

function PeriodTable({
  label,
  icon,
  accent,
  collaborators,
  meses,
}: {
  label: string;
  icon: React.ReactNode;
  accent: string;
  collaborators: ColaboradorConsolidado[];
  meses: string[];
}) {
  const trimestral = meses.length > 1;
  const [search, setSearch] = useState('');
  const ranked = [...collaborators].sort((a, b) => (Number(b.pontuacao) || 0) - (Number(a.pontuacao) || 0));
  const filtered = search.trim()
    ? ranked.filter(c => c.nome.toLowerCase().includes(search.trim().toLowerCase()))
    : ranked;

  const headCell = { fontWeight: 900, fontSize: 11.5, letterSpacing: '0.05em', textTransform: 'uppercase', color: COLORS.gray500, bgcolor: COLORS.gray50, borderBottom: `1px solid ${COLORS.line}`, whiteSpace: 'nowrap' } as const;
  const bodyCell = { borderBottom: `1px solid ${COLORS.soft}`, fontSize: 14, color: COLORS.ink, transition: 'background-color 0.2s' } as const;
  const medalhas = ['#d4a017', '#94a3b8', '#b45309'];

  return (
    <Box sx={{ ...panelSx, borderTop: `4px solid ${accent}`, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2.5, py: 2, borderBottom: `1px solid ${COLORS.line}` }}>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: ranked.length > 8 ? 1.5 : 0 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Box sx={{ color: accent, display: 'flex', p: 1, borderRadius: '12px', bgcolor: `${accent}1a` }}>{icon}</Box>
            <Typography component="h3" sx={{ fontWeight: 800, fontSize: 16, color: COLORS.navy }}>{label}</Typography>
          </Stack>
          <Box sx={{ px: 1.5, py: 0.4, borderRadius: '999px', bgcolor: COLORS.soft, fontSize: 12, fontWeight: 800, color: COLORS.muted }}>
            {ranked.length} {ranked.length === 1 ? 'profissional' : 'profissionais'}
          </Box>
        </Stack>
        {ranked.length > 8 && (
          <TextField
            size="small"
            fullWidth
            placeholder="Buscar por nome"
            value={search}
            onChange={e => setSearch(e.target.value)}
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '12px', fontSize: 13.5, '& fieldset': { borderColor: COLORS.input, borderWidth: 2 }, '&.Mui-focused fieldset': { borderColor: COLORS.blue } } }}
          />
        )}
      </Box>
      {ranked.length === 0 ? (
        <Box sx={{ p: 3 }}>
          <Typography sx={{ fontSize: 14, color: COLORS.muted }}>Ninguém neste período.</Typography>
        </Box>
      ) : filtered.length === 0 ? (
        <Box sx={{ p: 3 }}>
          <Typography sx={{ fontSize: 14, color: COLORS.muted }}>Nenhum nome encontrado para &quot;{search}&quot;.</Typography>
        </Box>
      ) : (
        <TableContainer sx={{ maxHeight: 620, flex: 1 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={{ ...headCell, width: 40 }}>Pos.</TableCell>
                <TableCell sx={headCell}>Profissional</TableCell>
                {trimestral && meses.map(mes => (
                  <TableCell key={mes} sx={headCell} align="right">{mes}</TableCell>
                ))}
                <TableCell sx={{ ...headCell, color: COLORS.blue, fontWeight: 700 }} align="right">{trimestral ? 'Soma' : 'Pontos'}</TableCell>
                {trimestral && <TableCell sx={{ ...headCell, fontStyle: 'italic' }} align="right">Média mensal</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((c, index) => (
                <TableRow key={c.id} hover sx={{ '&:hover td': { bgcolor: COLORS.bluePale } }}>
                  <TableCell sx={{ ...bodyCell, color: COLORS.muted, ...TABULAR }}>
                    {(() => {
                      const posicao = ranked.indexOf(c);
                      return posicao < 3 ? (
                        <Box sx={{ width: 24, height: 24, borderRadius: '50%', bgcolor: medalhas[posicao], color: '#fff', fontSize: 12, fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          {posicao + 1}
                        </Box>
                      ) : posicao + 1;
                    })()}
                  </TableCell>
                  <TableCell sx={{ ...bodyCell, fontWeight: 600 }}>
                    <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                      <Box sx={{ width: 30, height: 30, flexShrink: 0, borderRadius: '10px', bgcolor: COLORS.soft, color: COLORS.muted, fontSize: 12, fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        {c.nome.trim().split(/\s+/).slice(0, 2).map(parte => parte[0]).join('').toUpperCase()}
                      </Box>
                      <span>{c.nome}</span>
                    </Stack>
                  </TableCell>
                  {trimestral && meses.map((mes, i) => (
                    <TableCell key={mes} sx={{ ...bodyCell, color: COLORS.muted, ...TABULAR }} align="right">{c.porMes?.[i] ?? '—'}</TableCell>
                  ))}
                  <TableCell sx={{ ...bodyCell, fontWeight: 700, color: COLORS.blue, ...TABULAR }} align="right">{c.pontuacao}</TableCell>
                  {trimestral && (
                    <TableCell sx={{ ...bodyCell, fontStyle: 'italic', ...TABULAR }} align="right">
                      {decimalFormatter.format(mediaMensal(c))}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}

// ---------------------------------------------------------------------------
// Página principal
// ---------------------------------------------------------------------------

export default function ReadOnlyDashboardPage() {
  const { projects, projectCollaborators, actions: { fetchProjectCollaborators } } = useProjects();
  const [projectIds, setProjectIds] = useState<string[]>([]);
  const [activeGroup, setActiveGroup] = useState<GroupKey>('TARM');

  const orderedProjects = useMemo(() => [...projects].sort((a, b) =>
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  ), [projects]);

  useEffect(() => {
    if (projectIds.length === 0 && orderedProjects[0]?.id) setProjectIds([orderedProjects[0].id]);
  }, [orderedProjects, projectIds]);

  // Projetos selecionados em ordem cronológica (mês; desempate pela criação).
  const selectedProjects = useMemo(() =>
    orderedProjects
      .filter(p => p.id && projectIds.includes(p.id))
      .sort((a, b) => chaveMes(a.month) - chaveMes(b.month)
        || new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()),
    [orderedProjects, projectIds]);
  const selectedKey = selectedProjects.map(p => p.id).join(',');
  const trimestral = selectedProjects.length > 1;
  const mesesRotulos = useMemo(() => (trimestral ? selectedProjects.map(p => formatMes(p.month)) : []), [trimestral, selectedProjects]);

  useEffect(() => {
    selectedKey.split(',').filter(Boolean).forEach(id => {
      if (!projectCollaborators[id]) fetchProjectCollaborators(id).catch(console.error);
    });
  }, [selectedKey, projectCollaborators, fetchProjectCollaborators]);

  const carregando = selectedKey.split(',').filter(Boolean).some(id => !projectCollaborators[id]);

  const collaborators = useMemo(
    () => consolidar(selectedKey.split(',').filter(Boolean), projectCollaborators),
    [projectCollaborators, selectedKey],
  );

  const handleProjectsChange = (event: SelectChangeEvent<string[]>) => {
    const value = event.target.value;
    const lista = typeof value === 'string' ? value.split(',') : value;
    if (lista.length === 0) return; // mantém pelo menos um mês selecionado
    setProjectIds(lista.slice(0, MAX_MESES));
  };

  const byGroup = useMemo(() => {
    const map = new Map<GroupKey, ColaboradorConsolidado[]>();
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
  }, [selectedKey, availableGroups.join(',')]);

  const outros = collaborators.filter(c => !groupKeyFor(c.role, c.medicoRole));
  const handleTabChange = (_: SyntheticEvent, value: GroupKey) => setActiveGroup(value);

  const referencia = selectedProjects.length === 0
    ? ''
    : trimestral
      ? `Visão trimestral: soma dos pontos de ${listaEmPortugues(selectedProjects.map(p => formatMesExtenso(p.month)))}.`
      : `Mês de referência: ${formatMesExtenso(selectedProjects[0].month)}.`;

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: COLORS.page,
        backgroundImage: 'radial-gradient(at 0% 0%, rgba(37,99,235,0.07) 0px, transparent 50%), radial-gradient(at 100% 100%, rgba(37,99,235,0.07) 0px, transparent 50%)',
        backgroundAttachment: 'fixed',
        display: 'flex',
        flexDirection: 'column',
        color: COLORS.ink,
        fontFamily: FONT_STACK,
      }}
    >
      {/* Cabeçalho: mesmo do Protocolo CISBAF (branco translúcido, fixo, logo oficial) */}
      <Box
        component="header"
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 1100,
          bgcolor: 'rgba(255,255,255,0.92)',
          backdropFilter: 'blur(12px)',
          borderBottom: `1px solid ${COLORS.line}`,
          boxShadow: '0 10px 15px -3px rgba(15,23,42,0.08), 0 4px 6px -4px rgba(15,23,42,0.06)',
        }}
      >
        <Stack
          direction="row"
          sx={{ maxWidth: CONTENT_WIDTH, mx: '30px', px: { xs: 2, md: 4 }, height: 60, alignItems: 'center', justifyContent: 'space-between', gap: 2 }}
        >
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center', minWidth: 0 }}>
            <Box component="img" src="/cisbaf.png" alt="CISBAF" sx={{ height: 54, width: 54, flexShrink: 0, imageRendering: 'auto' }} />
            <Box sx={{ minWidth: 0 }}>
              <Typography sx={{ fontSize: { xs: 15, md: 21 }, fontWeight: 900, lineHeight: 1.1, color: COLORS.red, fontFamily: FONT_STACK, letterSpacing: '-0.01em' }} noWrap>
                Avaliação SAMU
              </Typography>
              <Typography sx={{ mt: 0.25, fontSize: { xs: 10, md: 11.5 }, fontWeight: 700, letterSpacing: '0.2em', textTransform: 'uppercase', color: COLORS.yellow, fontFamily: FONT_STACK }} noWrap>
                Painel de desempenho
              </Typography>
            </Box>
          </Stack>
        </Stack>
      </Box>

      <Box component="main" sx={{ flex: 1, width: '100%', maxWidth: CONTENT_WIDTH, mx: 'auto', px: { xs: 1, md: 4 }, py: { xs: 2, md: 6 } }}>
        {/* Moldura única: título, período e todo o conteúdo ficam dentro do mesmo cartão */}
        <Box
          sx={{
            bgcolor: COLORS.paper,
            border: '2px solid #d1d5db',
            borderRadius: { xs: '20px', md: '28px' },
            boxShadow: '0 20px 40px -12px rgba(15, 23, 42, 0.2)',
            overflow: 'hidden',
          }}
        >
        {/* Título e período de referência, num bloco só */}
        <Box
          sx={{
            position: 'relative',
            borderBottom: `1px solid ${COLORS.line}`,
            background: `linear-gradient(120deg, ${COLORS.bluePale} 0%, ${COLORS.paper} 55%)`,
            display: 'flex',
            flexDirection: { xs: 'column', md: 'row' },
          }}
        >
          <InsightsOutlined
            aria-hidden
            sx={{ position: 'absolute', right: { xs: -24, md: 'auto' }, left: { md: 520 }, top: { xs: -16, md: -28 }, fontSize: { xs: 150, md: 220 }, color: COLORS.blue, opacity: 0.06, pointerEvents: 'none' }}
          />

          <Box sx={{ position: 'relative', flex: 1, minWidth: 0, p: { xs: 3, md: 4.5 } }}>
            <Stack direction="row" sx={{ gap: 0.5, mb: 2 }}>
              <Box sx={{ width: 36, height: 5, borderRadius: '3px', bgcolor: COLORS.red }} />
              <Box sx={{ width: 18, height: 5, borderRadius: '3px', bgcolor: COLORS.yellow }} />
              <Box sx={{ width: 8, height: 5, borderRadius: '3px', bgcolor: COLORS.blue }} />
            </Stack>
            <Typography component="h1" sx={{ fontSize: { xs: 28, md: 40 }, fontWeight: 900, lineHeight: 1.08, letterSpacing: '-0.03em', color: COLORS.navy, fontFamily: FONT_STACK }}>
              Desempenho por função e período
            </Typography>
            <Typography sx={{ mt: 1.25, fontSize: { xs: 14.5, md: 16 }, color: COLORS.muted, maxWidth: 560 }}>
              {referencia || 'Selecione o período de referência.'}
            </Typography>
            {selectedProjects.length > 0 && (
              <Stack direction="row" sx={{ mt: 2, flexWrap: 'wrap', gap: 1 }}>
                {selectedProjects.map(p => (
                  <Box key={p.id} sx={{ px: 1.5, py: 0.4, borderRadius: '999px', bgcolor: COLORS.paper, border: '1px solid #bfdbfe', color: COLORS.blueDark, fontSize: 12.5, fontWeight: 800, ...TABULAR }}>
                    {formatMes(p.month)}
                  </Box>
                ))}
              </Stack>
            )}
          </Box>

          <Box
            sx={{
              position: 'relative',
              width: { xs: '100%', md: 400 },
              flexShrink: 0,
              p: { xs: 3, md: 4.5 },
              bgcolor: 'rgba(255,255,255,0.75)',
              borderTop: { xs: `1px solid ${COLORS.line}`, md: 'none' },
              borderLeft: { xs: 'none', md: `1px solid ${COLORS.line}` },
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
            }}
          >
            <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center', mb: 1.25 }}>
              <Box sx={{ display: 'flex', p: 0.75, borderRadius: '10px', bgcolor: COLORS.bluePale, color: COLORS.blue }}>
                <CalendarMonthOutlined fontSize="small" />
              </Box>
              <Typography sx={{ fontSize: 11.5, fontWeight: 900, letterSpacing: '0.06em', textTransform: 'uppercase', color: COLORS.gray500 }}>
                Período de referência
              </Typography>
            </Stack>
            <FormControl fullWidth size="small">
              <Select
                multiple
                value={projectIds}
                onChange={handleProjectsChange}
                displayEmpty
                renderValue={selected => {
                  if (selected.length === 1) {
                    const unico = orderedProjects.find(p => p.id === selected[0]);
                    return unico ? `${unico.name} — ${unico.month}` : '';
                  }
                  return `Trimestral: ${selected.length} meses selecionados`;
                }}
                MenuProps={{ slotProps: { paper: { sx: { borderRadius: '12px', mt: 0.5, border: `1px solid ${COLORS.line}`, boxShadow: '0 10px 15px -3px rgba(15,23,42,0.1)' } } } }}
                sx={{
                  bgcolor: COLORS.paper,
                  fontWeight: 600,
                  fontSize: 14.5,
                  borderRadius: '12px',
                  '& .MuiOutlinedInput-notchedOutline': { borderColor: COLORS.input, borderWidth: 2 },
                  '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: COLORS.blue },
                  '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: COLORS.blue, borderWidth: 2 },
                }}
              >
                {orderedProjects.map(project => {
                  const selecionado = projectIds.includes(project.id as string);
                  return (
                    <MenuItem
                      key={project.id}
                      value={project.id}
                      disabled={!selecionado && projectIds.length >= MAX_MESES}
                    >
                      <Checkbox checked={selecionado} size="small" sx={{ mr: 1, p: 0.5, '&.Mui-checked': { color: COLORS.blue } }} />
                      {project.name} — {project.month}
                    </MenuItem>
                  );
                })}
              </Select>
            </FormControl>
            <Typography sx={{ mt: 1.25, fontSize: 12.5, color: COLORS.muted }}>
              Escolha até {MAX_MESES} meses. Com 2 ou 3, os pontos são somados (visão trimestral).
            </Typography>
          </Box>
        </Box>

        <Box sx={{ bgcolor: COLORS.gray50, p: { xs: 2, md: 4 } }}>
            {selectedProjects.length === 0 ? (
              <Alert severity="info" variant="outlined" sx={{ borderRadius: '12px' }}>Selecione um mês para visualizar o dashboard.</Alert>
            ) : carregando ? (
              <Alert severity="info" variant="outlined" sx={{ borderRadius: '12px' }}>Carregando os dados dos meses selecionados...</Alert>
            ) : availableGroups.length === 0 ? (
              <Alert severity="info" variant="outlined" sx={{ borderRadius: '12px' }}>
                Nenhum profissional cadastrado {trimestral ? 'nos meses selecionados' : 'neste projeto'} ainda.
              </Alert>
            ) : (
              <>
                <Box sx={{ ...panelSx, mb: 3 }}>
                  <Tabs
                    value={activeGroup}
                    onChange={handleTabChange}
                    variant="scrollable"
                    scrollButtons="auto"
                    sx={{
                      px: 1,
                      minHeight: 52,
                      '& .MuiTabs-indicator': { height: 3, borderRadius: '3px 3px 0 0', bgcolor: COLORS.blue },
                    }}
                  >
                    {availableGroups.map(key => {
                      const meta = GROUP_META[key];
                      return (
                        <Tab
                          key={key}
                          value={key}
                          label={
                            <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                              <Box sx={{ display: 'flex', color: activeGroup === key ? COLORS.blue : COLORS.muted }}>{meta.icon}</Box>
                              <span>{meta.short}</span>
                              <Box
                                component="span"
                                sx={{
                                  ml: 0.5,
                                  px: 0.9,
                                  py: 0.1,
                                  borderRadius: '10px',
                                  fontSize: 12,
                                  fontWeight: 700,
                                  bgcolor: activeGroup === key ? COLORS.blue : COLORS.soft,
                                  color: activeGroup === key ? '#fff' : COLORS.muted,
                                  ...TABULAR,
                                }}
                              >
                                {groupCount(key)}
                              </Box>
                            </Stack>
                          }
                          sx={{
                            textTransform: 'none',
                            fontFamily: FONT_STACK,
                            fontWeight: 600,
                            fontSize: 14.5,
                            minHeight: 52,
                            color: COLORS.muted,
                            '&.Mui-selected': { color: COLORS.blue, fontWeight: 700 },
                          }}
                        />
                      );
                    })}
                  </Tabs>
                </Box>

                <GroupPanel groupKey={activeGroup} collaborators={byGroup.get(activeGroup) || []} meses={mesesRotulos} />
              </>
            )}

            {outros.length > 0 && (
              <Box sx={{ ...panelSx, mt: 3, p: 2.5 }}>
                <Typography sx={{ fontWeight: 800, fontSize: 14, color: COLORS.navy, mb: 1.25 }}>
                  Sem função classificada ({outros.length})
                </Typography>
                <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
                  {outros.map(c => (
                    <Box key={c.id} sx={{ px: 1.5, py: 0.5, borderRadius: '999px', bgcolor: COLORS.soft, fontSize: 13, color: COLORS.ink }}>
                      {c.nome}, {c.role}, {c.pontuacao} pontos
                    </Box>
                  ))}
                </Stack>
              </Box>
            )}
        </Box>
        </Box>
      </Box>

      <Box component="footer" sx={{ bgcolor: COLORS.paper, borderTop: `1px solid ${COLORS.line}` }}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          sx={{ maxWidth: CONTENT_WIDTH, mx: 'auto', px: { xs: 2, md: 4 }, py: 2.5, alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between', gap: 1 }}
        >
          <Typography sx={{ fontSize: 13, fontWeight: 700, color: COLORS.blueDark }}>
            CISBAF – Consórcio Intermunicipal de Saúde da Baixada Fluminense
          </Typography>
          <Typography sx={{ fontSize: 12.5, color: COLORS.muted }}>
            © {new Date().getFullYear()} Avaliação SAMU. Painel de consulta de desempenho.
          </Typography>
        </Stack>
      </Box>
    </Box>
  );
}
