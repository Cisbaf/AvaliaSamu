import { createHmac, timingSafeEqual } from 'node:crypto';

export const AUTH_COOKIE_NAME = 'avalia_session';
export const SESSION_MAX_AGE_SECONDS = 21 * 24 * 60 * 60;

function configuredPassword() {
  return process.env.APP_PASSWORD || '';
}

function safeEqual(first: string, second: string) {
  const firstDigest = createHmac('sha256', 'avalia-password-check').update(first).digest();
  const secondDigest = createHmac('sha256', 'avalia-password-check').update(second).digest();
  return timingSafeEqual(firstDigest, secondDigest);
}

function sign(expiresAt: string) {
  return createHmac('sha256', configuredPassword()).update(`avalia:${expiresAt}`).digest('base64url');
}

export function isPasswordConfigured() {
  return configuredPassword().length > 0;
}

export function passwordMatches(candidate: string) {
  return isPasswordConfigured() && safeEqual(candidate, configuredPassword());
}

export function createSessionToken() {
  const expiresAt = String(Math.floor(Date.now() / 1000) + SESSION_MAX_AGE_SECONDS);
  return `${expiresAt}.${sign(expiresAt)}`;
}

export function isValidSessionToken(token?: string) {
  if (!token || !isPasswordConfigured()) return false;
  const [expiresAt, receivedSignature, extra] = token.split('.');
  if (!expiresAt || !receivedSignature || extra || !/^\d+$/.test(expiresAt)) return false;
  if (Number(expiresAt) <= Math.floor(Date.now() / 1000)) return false;
  return safeEqual(receivedSignature, sign(expiresAt));
}
