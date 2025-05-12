import { ScoringParameters } from "@/types/project";

export const flattenParams = (params: ScoringParameters): Record<string, number> => {
    const flat: Record<string, number> = {};

    Object.entries(params).forEach(([section, sectionParams]) => {
        Object.entries(sectionParams).forEach(([field, rules]) => {
            rules.forEach((rule: { quantity?: number; duration?: string; points: number }, idx: number) => {
                const prefix = `${section}_${field}_${idx}`;
                if ('quantity' in rule) {
                    flat[`${prefix}_quantity`] = rule.quantity ?? 0;
                }
                if ('duration' in rule) {
                    flat[`${prefix}_duration`] = durationToSeconds(rule.duration!);
                }
                flat[`${prefix}_points`] = rule.points;
            });
        });
    });

    return flat;
};

export const unflattenParams = (flat: Record<string, number>): ScoringParameters => {
    const DEFAULT_PARAMS: ScoringParameters = {}; // Define the default structure here
    const params: ScoringParameters = JSON.parse(JSON.stringify(DEFAULT_PARAMS));

    Object.entries(flat).forEach(([key, value]) => {
        const parts = key.split('_');
        const [section, field, idxStr, type] = parts;
        const idx = parseInt(idxStr);
        const sectionParams = params[section as keyof ScoringParameters] as unknown as Record<string, any>;
        if (sectionParams?.[field]?.[idx]) {
            const fieldParams = sectionParams[field] as Array<Record<string, any>>;
            if (fieldParams?.[idx]) {
                fieldParams[idx][type] = type === 'duration' ? secondsToDuration(value) : value;
                fieldParams[idx].points = value;
            }
        }
    });

    return params;
};

// Helpers para conversão de tempo
const durationToSeconds = (duration: string) => {
    const [h, m, s] = duration.split(':').map(Number);
    return h * 3600 + m * 60 + s;
};

const secondsToDuration = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};