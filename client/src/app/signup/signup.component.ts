import {Component} from '@angular/core';
import {AuthService} from '../auth.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {

  submitError: string = null;

  constructor(private readonly router: Router,
              private readonly authService: AuthService) {
  }

  signup(email: string, password: string, totp: boolean) {
    this.submitError = null;

    this.authService.signup(email, password, totp)
      .subscribe(response => {
        if (response.status === 'OK' && !response.secret) {
          this.router.navigate(['signup-okay'], {replaceUrl: true});
        } else if (response.status === 'OK' && response.secret) {
          this.authService.signupResponse = response;
          this.router.navigate(['signup-secret'], {replaceUrl: true});
        } else if (response.status === 'USERNAME_TAKEN') {
          this.submitError = 'usernameTaken';
        } else if (response.status === 'WEAK_PASSWORD') {
          this.submitError = 'weakPassword';
        }
      });
  }

}
