import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {AuthService, SignupResponse} from '../auth.service';
import {Router} from '@angular/router';
import {MessageService} from '../message.service';
import {FormsModule} from '@angular/forms';
import QRCode from 'qrcode';
import {finalize} from 'rxjs/operators';

@Component({
  selector: 'app-signup-secret',
  templateUrl: './signup-secret.component.html',
  imports: [FormsModule],
  styleUrl: './signup-secret.component.css'
})
export class SignupSecretComponent implements OnInit {
  qrDataUrl = signal('');
  readonly submitting = signal(false);
  readonly ready = signal(false);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => this.qrDataUrl.set(''));
    this.authService
      .pendingSignup()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response?.status !== 'OK' || !response.username || !response.secret) {
            this.messageService.error(
              'Your sign up session has expired. Sign up again to continue.'
            );
            this.router.navigate(['signup'], {replaceUrl: true});
            return;
          }
          this.showQrCode(response).catch((err: unknown) =>
            this.messageService.error(err, 'Could not generate the QR code')
          );
        },
        error: (err) => this.messageService.error(err, 'Could not restore the pending sign up')
      });
  }

  verifyCode(code: string): void {
    if (this.ready() && !this.submitting()) {
      this.submitting.set(true);
      this.authService
        .signupVerifyCode(code)
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => this.submitting.set(false))
        )
        .subscribe({
          next: (success) => {
            if (success) {
              this.ready.set(false);
              this.qrDataUrl.set('');
              this.router.navigate(['signup-okay'], {replaceUrl: true});
            } else {
              this.messageService.error('Authorization code verification failed');
            }
          },
          error: (err) => this.messageService.error(err, 'Authorization code verification failed')
        });
    }
  }

  private async showQrCode(response: SignupResponse): Promise<void> {
    if (!response.username || !response.secret) {
      return;
    }

    const issuer = '2FA Demo';
    const label = encodeURIComponent(`${issuer}:${response.username}`);
    const qrLink = `otpauth://totp/${label}?secret=${encodeURIComponent(response.secret)}&issuer=${encodeURIComponent(issuer)}`;
    const dataUrl = await QRCode.toDataURL(qrLink, {errorCorrectionLevel: 'M', width: 256});
    if (!this.destroyRef.destroyed) {
      this.qrDataUrl.set(dataUrl);
      this.ready.set(true);
    }
  }
}
