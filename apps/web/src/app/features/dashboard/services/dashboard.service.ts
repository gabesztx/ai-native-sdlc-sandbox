import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Dashboard, Issue } from '../../../core/api/models';
import { DashboardControllerService } from '../../../core/api/services/dashboard-controller.service';

export type Tile = 'ALL' | 'DONE' | 'IN_PROGRESS' | 'BLOCKED' | 'UNASSIGNED';

export const TILE_SEGMENTS: Record<Tile, string> = {
  ALL: 'all',
  DONE: 'done',
  IN_PROGRESS: 'in-progress',
  BLOCKED: 'blocked',
  UNASSIGNED: 'unassigned',
};

export const TILE_LABELS: Record<Tile, string> = {
  ALL: 'Összes',
  DONE: 'Kész',
  IN_PROGRESS: 'Folyamatban',
  BLOCKED: 'Blokkolt',
  UNASSIGNED: 'Gazdátlan',
};

export function tileToSegment(tile: Tile): string {
  return TILE_SEGMENTS[tile];
}

export function segmentToTile(segment: string): Tile | null {
  const entry = (Object.entries(TILE_SEGMENTS) as [Tile, string][]).find(([, seg]) => seg === segment);
  return entry ? entry[0] : null;
}

export function issueSeverity(issue: Issue): 'secondary' | 'info' | 'success' | 'danger' {
  if (issue.status === 'Blocked') {
    return 'danger';
  }
  if (issue.statusCategory === 'DONE') {
    return 'success';
  }
  if (issue.statusCategory === 'IN_PROGRESS') {
    return 'info';
  }
  return 'secondary';
}

@Service()
export class DashboardService {
  private dashboardApi = inject(DashboardControllerService);

  public get(): Observable<Dashboard> {
    return this.dashboardApi.getDashboard();
  }

  public getIssues(tile: Tile): Observable<Issue[]> {
    return this.dashboardApi.listDashboardIssues({ tile });
  }
}
