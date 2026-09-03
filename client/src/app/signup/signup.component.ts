import {Component, inject, signal} from '@angular/core';
import {AuthService} from '../auth.service';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {MessageService} from '../message.service';
import {finalize} from 'rxjs/operators';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  imports: [FormsModule],
  styleUrl: './signup.component.css'
})
export class SignupComponent {
  readonly submitError = signal<'usernameTaken' | 'weakPassword' | 'passwordMismatch' | null>(null);
  readonly submitting = signal(false);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  signup(username: string, password: string, passwordConfirmation: string, totp: boolean): void {
    if (this.submitting()) {
      return;
    }
    this.submitError.set(null);
    if (password !== passwordConfirmation) {
      this.submitError.set('passwordMismatch');
      return;
    }

    this.submitting.set(true);
    this.authService
      .signup(username, password, totp)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          if (response.status === 'OK' && !response.secret) {
            this.router.navigate(['signup-okay'], {replaceUrl: true});
          } else if (response.status === 'OK' && response.secret) {
            this.authService.signupResponse = response;
            this.router.navigate(['signup-secret'], {replaceUrl: true});
          } else if (response.status === 'USERNAME_TAKEN') {
            this.submitError.set('usernameTaken');
          } else if (response.status === 'WEAK_PASSWORD') {
            this.submitError.set('weakPassword');
          }
        },
        error: (err) => this.messageService.error(err, 'Sign up failed')
      });
  }
}
