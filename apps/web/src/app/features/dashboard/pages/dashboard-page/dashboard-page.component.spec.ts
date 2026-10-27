import { signal } from '@angular/core';
import { Dashboard, Issue } from '../../../../core/api/models';
import { createPage } from '../../../../../testing/http-test-setup';
import { DashboardStreamService } from '../../services/dashboard-stream.service';
import { DashboardPageComponent } from './dashboard-page.component';

const ACTIVE_DASHBOARD: Dashboard = {
  sprint: {
    name: 'HUGO Sprint 42',
    startDate: '2026-10-20T08:00:00Z',
    endDate: '2026-11-03T16:00:00Z',
    elapsedPercent: 50,
    daysRemaining: 7,
  },
  kpis: { total: 10, donePercent: 40, inProgress: 3, blocked: 1, unassigned: 2 },
  updatedAt: '2026-10-27T10:00:00Z',
};

function fakeStream(dashboard: Dashboard | null, connected = true) {
  return {
    dashboard: signal(dashboard),
    connected: signal(connected),
    connect: vi.fn(),
    disconnect: vi.fn(),
  };
}

describe('DashboardPageComponent', () => {
  it('AC1: a KPI-csempék a kapott KPI-adatokat mutatják', () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD) },
    ]);

    const values = Array.from(page.root().querySelectorAll('.tile-value')).map((el) => el.textContent?.trim());
    expect(values).toEqual(['10', '40%', '3', '1', '2']);
  });

  it('AC4: nincs aktív sprint -> "Nincs aktív sprint" üzenet', () => {
    const page = createPage(DashboardPageComponent, {}, [
      {
        provide: DashboardStreamService,
        useValue: fakeStream({
          kpis: { total: 0, donePercent: 0, inProgress: 0, blocked: 0, unassigned: 0 },
          updatedAt: '2026-10-27T10:00:00Z',
        }),
      },
    ]);

    expect(page.root().textContent).toContain('Nincs aktív sprint');
  });

  it('AC23: soha nem szinkronizált állapot -> "Az adatok nem érhetők el" üzenet', () => {
    const page = createPage(DashboardPageComponent, {}, [
      {
        provide: DashboardStreamService,
        useValue: fakeStream({ kpis: { total: 0, donePercent: 0, inProgress: 0, blocked: 0, unassigned: 0 } }),
      },
    ]);

    expect(page.root().textContent).toContain('Az adatok nem érhetők el');
  });

  it('AC27: megszakadt kapcsolat esetén megjelenik az "Élő frissítés szünetel" jelzés', () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD, false) },
    ]);

    expect(page.root().textContent).toContain('Élő frissítés szünetel');
  });

  it('AC7/AC10: csempére kattintva megjelenik a szűrt lista, kulccsal és Jira-linkkel', async () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD) },
    ]);

    const blockedTile = Array.from(page.root().querySelectorAll<HTMLButtonElement>('.tile')).find((el) =>
      el.textContent?.includes('Blokkolt'),
    );
    blockedTile?.click();
    page.fixture.detectChanges();

    page.backend.expectOne('/api/dashboard/issues?tile=BLOCKED').flush([
      {
        key: 'HUGO-1',
        summary: 'Teszt feladat',
        status: 'Blocked',
        statusCategory: 'IN_PROGRESS',
        url: 'https://hugo.atlassian.net/browse/HUGO-1',
      },
    ] satisfies Issue[]);
    await page.fixture.whenStable();

    const link = page.root().querySelector<HTMLAnchorElement>('.issue-link');
    expect(link?.textContent?.trim()).toBe('HUGO-1');
    expect(link?.getAttribute('href')).toBe('https://hugo.atlassian.net/browse/HUGO-1');
    expect(link?.getAttribute('target')).toBe('_blank');
  });

  it('AC8: az UNASSIGNED lista "nincs" feliratot mutat assignee nélküli feladatnál', async () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD) },
    ]);

    const unassignedTile = Array.from(page.root().querySelectorAll<HTMLButtonElement>('.tile')).find((el) =>
      el.textContent?.includes('Gazdátlan'),
    );
    unassignedTile?.click();
    page.fixture.detectChanges();

    page.backend.expectOne('/api/dashboard/issues?tile=UNASSIGNED').flush([
      {
        key: 'HUGO-2',
        summary: 'Gazdátlan feladat',
        status: 'To Do',
        statusCategory: 'TODO',
        url: 'https://hugo.atlassian.net/browse/HUGO-2',
      },
    ] satisfies Issue[]);
    await page.fixture.whenStable();

    expect(page.root().querySelector('.assignee-name.none')?.textContent?.trim()).toBe('nincs');
  });
});
