import { parseDashboardEvent } from './dashboard-stream.service';

describe('parseDashboardEvent', () => {
  it('AC19: a data: sorból kiolvassa és visszaadja a Dashboard JSON-t', () => {
    const frame = 'event:dashboard\ndata:{"kpis":{"total":1,"donePercent":0,"inProgress":1,"blocked":0,"unassigned":0}}';

    const dashboard = parseDashboardEvent(frame);

    expect(dashboard).toEqual({ kpis: { total: 1, donePercent: 0, inProgress: 1, blocked: 0, unassigned: 0 } });
  });

  it('data: sor hiányában null-t ad vissza', () => {
    expect(parseDashboardEvent('event:dashboard')).toBeNull();
  });

  it('érvénytelen JSON esetén null-t ad vissza, nem dob hibát', () => {
    expect(parseDashboardEvent('data:{not valid json')).toBeNull();
  });
});
