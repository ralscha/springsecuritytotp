import {Component, inject, OnInit} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from 'primeng/api';
import {take} from 'rxjs/operators';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {FormsModule} from "@angular/forms";
import {InputTextModule} from "primeng/inputtext";
import {ButtonDirective} from "primeng/button";
import {QRCodeComponent} from "angularx-qrcode";


@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  imports: [
    FormsModule,
    InputTextModule,
    ButtonDirective,
    RouterLink,
    QRCodeComponent
  ],
  styleUrls: ['./sign-in.component.css']
})
export class SignInComponent implements OnInit {
  readonly domSanitizer = inject(DomSanitizer);
  qrLinkAdmin = 'otpauth://totp/admin?secret=W4AU5VIXXCPZ3S6T&issuer=2fademo';
  qrLinkUser = 'otpauth://totp/user?secret=LRVLAZ4WVFOU3JBF&issuer=2fademo';
  qrSafeLinkAdmin: SafeResourceUrl;
  qrSafeLinkUser: SafeResourceUrl;
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  constructor() {
    this.qrSafeLinkAdmin = this.domSanitizer.bypassSecurityTrustResourceUrl(this.qrLinkAdmin);
    this.qrSafeLinkUser = this.domSanitizer.bypassSecurityTrustResourceUrl(this.qrLinkUser);
  }

  ngOnInit(): void {
    // is the user already authenticated
    this.authService.authentication$.pipe(take(1)).subscribe(flow => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      }
    });
  }

  async signin(username: string, password: string): Promise<void> {
    this.authService
      .signin(username, password)
      .subscribe(flow => {
          if (flow === 'NOT_AUTHENTICATED') {
            this.handleError('Sign in failed');
          }
        },
        err => this.handleError(err));
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async handleError(error: any): Promise<void> {
    let message: string;
    if (typeof error === 'string') {
      message = error;
    } else {
      message = `Unexpected error: ${error.statusText}`;
    }

    this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: message});
  }


}
