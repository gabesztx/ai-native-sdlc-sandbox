import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { EMPTY, catchError } from 'rxjs';
import { TeamApiService } from '../../../core/team/team-api.service';

@Component({
  selector: 'team-page',
  templateUrl: './team-page.component.html',
  styleUrl: './team-page.component.scss',
})
export class TeamPageComponent {
  private teamApi = inject(TeamApiService);

  public members = toSignal(this.teamApi.list().pipe(catchError(() => EMPTY)), { initialValue: [] });
}
