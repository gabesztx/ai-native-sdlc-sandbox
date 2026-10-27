import { TEAM, draftForm } from '../../../../testing/form-fixtures';
import { createPage, setValue } from '../../../../testing/http-test-setup';
import { FormEditorPageComponent } from './form-editor-page.component';

describe('FormEditorPageComponent', () => {
  it('AC45: új űrlapot hoz létre a megadott témával, értékelttel és kérdésekkel', async () => {
    const page = createPage(FormEditorPageComponent);
    page.backend.expectOne('/api/admin/team-members').flush(TEAM);
    await page.fixture.whenStable();

    setValue(page.root(), '#topic', 'Csapatmunka Q4');
    const evaluated = page.root().querySelector('#evaluated') as HTMLSelectElement;
    setValue(page.root(), '#evaluated', evaluated.options[1].value);
    (page.root().querySelector('.add-scale') as HTMLButtonElement).click();
    (page.root().querySelector('.add-text') as HTMLButtonElement).click();
    await page.fixture.whenStable();
    setValue(page.root(), '#question-text-0', 'Mennyire segítőkész?');
    setValue(page.root(), '#question-text-1', 'Miben fejlődhetne?');
    (page.root().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const create = page.backend.expectOne('/api/admin/forms');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({
      topic: 'Csapatmunka Q4',
      evaluatedMemberId: 3,
      questions: [
        { text: 'Mennyire segítőkész?', type: 'SCALE', required: true, scaleMin: 1, scaleMax: 5 },
        { text: 'Miben fejlődhetne?', type: 'TEXT', required: true, scaleMin: undefined, scaleMax: undefined },
      ],
    });
    create.flush(draftForm());
    expect(page.navigateTo).toHaveBeenCalledWith(['/admin/forms', 7]);
  });

  it('a szerver mezőnkénti hibáját a mező mellett mutatja', async () => {
    const page = createPage(FormEditorPageComponent);
    page.backend.expectOne('/api/admin/team-members').flush(TEAM);
    await page.fixture.whenStable();
    setValue(page.root(), '#topic', 'T');
    setValue(page.root(), '#evaluated', (page.root().querySelector('#evaluated') as HTMLSelectElement).options[1].value);
    (page.root().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    page.backend
      .expectOne('/api/admin/forms')
      .flush({ detail: 'Invalid request', errors: { evaluatedMemberId: 'Nincs ilyen csapattag' } }, {
        status: 400,
        statusText: 'Bad Request',
      });
    await page.fixture.whenStable();

    expect(page.root().querySelector('.field-error')?.textContent).toContain('Nincs ilyen csapattag');
    expect(page.navigateTo).not.toHaveBeenCalled();
  });

  it('piszkozat szerkesztésekor betölti az adatokat, és megerősítés után törli', async () => {
    const page = createPage(FormEditorPageComponent, { id: '7' });
    page.backend.expectOne('/api/admin/team-members').flush(TEAM);
    await page.fixture.whenStable();
    page.backend.expectOne('/api/admin/forms/7').flush(draftForm());
    await page.fixture.whenStable();

    expect((page.root().querySelector('#topic') as HTMLInputElement).value).toBe('Csapatmunka Q4');
    expect((page.root().querySelector('#question-text-1') as HTMLInputElement).value).toBe('Miben fejlődhetne?');

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    (page.root().querySelector('.delete') as HTMLButtonElement).click();
    const del = page.backend.expectOne('/api/admin/forms/7');
    expect(del.request.method).toBe('DELETE');
    del.flush(null, { status: 204, statusText: 'No Content' });
    expect(page.navigate).toHaveBeenCalledWith('/admin/forms');
  });
});
