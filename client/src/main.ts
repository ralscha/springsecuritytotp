import {provideZonelessChangeDetection} from '@angular/core';
import {provideHttpClient, withInterceptorsFromDi, withXhr} from '@angular/common/http';
import {bootstrapApplication} from '@angular/platform-browser';
import {routes} from './app/app.routes';
import {AppComponent} from './app/app.component';
import {provideRouter, withHashLocation} from '@angular/router';

bootstrapApplication(AppComponent, {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withXhr(), withInterceptorsFromDi())
  ]
}).catch((err) => console.error(err));
