import {Component, inject, OnInit, signal} from '@angular/core';
import {finalize, take} from 'rxjs/operators';
import {AuthService} from '../auth.service';
import {MessageService} from '../message.service';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-totp',
  templateUrl: './totp.component.html',
  imports: [FormsModule],
  styleUrl: './totp.component.css'
})
export class TotpComponent implements OnInit {
  readonly submitting = signal(false);
  private readonly messageService = inject(MessageService);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    // are we in the correct phase
    this.authService.authenticate().pipe(take(1)).subscribe();
  }

  verifyTotp(code: string): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.authService
      .verifyTotp(code)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (flow) => {
          if (flow === 'TOTP') {
            this.messageService.error('This code has already been used. Wait for the next code.');
          }
        },
        error: (err) => this.messageService.error(err, 'Code verification failed')
      });
  }
}
