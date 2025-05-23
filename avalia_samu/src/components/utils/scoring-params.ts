import { NestedScoringParameters } from '@/types/project';

export const DEFAULT_PARAMS: NestedScoringParameters = {

    colab: {
        pausas: [
            { duration: 8100, points: 8 },   // 02:15:00
            { duration: 8220, points: 6 },   // 02:17:00
            { duration: 8340, points: 4 },   // 02:19:00
            { duration: 8460, points: 2 },   // 02:21:00
            { duration: 8520, points: 0 }    // 02:22:00
        ],
    },
    tarm: {
        removidos: [
            { quantity: 1, points: 6 },
            { quantity: 2, points: 0 }
        ],
        regulacao: [
            { duration: 120, points: 10 },    // 00:02:00
            { duration: 135, points: 7 },     // 00:02:15
            { duration: 150, points: 4 },     // 00:02:30
            { duration: 165, points: 1 },     // 00:02:45
            { duration: 166, points: 0 }      // 00:02:46
        ],

    },
    frota: {
        removidos: [
            { quantity: 1, points: 0 }
        ],
        regulacao: [
            { duration: 300, points: 10 },   // 00:05:00
            { duration: 315, points: 7 },    // 00:05:15
            { duration: 330, points: 4 },    // 00:05:30
            { duration: 345, points: 1 },    // 00:05:45
            { duration: 346, points: 0 }     // 00:05:46
        ],
        saidaVtr: [
            { duration: 240, points: 6 },   // 00:04:00
            { duration: 255, points: 4 },   // 00:04:15
            { duration: 270, points: 2 },   // 00:04:30
            { duration: 285, points: 1 },   // 00:04:45
            { duration: 286, points: 0 }    // 00:04:46
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
            { duration: 180, points: 10 },   // 00:03:00
            { duration: 195, points: 7 },    // 00:03:15
            { duration: 210, points: 4 },    // 00:03:30
            { duration: 225, points: 1 },    // 00:03:45
            { duration: 226, points: 0 }     // 00:03:46
        ],
        regulacaoLider: [
            { duration: 3600, points: 10 },      // 01:00:00
            { duration: 3615, points: 7 },       // 01:00:15
            { duration: 3630, points: 4 },       // 01:00:30
            { duration: 3645, points: 1 },       // 01:00:45
            { duration: 3646, points: 0 }        // 01:00:46 (assuming typo, should be 01:00:46)
        ]
    }
};


