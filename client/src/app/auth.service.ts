import {inject, Injectable} from '@angular/core';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {HttpClient, HttpParams} from '@angular/common/http';
import {catchError, map, share, switchMap, tap} from 'rxjs/operators';

export type AuthenticationFlow =
  | 'NOT_AUTHENTICATED'
  | 'AUTHENTICATED'
  | 'TOTP'
  | 'TOTP_ADDITIONAL_SECURITY';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  signupResponse: SignupResponse | null = null;
  private readonly httpClient = inject(HttpClient);
  private readonly authenticationSubject = new BehaviorSubject<AuthenticationFlow | null>(null);
  readonly authentication$ = this.authenticationSubject.asObservable();
  private readonly authenticationCall$: Observable<AuthenticationFlow>;

  constructor() {
    this.authenticationCall$ = this.httpClient
      .get<AuthenticationFlow>('authenticate', {
        withCredentials: true
      })
      .pipe(
        tap((flow) => this.authenticationSubject.next(flow)),
        catchError(() => this.notAuthenticated()),
        share()
      );
  }

  authenticate(): Observable<AuthenticationFlow> {
    return this.authenticationCall$;
  }

  isAuthenticated(): boolean {
    return this.authenticationSubject.getValue() === 'AUTHENTICATED';
  }

  signin(username: string, password: string): Observable<AuthenticationFlow> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.csrfToken().pipe(
      switchMap(() => this.httpClient.post<AuthenticationFlow>('signin', body, {withCredentials: true})),
      tap((flow) => this.authenticationSubject.next(flow)),
      catchError(() => this.notAuthenticated())
    );
  }

  verifyTotp(code: string): Observable<AuthenticationFlow> {
    const body = new HttpParams().set('code', code);
    return this.csrfToken().pipe(
      switchMap(() => this.httpClient.post<AuthenticationFlow>('verify-totp', body, {withCredentials: true})),
      tap((flow) => this.authenticationSubject.next(flow)),
      catchError(() => this.notAuthenticated())
    );
  }

  verifyTotpAdditionalSecurity(
    code1: string,
    code2: string,
    code3: string
  ): Observable<AuthenticationFlow> {
    const body = new HttpParams().set('code1', code1).set('code2', code2).set('code3', code3);
    return this.csrfToken().pipe(
      switchMap(() =>
        this.httpClient.post<AuthenticationFlow>('verify-totp-additional-security', body, {
          withCredentials: true
        })
      ),
      tap((flow) => this.authenticationSubject.next(flow)),
      catchError(() => this.notAuthenticated())
    );
  }

  signout(): Observable<void> {
    return this.csrfToken().pipe(
      switchMap(() => this.httpClient.post<void>('logout', null, {withCredentials: true})),
      tap(() => this.authenticationSubject.next('NOT_AUTHENTICATED'))
    );
  }

  signup(username: string, password: string, totp: boolean): Observable<SignupResponse> {
    const body = new HttpParams()
      .set('username', username)
      .set('password', password)
      .set('totp', `${totp ? 'true' : 'false'}`);
    return this.csrfToken().pipe(
      switchMap(() => this.httpClient.post<SignupResponse>('signup', body, {withCredentials: true}))
    );
  }

  signupVerifyCode(username: string, code: string): Observable<boolean> {
    const body = new HttpParams().set('username', username).set('code', code);
    return this.csrfToken().pipe(
      switchMap(() =>
        this.httpClient.post('signup-confirm-secret', body, {
          responseType: 'text',
          withCredentials: true
        })
      ),
      map((response) => response === 'true')
    );
  }

  private csrfToken(): Observable<string> {
    return this.httpClient
      .get('csrf', {
        responseType: 'text',
        withCredentials: true
      })
      .pipe(catchError(() => of('')));
  }

  private notAuthenticated(): Observable<AuthenticationFlow> {
    this.authenticationSubject.next('NOT_AUTHENTICATED');
    return of('NOT_AUTHENTICATED');
  }
}

export interface SignupResponse {
  status: 'OK' | 'USERNAME_TAKEN' | 'WEAK_PASSWORD';
  username?: string;
  secret?: string;
}
