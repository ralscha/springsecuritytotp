import {Component} from '@angular/core';
import {AuthService} from './auth.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  authenticated = false;

  constructor(private readonly router: Router,
              private readonly authService: AuthService) {

    this.authService.authentication$.subscribe(flow => {
      this.authenticated = flow === 'AUTHENTICATED';

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

  signout() {
    this.authService.signout().subscribe(() => this.router.navigate(['signin'], {replaceUrl: true}));
  }
}
