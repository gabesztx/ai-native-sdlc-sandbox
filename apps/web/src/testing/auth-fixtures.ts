import { AuthTokens } from '../app/core/api/models';

export function adminTokens(suffix = '1'): AuthTokens {
  return {
    accessToken: `admin-access-${suffix}`,
    accessTokenExpiresIn: 900,
    refreshToken: `admin-refresh-${suffix}`,
    refreshTokenExpiresIn: 28800,
    me: { role: 'ADMIN', name: 'Admin' },
  };
}

export function memberTokens(suffix = '1'): AuthTokens {
  return {
    accessToken: `member-access-${suffix}`,
    accessTokenExpiresIn: 900,
    refreshToken: `member-refresh-${suffix}`,
    refreshTokenExpiresIn: 28800,
    me: { role: 'MEMBER', memberId: 3, name: 'Teszt Elek' },
  };
}

export function storeSession(tokens: AuthTokens): void {
  sessionStorage.setItem(
    'hugo.auth',
    JSON.stringify({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, me: tokens.me }),
  );
}
