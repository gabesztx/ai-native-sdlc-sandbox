import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'empty-state',
  styleUrl: './empty-state.component.scss',
  templateUrl: './empty-state.component.html',
})
export class EmptyStateComponent {
  public icon = input.required<string>();
  public message = input.required<string>();
}
