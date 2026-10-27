import { Component, input } from '@angular/core';
import { Card } from 'primeng/card';

@Component({
  imports: [Card],
  selector: 'stat-tile',
  styleUrl: './stat-tile.component.scss',
  templateUrl: './stat-tile.component.html',
})
export class StatTileComponent {
  public label = input.required<string>();
  public value = input.required<string | number>();
  public icon = input('');
}
