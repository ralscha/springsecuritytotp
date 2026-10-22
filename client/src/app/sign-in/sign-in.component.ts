import {Component, inject, OnInit, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from '../message.service';
import {finalize, take} from 'rxjs/operators';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  imports: [FormsModule, RouterLink],
  styleUrl: './sign-in.component.css'
})
export class SignInComponent implements OnInit {
  readonly submitting = signal(false);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    // is the user already authenticated
    this.authService.authenticate().pipe(take(1)).subscribe();
  }

  signin(username: string, password: string): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.authService
      .signin(username, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (flow) => {
          if (flow === 'NOT_AUTHENTICATED') {
            this.messageService.error('Sign in failed');
          }
        },
        error: (err) => this.messageService.error(err, 'Sign in failed')
      });
  }
}
