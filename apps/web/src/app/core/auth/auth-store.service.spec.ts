import { TestBed } from '@angular/core/testing';
import { adminTokens, memberTokens } from '../../../testing/auth-fixtures';
import { AuthStoreService } from './auth-store.service';

describe('AuthStoreService', () => {
  beforeEach(() => sessionStorage.clear());

  it('stores tokens in sessionStorage and derives the home path from the role', () => {
    const store = TestBed.inject(AuthStoreService);

    store.save(memberTokens());

    expect(store.accessToken()).toBe('member-access-1');
    expect(store.homePath()).toBe('/reviews');
    expect(sessionStorage.getItem('hugo.auth')).toContain('member-refresh-1');
  });

  it('clear removes every token from sessionStorage', () => {
    const store = TestBed.inject(AuthStoreService);
    store.save(adminTokens());

    store.clear();

    expect(store.isLoggedIn()).toBe(false);
    expect(store.homePath()).toBe('/login');
    expect(sessionStorage.getItem('hugo.auth')).toBeNull();
  });
});
