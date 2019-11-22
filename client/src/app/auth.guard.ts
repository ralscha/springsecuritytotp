import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {Observable} from 'rxjs';
import {AuthService} from './auth.service';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private readonly authService: AuthService,
              private readonly router: Router) {
  }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    if (this.authService.isAuthenticated()) {
      return true;
    }

    return this.authService.authenticate().pipe(map(flow => {
        switch (flow) {
          case 'AUTHENTICATED':
            return true;
          case 'NOT_AUTHENTICATED':
            return this.router.createUrlTree(['signin']);
          case 'TOTP':
            return this.router.createUrlTree(['totp']);
          case 'TOTP_ADDITIONAL_SECURITY':
            return this.router.createUrlTree(['totp-additional-security']);
        }
      }
    ));
  }
}
