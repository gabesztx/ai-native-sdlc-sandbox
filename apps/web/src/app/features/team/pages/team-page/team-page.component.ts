import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { Card } from 'primeng/card';
import { Skeleton } from 'primeng/skeleton';
import { TeamMember } from '../../../../core/api/models';
import { TeamApiService } from '../../../../core/team/team-api.service';
import { EmptyStateComponent } from '../../../../shared/ui/empty-state/empty-state.component';
import { MemberAvatarComponent } from '../../../../shared/ui/member-avatar/member-avatar.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'team-page',
  imports: [Card, Skeleton, EmptyStateComponent, MemberAvatarComponent, PageHeaderComponent],
  templateUrl: './team-page.component.html',
  styleUrl: './team-page.component.scss',
})
export class TeamPageComponent {
  private teamApi = inject(TeamApiService);

  public members = toSignal<TeamMember[] | undefined>(this.teamApi.list().pipe(catchError(() => of([]))), {
    initialValue: undefined,
  });
  public loading = computed(() => this.members() === undefined);
  public skeletonRows = [1, 2, 3, 4];
}
