import { createPage } from '../../../../../testing/http-test-setup';
import { reviewSummary } from '../../../../../testing/review-fixtures';
import { ReviewListPageComponent } from './review-list-page.component';

describe('ReviewListPageComponent', () => {
  it('AC44: a beküldött kitöltés Beküldve állapotú, időponttal, és nincs rá kitöltő link', async () => {
    const page = createPage(ReviewListPageComponent);

    page.backend.expectOne('/api/me/reviews').flush([reviewSummary('SUBMITTED')]);
    await page.fixture.whenStable();

    const review = page.root().querySelector('.review') as HTMLElement;
    expect(review.querySelector('.status')?.textContent).toContain('Beküldve');
    expect(review.querySelector('.time')).not.toBeNull();
    expect(review.querySelector('a')).toBeNull();
  });

  it('a kitöltendő értékelés linkkel nyitható, az értékelt nevével', async () => {
    const page = createPage(ReviewListPageComponent);

    page.backend.expectOne('/api/me/reviews').flush([reviewSummary('PENDING')]);
    await page.fixture.whenStable();

    const review = page.root().querySelector('.review') as HTMLElement;
    expect(review.querySelector('a.fill')?.getAttribute('href')).toBe('/reviews/21');
    expect(review.querySelector('.status')?.textContent).toContain('Kitöltendő');
    expect(review.querySelector('.evaluated')?.textContent).toContain('Teszt Elek');
  });
});
