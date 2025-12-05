import {provideZonelessChangeDetection} from '@angular/core';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {bootstrapApplication} from '@angular/platform-browser';
import {routes} from './app/app.routes';
import {AppComponent} from './app/app.component';
import {provideRouter, withHashLocation} from '@angular/router';
import {MessageService} from 'primeng/api';
import {providePrimeNG} from 'primeng/config';
import Aura from '@primeuix/themes/aura';

bootstrapApplication(AppComponent, {
  providers: [
    provideZonelessChangeDetection(),
    MessageService,
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptorsFromDi()),
    providePrimeNG({
      theme: {
        preset: Aura
      }
    })
  ]
}).catch((err) => console.error(err));
