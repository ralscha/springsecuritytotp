import {Component, inject, OnInit} from '@angular/core';
import {take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from 'primeng/api';
import {FormsModule} from "@angular/forms";
import {InputTextModule} from "primeng/inputtext";
import {KeyFilterModule} from "primeng/keyfilter";
import {ButtonDirective} from "primeng/button";


@Component({
  selector: 'app-totp-additional-security',
  templateUrl: './totp-additional-security.component.html',
  imports: [
    FormsModule,
    InputTextModule,
    KeyFilterModule,
    ButtonDirective
  ],
  styleUrls: ['./totp-additional-security.component.css']
})
export class TotpAdditionalSecurityComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);


  ngOnInit(): void {
    // are we in the correct phase
    this.authService.authentication$.pipe(take(1)).subscribe(flow => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      } else if (flow !== 'TOTP_ADDITIONAL_SECURITY') {
        this.router.navigate(['signin'], {replaceUrl: true});
      }
    });
  }

  async verifyTotpAdditionalSecurity(code1: string, code2: string, code3: string): Promise<void> {
    this.authService.verifyTotpAdditionalSecurity(code1, code2, code3).subscribe(flow => {
      if (flow === 'NOT_AUTHENTICATED') {
        this.handleError('Sign in failed');
      }
    }, err => this.handleError(err));
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
