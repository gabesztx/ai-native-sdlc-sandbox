import { Component, computed, input } from '@angular/core';
import { Tag } from 'primeng/tag';
import { FORM_STATUS_LABELS, REVIEW_STATUS_LABELS, STATUS_SEVERITIES } from './status-labels';

const STATUS_LABELS: Record<string, string> = { ...FORM_STATUS_LABELS, ...REVIEW_STATUS_LABELS };

@Component({
  imports: [Tag],
  selector: 'status-tag',
  styleUrl: './status-tag.component.scss',
  templateUrl: './status-tag.component.html',
})
export class StatusTagComponent {
  public status = input.required<string>();
  public label = computed(() => STATUS_LABELS[this.status()] ?? this.status());
  public severity = computed(() => STATUS_SEVERITIES[this.status()] ?? 'secondary');
}
