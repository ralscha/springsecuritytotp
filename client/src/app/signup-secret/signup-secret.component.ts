import {Component, inject, OnInit, signal} from '@angular/core';
import {AuthService} from '../auth.service';
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
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  async ngOnInit(): Promise<void> {
    if (this.authService.signupResponse) {
      await this.showQrCode();
      return;
    }

    this.authService.pendingSignup().subscribe({
      next: (response) => {
        if (!response) {
          this.router.navigate(['signup'], {replaceUrl: true});
          return;
        }
        this.authService.signupResponse = response;
        this.showQrCode().catch((err: unknown) =>
          this.messageService.error(err, 'Could not generate the QR code')
        );
      },
      error: (err) => this.messageService.error(err, 'Could not restore the pending sign up')
    });
  }

  verifyCode(code: string): void {
    if (this.authService.signupResponse && !this.submitting()) {
      this.submitting.set(true);
      this.authService
        .signupVerifyCode(code)
        .pipe(finalize(() => this.submitting.set(false)))
        .subscribe({
          next: (success) => {
            if (success) {
              this.authService.signupResponse = null;
              this.router.navigate(['signup-okay'], {replaceUrl: true});
            } else {
              this.messageService.error('Authorization code verification failed');
            }
          },
          error: (err) => this.messageService.error(err, 'Authorization code verification failed')
        });
    }
  }

  private async showQrCode(): Promise<void> {
    const response = this.authService.signupResponse;
    if (!response?.username || !response.secret) {
      return;
    }

    const issuer = '2FA Demo';
    const label = encodeURIComponent(`${issuer}:${response.username}`);
    const qrLink = `otpauth://totp/${label}?secret=${response.secret}&issuer=${encodeURIComponent(issuer)}`;
    this.qrDataUrl.set(await QRCode.toDataURL(qrLink, {errorCorrectionLevel: 'M', width: 256}));
  }
}
