import {Service, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';

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
    } else if (error instanceof HttpErrorResponse) {
      if (error.status === 429) {
        detail = 'Too many attempts. Please wait a few minutes before trying again.';
      } else if (error.status === 0) {
        detail = 'Could not reach the server. Check your connection and try again.';
      } else if (error.status === 403) {
        detail = 'Your session could not be verified. Reload the page and try again.';
      }
    }

    this.add({summary: 'Error', detail});
  }

  clear(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    this.message.set(null);
    this.timeoutId = undefined;
  }
}
