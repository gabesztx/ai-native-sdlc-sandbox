import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Dashboard, Issue } from '../../../core/api/models';
import { DashboardControllerService } from '../../../core/api/services/dashboard-controller.service';

export type Tile = 'ALL' | 'DONE' | 'IN_PROGRESS' | 'BLOCKED' | 'UNASSIGNED';

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
