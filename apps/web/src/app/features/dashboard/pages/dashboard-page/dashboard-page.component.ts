import { Component, DestroyRef, computed, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
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
  accent: string;
  value(kpis: Kpis): number;
}

const TILE_DEFINITIONS: TileDefinition[] = [
  { tile: 'ALL', label: 'Összes', icon: 'pi pi-list', accent: 'var(--p-primary-500)', value: (kpis) => kpis.total },
  { tile: 'DONE', label: 'Kész %', icon: 'pi pi-check-circle', accent: 'var(--p-green-500)', value: (kpis) => kpis.donePercent },
  { tile: 'IN_PROGRESS', label: 'Folyamatban', icon: 'pi pi-spinner', accent: 'var(--p-blue-500)', value: (kpis) => kpis.inProgress },
  { tile: 'BLOCKED', label: 'Blokkolt', icon: 'pi pi-ban', accent: 'var(--p-red-500)', value: (kpis) => kpis.blocked },
  { tile: 'UNASSIGNED', label: 'Gazdátlan', icon: 'pi pi-user-minus', accent: 'var(--p-orange-500)', value: (kpis) => kpis.unassigned },
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
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  public tiles = TILE_DEFINITIONS;
  public issueSeverity = issueSeverity;

  public dashboard = this.stream.dashboard;
  public connected = this.stream.connected;
  public loading = computed(() => this.dashboard() === null);
  public unavailable = computed(() => this.dashboard() !== null && this.dashboard()?.updatedAt == null);
  public noActiveSprint = computed(() => this.dashboard()?.updatedAt != null && this.dashboard()?.sprint == null);

  public selectedTile = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('tile') as Tile | null)),
    { initialValue: null },
  );

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
    const next = this.selectedTile() === tile ? null : tile;
    void this.router.navigate([], { relativeTo: this.route, queryParams: { tile: next } });
  }
}
