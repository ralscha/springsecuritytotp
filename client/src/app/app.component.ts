import {Component, inject, signal} from '@angular/core';
import {AuthService} from './auth.service';
import {Router, RouterLink, RouterOutlet} from '@angular/router';
import {NgxLoadingBar} from '@ngx-loading-bar/core';
import {MessageService} from './message.service';
import {finalize} from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  imports: [NgxLoadingBar, RouterLink, RouterOutlet],
  styleUrl: './app.component.css'
})
export class AppComponent {
  readonly authenticated = signal(false);
  readonly signingOut = signal(false);
  readonly currentYear = new Date().getFullYear();
  private readonly messageService = inject(MessageService);
  readonly toast = this.messageService.message;
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  constructor() {
    this.authService.authentication$.subscribe((flow) => {
      this.authenticated.set(flow === 'AUTHENTICATED');

      switch (flow) {
        case 'AUTHENTICATED':
          this.router.navigate(['home'], {replaceUrl: true});
          break;
        case 'NOT_AUTHENTICATED':
          this.router.navigate(['signin'], {replaceUrl: true});
          break;
        case 'TOTP':
          this.router.navigate(['totp'], {replaceUrl: true});
          break;
        case 'TOTP_ADDITIONAL_SECURITY':
          this.router.navigate(['totp-additional-security'], {replaceUrl: true});
          break;
      }
    });
  }

  signout(): void {
    if (this.signingOut()) {
      return;
    }
    this.signingOut.set(true);
    this.authService
      .signout()
      .pipe(finalize(() => this.signingOut.set(false)))
      .subscribe({
        next: () => this.router.navigate(['signin'], {replaceUrl: true}),
        error: (err) => this.messageService.error(err, 'Sign out failed')
      });
  }
}
