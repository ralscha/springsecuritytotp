import {Component, inject, OnInit, signal} from '@angular/core';
import {AuthService} from '../auth.service';
import {Router} from '@angular/router';
import {MessageService} from '../message.service';
import {FormsModule} from '@angular/forms';
import QRCode from 'qrcode';

@Component({
  selector: 'app-signup-secret',
  templateUrl: './signup-secret.component.html',
  imports: [FormsModule],
  styleUrl: './signup-secret.component.css'
})
export class SignupSecretComponent implements OnInit {
  qrDataUrl = signal('');
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  async ngOnInit(): Promise<void> {
    if (!this.authService.signupResponse) {
      this.router.navigate(['signin'], {replaceUrl: true});
      return;
    }

    const qrLink = `otpauth://totp/${this.authService.signupResponse.username}?secret=${this.authService.signupResponse.secret}&issuer=2fademo`;
    this.qrDataUrl.set(await QRCode.toDataURL(qrLink, {errorCorrectionLevel: 'M', width: 256}));
  }

  async verifyCode(code: string): Promise<void> {
    if (this.authService.signupResponse?.username) {
      this.authService
        .signupVerifyCode(this.authService.signupResponse.username, code)
        .subscribe((success) => {
          if (success) {
            this.authService.signupResponse = null;
            this.router.navigate(['signup-okay']);
          } else {
            this.messageService.add({
              key: 'tst',
              severity: 'error',
              summary: 'Error',
              detail: 'Authorization Code verification failed'
            });
          }
        });
    }
  }
}
