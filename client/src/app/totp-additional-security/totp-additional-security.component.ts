import {Component, inject, OnInit} from '@angular/core';
import {take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from '../message.service';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-totp-additional-security',
  templateUrl: './totp-additional-security.component.html',
  imports: [FormsModule],
  styleUrl: './totp-additional-security.component.css'
})
export class TotpAdditionalSecurityComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    // are we in the correct phase
    this.authService.authentication$.pipe(take(1)).subscribe((flow) => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      } else if (flow !== 'TOTP_ADDITIONAL_SECURITY') {
        this.router.navigate(['signin'], {replaceUrl: true});
      }
    });
  }

  verifyTotpAdditionalSecurity(code1: string, code2: string, code3: string): void {
    this.authService.verifyTotpAdditionalSecurity(code1, code2, code3).subscribe({
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
}
