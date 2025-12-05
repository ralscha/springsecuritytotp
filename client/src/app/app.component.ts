import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {AuthService} from './auth.service';
import {Router, RouterOutlet} from '@angular/router';
import {ToastModule} from 'primeng/toast';
import {NgxLoadingBar} from '@ngx-loading-bar/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  imports: [ToastModule, NgxLoadingBar, RouterOutlet],
  styleUrl: './app.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  authenticated = signal(false);
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
    this.authService
      .signout()
      .subscribe(() => this.router.navigate(['signin'], {replaceUrl: true}));
  }
}
