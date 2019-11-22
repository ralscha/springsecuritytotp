import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from 'primeng/api';
import {take} from 'rxjs/operators';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import * as qrcode from 'qrcode-generator';

@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.css']
})
export class SignInComponent implements OnInit {

  qrLinkAdmin = 'otpauth://totp/admin?secret=W4AU5VIXXCPZ3S6T&issuer=2fademo';
  qrLinkUser = 'otpauth://totp/user?secret=LRVLAZ4WVFOU3JBF&issuer=2fademo';
  qrCodeAdmin: string;
  qrCodeUser: string;
  qrSafeLinkAdmin: SafeResourceUrl;
  qrSafeLinkUser: SafeResourceUrl;

  constructor(private readonly router: Router,
              private readonly authService: AuthService,
              private readonly messageService: MessageService,
              readonly domSanitizer: DomSanitizer) {

    this.qrSafeLinkAdmin = this.domSanitizer.bypassSecurityTrustResourceUrl(this.qrLinkAdmin);
    this.qrSafeLinkUser = this.domSanitizer.bypassSecurityTrustResourceUrl(this.qrLinkUser);

    const qrAdmin = qrcode(0, 'L');
    qrAdmin.addData(this.qrLinkAdmin);
    qrAdmin.make();
    this.qrCodeAdmin = qrAdmin.createDataURL(4);

    const qrUser = qrcode(0, 'L');
    qrUser.addData(this.qrLinkUser);
    qrUser.make();
    this.qrCodeUser = qrUser.createDataURL(4);
  }

  ngOnInit(): void {
    // is the user already authenticated
    this.authService.authentication$.pipe(take(1)).subscribe(flow => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      }
    });
  }

  async signin(username: string, password: string) {
    this.authService
      .signin(username, password)
      .subscribe(flow => {
          if (flow === 'NOT_AUTHENTICATED') {
            this.handleError('Sign in failed');
          }
        },
        err => this.handleError(err));
  }

  async handleError(error: any) {
    let message: string;
    if (typeof error === 'string') {
      message = error;
    } else {
      message = `Unexpected error: ${error.statusText}`;
    }

    this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: message});
  }


}
