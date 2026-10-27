import { Component, computed, input } from '@angular/core';
import { Avatar } from 'primeng/avatar';
import { initials } from '../../initials';

@Component({
  imports: [Avatar],
  selector: 'member-avatar',
  styleUrl: './member-avatar.component.scss',
  templateUrl: './member-avatar.component.html',
  host: { 'aria-hidden': 'true' },
})
export class MemberAvatarComponent {
  public name = input.required<string>();
  public size = input<'normal' | 'large'>('normal');
  public initials = computed(() => initials(this.name()));
}
