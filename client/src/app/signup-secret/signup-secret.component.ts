import {Component, inject, OnInit} from '@angular/core';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {AuthService} from '../auth.service';
import {Router} from '@angular/router';
import {MessageService} from 'primeng/api';
import {FormsModule} from "@angular/forms";
import {QRCodeComponent} from "angularx-qrcode";
import {InputTextModule} from "primeng/inputtext";
import {KeyFilterModule} from "primeng/keyfilter";
import {ButtonDirective} from "primeng/button";


@Component({
  selector: 'app-signup-secret',
  templateUrl: './signup-secret.component.html',
  imports: [
    FormsModule,
    QRCodeComponent,
    InputTextModule,
    KeyFilterModule,
    ButtonDirective
  ],
  styleUrls: ['./signup-secret.component.css']
})
export class SignupSecretComponent implements OnInit {
  qrSafeLink: SafeResourceUrl | null = null;
  qrLink: string | null = null;
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly sanitizer = inject(DomSanitizer);

  ngOnInit(): void {
    if (!this.authService.signupResponse) {
      this.router.navigate(['login'], {replaceUrl: true});
      return;
    }

    this.qrLink = `otpauth://totp/${this.authService.signupResponse.username}?secret=${this.authService.signupResponse.secret}&issuer=2fademo`;
    this.qrSafeLink = this.sanitizer.bypassSecurityTrustResourceUrl(this.qrLink);
  }

  async verifyCode(code: string): Promise<void> {
    if (this.authService.signupResponse?.username) {
      this.authService.signupVerifyCode(this.authService.signupResponse.username, code)
        .subscribe(success => {
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
