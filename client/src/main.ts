import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {bootstrapApplication} from '@angular/platform-browser';
import {provideAnimations} from '@angular/platform-browser/animations';
import {routes} from './app/app.routes';
import {AppComponent} from './app/app.component';
import {provideRouter, withHashLocation} from "@angular/router";
import {MessageService} from "primeng/api";


bootstrapApplication(AppComponent, {
  providers: [
    MessageService,
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations()
  ]
})
  .catch(err => console.error(err));
