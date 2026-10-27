import { FormSummary } from '../../../../core/api/models';
import { createPage } from '../../../../../testing/http-test-setup';
import { FormListPageComponent } from './form-list-page.component';

describe('FormListPageComponent', () => {
  it('az Űrlapok lista állapotot és kiküldöttnél „beküldött / címzett” számot mutat', async () => {
    const page = createPage(FormListPageComponent);
    const forms: FormSummary[] = [
      {
        id: 1, topic: 'Q4', evaluatedMember: { id: 3, name: 'Teszt Elek' }, status: 'SENT',
        createdAt: '2026-10-27T10:00:00Z', sentAt: '2026-10-27T11:00:00Z', recipientCount: 5, submittedCount: 3,
      },
      {
        id: 2, topic: 'Q1', evaluatedMember: { id: 4, name: 'Minta Anna' }, status: 'DRAFT',
        createdAt: '2026-10-27T09:00:00Z', recipientCount: 0, submittedCount: 0,
      },
    ];

    page.backend.expectOne('/api/admin/forms').flush(forms);
    await page.fixture.whenStable();

    const rows = page.root().querySelectorAll('.form-row');
    expect(rows[0].querySelector('.status')?.textContent).toContain('Kiküldve');
    expect(rows[0].querySelector('.counts')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('3 / 5');
    expect(rows[1].querySelector('.status')?.textContent).toContain('Piszkozat');
  });
});
