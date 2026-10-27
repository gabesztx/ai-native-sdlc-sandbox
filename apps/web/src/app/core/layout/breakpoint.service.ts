import { BreakpointObserver } from '@angular/cdk/layout';
import { Service, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

const MOBILE_QUERY = '(max-width: 768px)';

@Service()
export class BreakpointService {
  private observer = inject(BreakpointObserver);

  public isMobile = toSignal(this.observer.observe(MOBILE_QUERY).pipe(map((state) => state.matches)), {
    initialValue: this.observer.isMatched(MOBILE_QUERY),
  });
}
