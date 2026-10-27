import { PageHarness, createPage } from '../../../../testing/http-test-setup';
import { pendingReview, reviewSummary } from '../../../../testing/review-fixtures';
import { ReviewFillPageComponent } from './review-fill-page.component';

async function openPending(): Promise<PageHarness<ReviewFillPageComponent>> {
  const page = createPage(ReviewFillPageComponent, { id: '21' });
  await page.fixture.whenStable();
  page.backend.expectOne('/api/me/reviews/21').flush(pendingReview());
  await page.fixture.whenStable();
  return page;
}

async function chooseScale(page: PageHarness<ReviewFillPageComponent>, value: string): Promise<void> {
  const options = Array.from(page.root().querySelectorAll('.scale p-togglebutton')) as HTMLElement[];
  const option = options.find((button) => button.getAttribute('aria-label') === value);
  option?.click();
  await page.fixture.whenStable();
  expect(option?.getAttribute('aria-pressed')).toBe('true');
}

function typeAnswer(root: HTMLElement, index: number, text: string): void {
  const area = root.querySelectorAll('textarea')[index] as HTMLTextAreaElement;
  area.value = text;
  area.dispatchEvent(new Event('input'));
}

describe('ReviewFillPageComponent', () => {
  it('AC44: kitöltés és beküldés után a kitöltött válaszokat küldi, és az Értékeléseim oldalra visz', async () => {
    const page = await openPending();

    await chooseScale(page, '4');
    typeAnswer(page.root(), 0, '  Több dokumentáció  ');
    (page.root().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const submit = page.backend.expectOne('/api/me/reviews/21/submit');
    expect(submit.request.body).toEqual({
      answers: [
        { questionId: 11, scaleValue: 4 },
        { questionId: 12, textValue: 'Több dokumentáció' },
      ],
    });
    submit.flush(reviewSummary('SUBMITTED'));
    expect(page.navigate).toHaveBeenCalledWith('/reviews');
  });

  it('kötelező válasz hiányában nem küld kérést, és jelzi a hibát', async () => {
    const page = await openPending();

    await chooseScale(page, '3');
    (page.root().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await page.fixture.whenStable();

    page.backend.expectNone('/api/me/reviews/21/submit');
    expect(page.root().querySelector('.error')?.textContent).toContain('kötelező');
  });

  it('a szerver kérdésenkénti hibáját a kérdésnél mutatja', async () => {
    const page = await openPending();
    await chooseScale(page, '5');
    typeAnswer(page.root(), 0, 'ok');
    (page.root().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    page.backend
      .expectOne('/api/me/reviews/21/submit')
      .flush({ detail: 'Invalid request', errors: { 'answers[0]': 'Az érték 1 és 5 között lehet' } }, {
        status: 400,
        statusText: 'Bad Request',
      });
    await page.fixture.whenStable();

    expect(page.root().querySelector('.field-error')?.textContent).toContain('Az érték 1 és 5 között lehet');
    expect(page.navigate).not.toHaveBeenCalled();
  });

  it('AC44: beküldött kitöltés nem nyitható meg újra kitöltésre', async () => {
    const page = createPage(ReviewFillPageComponent, { id: '21' });
    await page.fixture.whenStable();
    page.backend.expectOne('/api/me/reviews/21').flush({ ...pendingReview(), status: 'SUBMITTED' });
    await page.fixture.whenStable();

    expect(page.root().querySelector('form')).toBeNull();
    expect(page.root().querySelector('.submitted')?.textContent).toContain('már beküldted');
  });
});
