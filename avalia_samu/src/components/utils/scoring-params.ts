import { NestedScoringParameters, ScoringRule } from '@/types/project';

export const DEFAULT_PARAMS: NestedScoringParameters = {
    tarm: {
        removidos: [
            { quantity: 1, points: 6 },
            { quantity: 2, points: 0 }
        ],
        regulacao: [
            { duration: '00:02:00', points: 10 },
            { duration: '00:02:15', points: 7 },
            { duration: '00:02:30', points: 4 },
            { duration: '00:02:45', points: 1 },
            { duration: '00:02:46', points: 0 }
        ],
        pausas: [
            { duration: '02:15:00', points: 8 },
            { duration: '02:17:00', points: 6 },
            { duration: '02:19:00', points: 4 },
            { duration: '02:21:00', points: 2 },
            { duration: '02:22:00', points: 0 }
        ]
    },
    frota: {
        removidos: [
            { quantity: 1, points: 0 }
        ],
        regulacao: [
            { duration: '00:05:00', points: 10 },
            { duration: '00:05:15', points: 7 },
            { duration: '00:05:30', points: 4 },
            { duration: '00:05:45', points: 1 },
            { duration: '00:05:46', points: 0 }
        ],
        saidaVtr: [
            { duration: '00:04:00', points: 6 },
            { duration: '00:04:15', points: 4 },
            { duration: '00:04:30', points: 2 },
            { duration: '00:04:45', points: 1 },
            { duration: '00:04:46', points: 0 }
        ]
    },
    medico: {
        removidos: [
            { quantity: 20, points: 6 },
            { quantity: 30, points: 4 },
            { quantity: 45, points: 2 },
            { quantity: 46, points: 0 }
        ],
        regulacao: [
            { duration: '00:03:00', points: 10 },
            { duration: '00:03:15', points: 7 },
            { duration: '00:03:30', points: 4 },
            { duration: '00:03:45', points: 1 },
            { duration: '00:03:46', points: 0 }
        ],
        regulacaoLider: [
            { duration: '01:00:00', points: 10 },
            { duration: '01:00:15', points: 7 },
            { duration: '01:00:30', points: 4 },
            { duration: '01:00:45', points: 1 },
            { duration: '00:01:00:46', points: 0 }
        ]
    }
};

const durationToSeconds = (duration: string): number => {
    const [h, m, s] = duration.split(':').map(Number);
    return h * 3600 + m * 60 + s;
};

const secondsToDuration = (seconds: number): string => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};


