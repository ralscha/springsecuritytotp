import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HomeComponent} from './home/home.component';
import {SignInComponent} from './sign-in/sign-in.component';
import {TotpComponent} from './totp/totp.component';
import {TotpAdditionalSecurityComponent} from './totp-additional-security/totp-additional-security.component';
import {FormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {MessageService} from 'primeng/api';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LoadingBarHttpClientModule} from '@ngx-loading-bar/http-client';
import {LoadingBarModule} from '@ngx-loading-bar/core';
import {KeyFilterModule} from 'primeng/keyfilter';
import {SignupComponent} from './signup/signup.component';
import {SignupOkayComponent} from './signup-okay/signup-okay.component';
import {SignupSecretComponent} from './signup-secret/signup-secret.component';
import {CheckboxModule} from 'primeng/checkbox';

@NgModule({ declarations: [
        AppComponent,
        HomeComponent,
        SignInComponent,
        TotpComponent,
        TotpAdditionalSecurityComponent,
        SignupComponent,
        SignupOkayComponent,
        SignupSecretComponent
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        FormsModule,
        InputTextModule,
        BrowserAnimationsModule,
        LoadingBarHttpClientModule,
        LoadingBarModule,
        KeyFilterModule,
        CheckboxModule,
        ButtonModule,
        ToastModule,
        AppRoutingModule], providers: [MessageService, provideHttpClient(withInterceptorsFromDi())] })
export class AppModule {
}
