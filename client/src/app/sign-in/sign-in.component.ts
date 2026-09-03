import {Component, inject, OnInit, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from '../message.service';
import {finalize, take} from 'rxjs/operators';
import {FormsModule} from '@angular/forms';
import QRCode from 'qrcode';

@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  imports: [FormsModule, RouterLink],
  styleUrl: './sign-in.component.css'
})
export class SignInComponent implements OnInit {
  qrLinkAdmin = 'otpauth://totp/admin?secret=W4AU5VIXXCPZ3S6T&issuer=2fademo';
  qrLinkUser = 'otpauth://totp/user?secret=LRVLAZ4WVFOU3JBF&issuer=2fademo';
  qrAdminDataUrl = signal('');
  qrUserDataUrl = signal('');
  readonly submitting = signal(false);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  constructor() {
    this.generateQrCodes().catch((err: unknown) =>
      this.messageService.error(err, 'Could not generate the demo QR codes')
    );
  }

  ngOnInit(): void {
    // is the user already authenticated
    this.authService.authenticate().pipe(take(1)).subscribe();
  }

  signin(username: string, password: string): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.authService
      .signin(username, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (flow) => {
          if (flow === 'NOT_AUTHENTICATED') {
            this.messageService.error('Sign in failed');
          }
        },
        error: (err) => this.messageService.error(err)
      });
  }

  private async generateQrCodes(): Promise<void> {
    const options = {errorCorrectionLevel: 'M' as const, width: 256};
    this.qrAdminDataUrl.set(await QRCode.toDataURL(this.qrLinkAdmin, options));
    this.qrUserDataUrl.set(await QRCode.toDataURL(this.qrLinkUser, options));
  }
}
