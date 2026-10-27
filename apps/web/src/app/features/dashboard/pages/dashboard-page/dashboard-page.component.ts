import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { Card } from 'primeng/card';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { MemberAvatarComponent } from '../../../../shared/components/member-avatar/member-avatar.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { Issue, Kpis } from '../../../../core/api/models';
import { DashboardService, Tile } from '../../services/dashboard.service';
import { DashboardStreamService } from '../../services/dashboard-stream.service';

interface TileDefinition {
  tile: Tile;
  label: string;
  icon: string;
  value(kpis: Kpis): number;
}

const TILE_DEFINITIONS: TileDefinition[] = [
  { tile: 'ALL', label: 'Összes', icon: 'pi pi-list', value: (kpis) => kpis.total },
  { tile: 'DONE', label: 'Kész %', icon: 'pi pi-check-circle', value: (kpis) => kpis.donePercent },
  { tile: 'IN_PROGRESS', label: 'Folyamatban', icon: 'pi pi-spinner', value: (kpis) => kpis.inProgress },
  { tile: 'BLOCKED', label: 'Blokkolt', icon: 'pi pi-ban', value: (kpis) => kpis.blocked },
  { tile: 'UNASSIGNED', label: 'Gazdátlan', icon: 'pi pi-user-minus', value: (kpis) => kpis.unassigned },
];

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

@Component({
  selector: 'dashboard-page',
  imports: [Card, ProgressBar, Skeleton, Tag, EmptyStateComponent, MemberAvatarComponent, PageHeaderComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent {
  private dashboardService = inject(DashboardService);
  private stream = inject(DashboardStreamService);

  public tiles = TILE_DEFINITIONS;
  public issueSeverity = issueSeverity;

  public dashboard = this.stream.dashboard;
  public connected = this.stream.connected;
  public loading = computed(() => this.dashboard() === null);
  public unavailable = computed(() => this.dashboard() !== null && this.dashboard()?.updatedAt == null);
  public noActiveSprint = computed(() => this.dashboard()?.updatedAt != null && this.dashboard()?.sprint == null);

  public selectedTile = signal<Tile | null>(null);

  private issuesResult = toSignal(
    toObservable(this.selectedTile).pipe(
      switchMap((tile) =>
        tile === null ? of(null) : this.dashboardService.getIssues(tile).pipe(catchError(() => of([] as Issue[]))),
      ),
    ),
    { initialValue: null },
  );
  public issues = computed(() => this.issuesResult());
  public issuesLoading = computed(() => this.selectedTile() !== null && this.issuesResult() === null);

  constructor() {
    this.stream.connect();
    inject(DestroyRef).onDestroy(() => this.stream.disconnect());
  }

  public selectTile(tile: Tile): void {
    this.selectedTile.set(this.selectedTile() === tile ? null : tile);
  }
}
