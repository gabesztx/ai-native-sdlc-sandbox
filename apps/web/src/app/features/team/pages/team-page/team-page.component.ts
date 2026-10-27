import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { Card } from 'primeng/card';
import { Skeleton } from 'primeng/skeleton';
import { TeamMember } from '../../../../core/api/models';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { MemberAvatarComponent } from '../../../../shared/components/member-avatar/member-avatar.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { TeamService } from '../../services/team.service';

@Component({
  selector: 'team-page',
  imports: [Card, Skeleton, EmptyStateComponent, MemberAvatarComponent, PageHeaderComponent],
  templateUrl: './team-page.component.html',
  styleUrl: './team-page.component.scss',
})
export class TeamPageComponent {
  private teamService = inject(TeamService);

  public members = toSignal<TeamMember[] | undefined>(this.teamService.list().pipe(catchError(() => of([]))), {
    initialValue: undefined,
  });
  public loading = computed(() => this.members() === undefined);
  public skeletonRows = [1, 2, 3, 4];
}
