import {provideZonelessChangeDetection} from '@angular/core';
import {provideHttpClient, withInterceptorsFromDi, withXhr} from '@angular/common/http';
import {bootstrapApplication} from '@angular/platform-browser';
import {routes} from './app/app.routes';
import {AppComponent} from './app/app.component';
import {provideRouter, withHashLocation} from '@angular/router';
import {provideLoadingBarInterceptor} from '@ngx-loading-bar/http-client';

bootstrapApplication(AppComponent, {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withXhr(), withInterceptorsFromDi()),
    provideLoadingBarInterceptor()
  ]
}).catch((err) => console.error(err));
