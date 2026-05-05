export const parseTimeToSeconds = (timeString: string): number | null => {
  if (timeString === null || typeof timeString === 'undefined' || timeString.trim() === '') {
    return 0; // Tratar string vazia como 0 segundos, ou null se preferir validação mais estrita
  }
  if (typeof timeString !== 'string') return null;

  const parts = timeString.split(':');
  let hours = 0, minutes = 0, seconds = 0;

  if (parts.length === 3) { // HH:MM:SS
    hours = parseInt(parts[0], 10);
    minutes = parseInt(parts[1], 10);
    seconds = parseInt(parts[2], 10);
  } else if (parts.length === 2) { // MM:SS
    minutes = parseInt(parts[0], 10);
    seconds = parseInt(parts[1], 10);
  } else if (parts.length === 1 && !isNaN(parseInt(parts[0], 10))) { // SS
    seconds = parseInt(parts[0], 10);
  } else {
    return null; // Formato inválido
  }

  if (isNaN(hours) || isNaN(minutes) || isNaN(seconds)) return null;
  // Validação básica dos campos (minutos e segundos entre 0-59)
  if (minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59 || hours < 0) return null;


  return hours * 3600 + minutes * 60 + seconds;
};

// Formata segundos totais para string HH:MM:SS
export const formatSecondsToTime = (totalSeconds: number | null | undefined): string => {
  if (totalSeconds === null || typeof totalSeconds === 'undefined' || isNaN(totalSeconds) || totalSeconds < 0) {
    totalSeconds = 0; // Default para 0 se inválido ou nulo
  }

  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;

  const pad = (num: number) => num.toString().padStart(2, '0');

  return `${pad(hours)}:${pad(minutes)}:${pad(secs)}`;
};