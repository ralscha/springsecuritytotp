import {Component, inject, OnInit, signal} from '@angular/core';
import {finalize, take} from 'rxjs/operators';
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
  readonly submitting = signal(false);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    // are we in the correct phase
    this.authService.authenticate().pipe(take(1)).subscribe();
  }

  verifyTotpAdditionalSecurity(code1: string, code2: string, code3: string): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.authService
      .verifyTotpAdditionalSecurity(code1, code2, code3)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (flow) => {
          if (flow === 'NOT_AUTHENTICATED') {
            this.messageService.error('Code verification failed');
          }
        },
        error: (err) => this.messageService.error(err, 'Code verification failed')
      });
  }
}
