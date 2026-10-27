import { annasResponse, draftForm, sentForm } from '../../../../../testing/form-fixtures';
import { createPage } from '../../../../../testing/http-test-setup';
import { FormDetailPageComponent } from './form-detail-page.component';

describe('FormDetailPageComponent', () => {
  it('AC45: kiküldés után a címzettek állapota és a beküldött válaszok név szerint látszanak', async () => {
    const page = createPage(FormDetailPageComponent, { id: '7' });
    await page.fixture.whenStable();
    page.backend.expectOne('/api/admin/forms/7').flush(draftForm());
    await page.fixture.whenStable();

    (page.root().querySelector('.send') as HTMLButtonElement).click();
    const send = page.backend.expectOne('/api/admin/forms/7/send');
    expect(send.request.method).toBe('POST');
    send.flush(sentForm());
    page.backend.expectOne('/api/admin/forms/7/responses').flush([annasResponse()]);
    await page.fixture.whenStable();

    const recipients = Array.from(page.root().querySelectorAll('.recipient')).map((li) => [
      li.querySelector('.name')?.textContent?.trim(),
      li.querySelector('.recipient-status')?.textContent?.trim(),
    ]);
    expect(recipients).toEqual([
      ['Minta Anna', 'Beküldve'],
      ['Próba Péter', 'Kitöltendő'],
    ]);
    const answers = Array.from(page.root().querySelectorAll('.answer')).map((dd) => dd.textContent?.trim());
    expect(answers).toEqual(['4', 'Több dokumentáció']);
    expect(page.root().querySelector('.response .response-name')?.textContent).toContain('Minta Anna');
    expect(page.root().querySelector('.send')).toBeNull();
  });

  it('kérdés nélküli kiküldésnél a szerver hibaüzenetét mutatja', async () => {
    const page = createPage(FormDetailPageComponent, { id: '7' });
    await page.fixture.whenStable();
    page.backend.expectOne('/api/admin/forms/7').flush({ ...draftForm(), questions: [] });
    await page.fixture.whenStable();

    (page.root().querySelector('.send') as HTMLButtonElement).click();
    page.backend
      .expectOne('/api/admin/forms/7/send')
      .flush({ detail: 'Legalább egy kérdés szükséges' }, { status: 422, statusText: 'Unprocessable' });
    await page.fixture.whenStable();

    expect(page.root().querySelector('.error')?.textContent).toContain('Legalább egy kérdés szükséges');
  });
});
