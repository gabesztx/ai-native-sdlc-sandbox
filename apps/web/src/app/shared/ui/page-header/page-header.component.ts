import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'page-header',
  styleUrl: './page-header.component.scss',
  templateUrl: './page-header.component.html',
})
export class PageHeaderComponent {
  public title = input.required<string>();
  public subtitle = input('');
}
