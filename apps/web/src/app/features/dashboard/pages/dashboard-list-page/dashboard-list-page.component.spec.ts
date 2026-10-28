import { Issue } from '../../../../core/api/models';
import { createPage } from '../../../../../testing/http-test-setup';
import { DashboardListPageComponent } from './dashboard-list-page.component';

describe('DashboardListPageComponent', () => {
  it('AC3: a tileSegment input alapján rögtön a szűrt listát kéri le', () => {
    const page = createPage(DashboardListPageComponent, { tileSegment: 'blocked' });

    page.backend.expectOne('/api/dashboard/issues?tile=BLOCKED').flush([] satisfies Issue[]);

    expect(page.root().querySelector('page-header')).not.toBeNull();
  });

  it('AC5: érvénytelen URL-szegmensnél a szülő route-ra navigál', () => {
    const page = createPage(DashboardListPageComponent, { tileSegment: 'xyz' });

    expect(page.navigateTo).toHaveBeenCalledWith(['..'], expect.objectContaining({ relativeTo: expect.anything() }));
  });

  it('AC6: folyamatban lévő lekérdezésnél skeleton-sorok jelennek meg', () => {
    const page = createPage(DashboardListPageComponent, { tileSegment: 'blocked' });

    expect(page.root().querySelectorAll('p-skeleton').length).toBeGreaterThan(0);
  });

  it('AC7: üres szűrt eredménynél az empty-state jelenik meg', () => {
    const page = createPage(DashboardListPageComponent, { tileSegment: 'blocked' });

    page.backend.expectOne('/api/dashboard/issues?tile=BLOCKED').flush([] satisfies Issue[]);
    page.fixture.detectChanges();

    expect(page.root().querySelector('empty-state')).not.toBeNull();
  });

  it('AC8: egy issue-kártyán kulcs, összegzés, állapot és hozzárendelt (vagy "nincs") látszik', () => {
    const page = createPage(DashboardListPageComponent, { tileSegment: 'unassigned' });

    page.backend.expectOne('/api/dashboard/issues?tile=UNASSIGNED').flush([
      {
        key: 'HUGO-2',
        summary: 'Gazdátlan feladat',
        status: 'To Do',
        statusCategory: 'TODO',
        url: 'https://hugo.atlassian.net/browse/HUGO-2',
      },
    ] satisfies Issue[]);
    page.fixture.detectChanges();

    const root = page.root();
    const link = root.querySelector<HTMLAnchorElement>('.issue-link');
    expect(link?.textContent?.trim()).toBe('HUGO-2');
    expect(link?.getAttribute('href')).toBe('https://hugo.atlassian.net/browse/HUGO-2');
    expect(root.querySelector('.issue-summary')?.textContent?.trim()).toBe('Gazdátlan feladat');
    expect(root.querySelector('p-tag')).not.toBeNull();
    expect(root.querySelector('.assignee-name.none')?.textContent?.trim()).toBe('nincs');
  });

  it('AC2: a vissza-elemre kattintva a szülő route-ra navigál', () => {
    const page = createPage(DashboardListPageComponent, { tileSegment: 'blocked' });
    page.backend.expectOne('/api/dashboard/issues?tile=BLOCKED').flush([] satisfies Issue[]);

    page.root().querySelector<HTMLButtonElement>('.back-button')?.click();

    expect(page.navigateTo).toHaveBeenCalledWith(['..'], expect.objectContaining({ relativeTo: expect.anything() }));
  });
});
