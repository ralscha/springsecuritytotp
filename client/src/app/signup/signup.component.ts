import {Component, inject} from '@angular/core';
import {AuthService} from '../auth.service';
import {Router} from '@angular/router';
import {FormsModule} from "@angular/forms";
import {InputTextModule} from "primeng/inputtext";
import {CheckboxModule} from "primeng/checkbox";
import {ButtonDirective} from "primeng/button";


@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  imports: [
    FormsModule,
    InputTextModule,
    CheckboxModule,
    ButtonDirective
  ],
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  submitError: string | null = null;
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  signup(email: string, password: string, totp: boolean): void {
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
