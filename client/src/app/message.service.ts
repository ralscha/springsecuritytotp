import {Service, signal} from '@angular/core';

export interface ToastMessage {
  key?: string;
  severity?: string;
  summary?: string;
  detail?: string;
}

@Service()
export class MessageService {
  readonly message = signal<ToastMessage | null>(null);
  private timeoutId: ReturnType<typeof setTimeout> | undefined;

  add(message: ToastMessage): void {
    this.message.set(message);

    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }

    this.timeoutId = setTimeout(() => this.clear(), 5000);
  }

  clear(): void {
    this.message.set(null);
    this.timeoutId = undefined;
  }
}
