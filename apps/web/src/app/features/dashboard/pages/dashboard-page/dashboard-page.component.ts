import { Component, DestroyRef, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Card } from 'primeng/card';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { Kpis } from '../../../../core/api/models';
import { DashboardStreamService } from '../../services/dashboard-stream.service';
import { Tile, tileToSegment } from '../../services/dashboard.service';

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

@Component({
  selector: 'dashboard-page',
  imports: [Card, ProgressBar, Skeleton, EmptyStateComponent, PageHeaderComponent, RouterOutlet],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent {
  private stream = inject(DashboardStreamService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  public tiles = TILE_DEFINITIONS;

  public dashboard = this.stream.dashboard;
  public connected = this.stream.connected;
  public loading = computed(() => this.dashboard() === null);
  public unavailable = computed(() => this.dashboard() !== null && this.dashboard()?.updatedAt == null);
  public noActiveSprint = computed(() => this.dashboard()?.updatedAt != null && this.dashboard()?.sprint == null);

  public hasChildRoute = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.route.firstChild !== null),
    ),
    { initialValue: this.route.snapshot.firstChild !== null },
  );

  constructor() {
    this.stream.connect();
    inject(DestroyRef).onDestroy(() => this.stream.disconnect());
  }

  public selectTile(tile: Tile): void {
    void this.router.navigate([tileToSegment(tile)], { relativeTo: this.route });
  }
}
