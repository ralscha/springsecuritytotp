import {Component, inject, OnInit, signal} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from '../message.service';
import {take} from 'rxjs/operators';
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
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  constructor() {
    this.generateQrCodes();
  }

  ngOnInit(): void {
    // is the user already authenticated
    this.authService.authentication$.pipe(take(1)).subscribe((flow) => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      }
    });
  }

  signin(username: string, password: string): void {
    this.authService.signin(username, password).subscribe({
      next: (flow) => {
        if (flow === 'NOT_AUTHENTICATED') {
          this.handleError('Sign in failed');
        }
      },
      error: (err) => this.handleError(err)
    });
  }

  async handleError(error: unknown): Promise<void> {
    let message: string;
    if (typeof error === 'string') {
      message = error;
    } else if (this.hasStatusText(error)) {
      message = `Unexpected error: ${error.statusText}`;
    } else {
      message = 'Unexpected error';
    }

    this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: message});
  }

  private hasStatusText(error: unknown): error is {statusText: string} {
    return (
      typeof error === 'object' &&
      error !== null &&
      'statusText' in error &&
      typeof error.statusText === 'string'
    );
  }

  private async generateQrCodes(): Promise<void> {
    const options = {errorCorrectionLevel: 'M' as const, width: 256};
    this.qrAdminDataUrl.set(await QRCode.toDataURL(this.qrLinkAdmin, options));
    this.qrUserDataUrl.set(await QRCode.toDataURL(this.qrLinkUser, options));
  }
}
