import {Component, OnInit} from '@angular/core';
import {take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-totp-additional-security',
  templateUrl: './totp-additional-security.component.html',
  styleUrls: ['./totp-additional-security.component.css']
})
export class TotpAdditionalSecurityComponent implements OnInit {

  constructor(private readonly router: Router,
              private readonly authService: AuthService,
              private readonly messageService: MessageService) {
  }

  ngOnInit() {
    // are we in the correct phase
    this.authService.authentication$.pipe(take(1)).subscribe(flow => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      } else if (flow !== 'TOTP_ADDITIONAL_SECURITY') {
        this.router.navigate(['signin'], {replaceUrl: true});
      }
    });
  }

  async verifyTotpAdditionalSecurity(code1: string, code2: string, code3: string) {
    this.authService.verifyTotpAdditionalSecurity(code1, code2, code3).subscribe(flow => {
      if (flow === 'NOT_AUTHENTICATED') {
        this.handleError('Sign in failed');
      }
    }, err => this.handleError(err));
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
