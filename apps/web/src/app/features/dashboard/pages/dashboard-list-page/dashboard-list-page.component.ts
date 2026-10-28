import { Component, computed, effect, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { Card } from 'primeng/card';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { MemberAvatarComponent } from '../../../../shared/components/member-avatar/member-avatar.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { Issue } from '../../../../core/api/models';
import { DashboardService, TILE_LABELS, issueSeverity, segmentToTile } from '../../services/dashboard.service';

@Component({
  selector: 'dashboard-list-page',
  imports: [Card, Skeleton, Tag, EmptyStateComponent, MemberAvatarComponent, PageHeaderComponent],
  host: { 'animate.enter': 'fade-in-up', 'animate.leave': 'view-leave' },
  templateUrl: './dashboard-list-page.component.html',
  styleUrl: './dashboard-list-page.component.scss',
})
export class DashboardListPageComponent {
  private dashboardService = inject(DashboardService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  public tileSegment = input.required<string>();
  public issueSeverity = issueSeverity;

  public tile = computed(() => segmentToTile(this.tileSegment()));
  public tileLabel = computed(() => {
    const tile = this.tile();
    return tile ? TILE_LABELS[tile] : '';
  });

  private issuesResult = toSignal(
    toObservable(this.tile).pipe(
      switchMap((tile) => (tile === null ? of(null) : this.dashboardService.getIssues(tile).pipe(catchError(() => of([] as Issue[]))))),
    ),
    { initialValue: null },
  );
  public issues = computed(() => this.issuesResult());
  public issuesLoading = computed(() => this.tile() !== null && this.issuesResult() === null);

  constructor() {
    effect(() => {
      if (this.tile() === null) {
        void this.router.navigate(['..'], { relativeTo: this.route });
      }
    });
  }

  public goBack(): void {
    void this.router.navigate(['..'], { relativeTo: this.route });
  }
}
