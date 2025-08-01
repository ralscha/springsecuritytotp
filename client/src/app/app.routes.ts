import {inject} from '@angular/core';
import {Routes} from '@angular/router';
import {HomeComponent} from './home/home.component';
import {AuthGuard} from './auth.guard';
import {SignInComponent} from './sign-in/sign-in.component';
import {TotpComponent} from './totp/totp.component';
import {TotpAdditionalSecurityComponent} from './totp-additional-security/totp-additional-security.component';
import {SignupComponent} from './signup/signup.component';
import {SignupOkayComponent} from './signup-okay/signup-okay.component';
import {SignupSecretComponent} from './signup-secret/signup-secret.component';

export const routes: Routes = [
  {path: '', redirectTo: 'home', pathMatch: 'full'},
  {path: 'home', component: HomeComponent, canActivate: [() => inject(AuthGuard).canActivate()]},
  {path: 'signin', component: SignInComponent},
  {path: 'totp', component: TotpComponent},
  {path: 'totp-additional-security', component: TotpAdditionalSecurityComponent},
  {path: 'signup', component: SignupComponent},
  {path: 'signup-okay', component: SignupOkayComponent},
  {path: 'signup-secret', component: SignupSecretComponent},
  {path: '**', redirectTo: 'home'}
];
