import { signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Dashboard } from '../../../../core/api/models';
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

  it('0006/AC1: csempére kattintva a megfelelő lista aloldal URL-szegmensére navigál', () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD) },
    ]);

    const blockedTile = Array.from(page.root().querySelectorAll<HTMLButtonElement>('.tile')).find((el) =>
      el.textContent?.includes('Blokkolt'),
    );
    blockedTile?.click();

    expect(page.navigateTo).toHaveBeenCalledWith(['blocked'], expect.objectContaining({ relativeTo: expect.anything() }));
  });

  it('0006/AC9: a főnézet gyökérelemén jelen van a belépő animáció-osztály', () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD) },
    ]);

    expect(page.root().querySelector('.dashboard-main')).not.toBeNull();
  });

  it('0006/AC1+AC2: aktív gyermek route esetén a KPI-kártyák és a sprint progress card eltűnik', () => {
    const page = createPage(DashboardPageComponent, {}, [
      { provide: DashboardStreamService, useValue: fakeStream(ACTIVE_DASHBOARD) },
      { provide: ActivatedRoute, useValue: { snapshot: { firstChild: {} }, firstChild: {} } },
    ]);

    expect(page.root().querySelector('.dashboard-main')).toBeNull();
  });
});
