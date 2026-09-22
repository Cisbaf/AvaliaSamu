import { NextRequest, NextResponse } from 'next/server';
import {
  AUTH_COOKIE_NAME,
  createSessionToken,
  isPasswordConfigured,
  isValidSessionToken,
  passwordMatches,
  SESSION_MAX_AGE_SECONDS,
} from '@/lib/auth';

export async function GET(request: NextRequest) {
  return NextResponse.json({
    authenticated: isValidSessionToken(request.cookies.get(AUTH_COOKIE_NAME)?.value),
    configured: isPasswordConfigured(),
  });
}

export async function POST(request: NextRequest) {
  if (!isPasswordConfigured()) {
    return NextResponse.json({ message: 'APP_PASSWORD não foi configurada no servidor.' }, { status: 503 });
  }

  const body = await request.json().catch(() => ({}));
  if (!passwordMatches(typeof body.password === 'string' ? body.password : '')) {
    return NextResponse.json({ message: 'Senha incorreta.' }, { status: 401 });
  }

  const response = NextResponse.json({ authenticated: true });
  const secureCookie = process.env.APP_COOKIE_SECURE
    ? process.env.APP_COOKIE_SECURE === 'true'
    : process.env.NODE_ENV === 'production';
  response.cookies.set(AUTH_COOKIE_NAME, createSessionToken(), {
    httpOnly: true,
    secure: secureCookie,
    sameSite: 'lax',
    path: '/',
    maxAge: SESSION_MAX_AGE_SECONDS,
  });
  return response;
}

export async function DELETE() {
  const response = NextResponse.json({ authenticated: false });
  const secureCookie = process.env.APP_COOKIE_SECURE
    ? process.env.APP_COOKIE_SECURE === 'true'
    : process.env.NODE_ENV === 'production';
  response.cookies.set(AUTH_COOKIE_NAME, '', {
    httpOnly: true,
    secure: secureCookie,
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
  });
  return response;
}
