import { Service, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Dashboard } from '../../../core/api/models';
import { ApiConfiguration } from '../../../core/api/api-configuration';
import { AuthStoreService } from '../../../core/auth/auth-store.service';
import { TokenRefreshService } from '../../../core/auth/token-refresh.service';

const RECONNECT_DELAY_MS = 3000;

export function parseDashboardEvent(rawFrame: string): Dashboard | null {
  const dataLine = rawFrame.split('\n').find((line) => line.startsWith('data:'));
  if (!dataLine) {
    return null;
  }
  try {
    return JSON.parse(dataLine.slice('data:'.length).trim()) as Dashboard;
  } catch {
    return null;
  }
}

@Service()
export class DashboardStreamService {
  private authStore = inject(AuthStoreService);
  private tokenRefresh = inject(TokenRefreshService);
  private apiConfig = inject(ApiConfiguration);

  private dashboardSignal = signal<Dashboard | null>(null);
  private connectedSignal = signal(false);
  private stopped = false;

  public dashboard = this.dashboardSignal.asReadonly();
  public connected = this.connectedSignal.asReadonly();

  public connect(): void {
    this.stopped = false;
    void this.run();
  }

  public disconnect(): void {
    this.stopped = true;
    this.connectedSignal.set(false);
  }

  private async run(): Promise<void> {
    if (this.stopped) {
      return;
    }
    try {
      const response = await this.openConnection();
      if (response.status === 401) {
        await firstValueFrom(this.tokenRefresh.refresh());
        this.reconnectSoon(0);
        return;
      }
      if (!response.ok || !response.body) {
        this.reconnectSoon();
        return;
      }
      this.connectedSignal.set(true);
      await this.readStream(response.body);
    } catch {
      this.reconnectSoon();
      return;
    }
    this.connectedSignal.set(false);
    this.reconnectSoon();
  }

  private openConnection(): Promise<Response> {
    const token = this.authStore.accessToken();
    return fetch(`${this.apiConfig.rootUrl}/api/dashboard/stream`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
  }

  private async readStream(body: ReadableStream<Uint8Array>): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (!this.stopped) {
      const { done, value } = await reader.read();
      if (done) {
        return;
      }
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split('\n\n');
      buffer = frames.pop() ?? '';
      for (const frame of frames) {
        const dashboard = parseDashboardEvent(frame);
        if (dashboard) {
          this.dashboardSignal.set(dashboard);
        }
      }
    }
  }

  private reconnectSoon(delayMs = RECONNECT_DELAY_MS): void {
    this.connectedSignal.set(false);
    if (this.stopped) {
      return;
    }
    setTimeout(() => void this.run(), delayMs);
  }
}
