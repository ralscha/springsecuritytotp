import {MessageService} from 'primeng/api';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {bootstrapApplication, BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {provideAnimations} from '@angular/platform-browser/animations';
import {LoadingBarHttpClientModule} from '@ngx-loading-bar/http-client';
import {LoadingBarModule} from '@ngx-loading-bar/core';
import {KeyFilterModule} from 'primeng/keyfilter';
import {CheckboxModule} from 'primeng/checkbox';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {AppRoutingModule} from './app/app-routing.module';
import {AppComponent} from './app/app.component';
import {importProvidersFrom} from '@angular/core';


bootstrapApplication(AppComponent, {
  providers: [
    importProvidersFrom(BrowserModule, FormsModule, InputTextModule, LoadingBarHttpClientModule, LoadingBarModule, KeyFilterModule, CheckboxModule, ButtonModule, ToastModule, AppRoutingModule),
    MessageService, provideHttpClient(withInterceptorsFromDi()),
    provideAnimations()
  ]
})
  .catch(err => console.error(err));
