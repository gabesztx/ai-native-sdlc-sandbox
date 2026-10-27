import { TEAM } from '../../../../testing/form-fixtures';
import { createPage } from '../../../../testing/http-test-setup';
import { TeamPageComponent } from './team-page.component';

describe('TeamPageComponent', () => {
  it('AC53: a Csapattagok oldal a csapattagok nevét mutatja', async () => {
    const page = createPage(TeamPageComponent);

    page.backend.expectOne('/api/admin/team-members').flush(TEAM);
    await page.fixture.whenStable();

    const names = Array.from(page.root().querySelectorAll('.member')).map((li) => li.textContent?.trim());
    expect(names).toEqual(['Teszt Elek', 'Minta Anna', 'Próba Péter']);
  });
});
