import {Component, OnInit} from '@angular/core';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {AuthService} from '../auth.service';
import {Router} from '@angular/router';
import * as qrcode from 'qrcode-generator';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-signup-secret',
  templateUrl: './signup-secret.component.html',
  styleUrls: ['./signup-secret.component.css']
})
export class SignupSecretComponent implements OnInit {

  qrSafeLink: SafeResourceUrl;
  qrCode: string;

  constructor(private readonly router: Router,
              private readonly authService: AuthService,
              private readonly messageService: MessageService,
              private readonly sanitizer: DomSanitizer) {
  }

  ngOnInit() {
    if (!this.authService.signupResponse) {
      this.router.navigate(['login'], {replaceUrl: true});
      return;
    }

    const link = `otpauth://totp/${this.authService.signupResponse.username}?secret=${this.authService.signupResponse.secret}&issuer=2fademo`;
    this.qrSafeLink = this.sanitizer.bypassSecurityTrustResourceUrl(link);

    const qrAdmin = qrcode(0, 'L');
    qrAdmin.addData(link);
    qrAdmin.make();
    this.qrCode = qrAdmin.createDataURL(4);
  }

  async verifyCode(code: string) {
    this.authService.signupVerifyCode(this.authService.signupResponse.username, code)
      .subscribe(success => {
        if (success) {
          this.authService.signupResponse = null;
          this.router.navigate(['signup-okay']);
        } else {
          this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: 'Authorization Code verification failed'});
        }
      });
  }


}
