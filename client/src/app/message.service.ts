import {Service, signal} from '@angular/core';

export interface ToastMessage {
  summary: string;
  detail: string;
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

  error(error: unknown, fallback = 'Unexpected error'): void {
    let detail = fallback;
    if (typeof error === 'string') {
      detail = error;
    } else if (
      typeof error === 'object' &&
      error !== null &&
      'statusText' in error &&
      typeof error.statusText === 'string' &&
      error.statusText
    ) {
      detail = `Unexpected error: ${error.statusText}`;
    }

    this.add({summary: 'Error', detail});
  }

  clear(): void {
    this.message.set(null);
    this.timeoutId = undefined;
  }
}
