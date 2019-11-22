import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {HttpClient, HttpParams} from '@angular/common/http';
import {catchError, map, share, tap} from 'rxjs/operators';

export type AuthenticationFlow = 'NOT_AUTHENTICATED' | 'AUTHENTICATED' | 'TOTP' | 'TOTP_ADDITIONAL_SECURITY';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  signupResponse: SignupResponse = null;
  private readonly authenticationSubject = new BehaviorSubject<AuthenticationFlow>(null);
  readonly authentication$ = this.authenticationSubject.asObservable();
  private readonly authenticationCall$: Observable<AuthenticationFlow>;

  constructor(private readonly httpClient: HttpClient) {
    this.authenticationCall$ = this.httpClient.get<AuthenticationFlow>('authenticate', {
      withCredentials: true
    })
      .pipe(
        tap(flow => this.authenticationSubject.next(flow)),
        catchError(_ => of('NOT_AUTHENTICATED' as AuthenticationFlow)),
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
    return this.httpClient.post<AuthenticationFlow>('signin', body, {withCredentials: true})
      .pipe(
        tap(flow => this.authenticationSubject.next(flow)),
        catchError(_ => of('NOT_AUTHENTICATED' as AuthenticationFlow))
      );
  }

  verifyTotp(code: string): Observable<AuthenticationFlow> {
    const body = new HttpParams().set('code', code);
    return this.httpClient.post<AuthenticationFlow>('verify-totp', body, {withCredentials: true})
      .pipe(
        tap(flow => this.authenticationSubject.next(flow)),
        catchError(_ => of('NOT_AUTHENTICATED' as AuthenticationFlow))
      );
  }

  verifyTotpAdditionalSecurity(code1: string, code2: string, code3: string): Observable<AuthenticationFlow> {
    const body = new HttpParams().set('code1', code1).set('code2', code2).set('code3', code3);
    return this.httpClient.post<AuthenticationFlow>('verify-totp-additional-security',
      body, {withCredentials: true})
      .pipe(
        tap(flow => this.authenticationSubject.next(flow)),
        catchError(_ => of('NOT_AUTHENTICATED' as AuthenticationFlow))
      );
  }

  signout(): Observable<void> {
    return this.httpClient.get<void>('logout', {withCredentials: true})
      .pipe(
        tap(() => this.authenticationSubject.next('NOT_AUTHENTICATED'))
      );
  }

  signup(username: string, password: string, totp: boolean): Observable<SignupResponse> {
    const body = new HttpParams().set('username', username).set('password', password)
      .set('totp', `${totp ? 'true' : 'false'}`);
    return this.httpClient.post<SignupResponse>('signup', body);
  }

  signupVerifyCode(username: string, code: string): Observable<boolean> {
    const body = new HttpParams().set('username', username).set('code', code);
    return this.httpClient.post('signup-confirm-secret', body, {responseType: 'text'})
      .pipe(
        map(response => response === 'true'),
      );
  }
}

export interface SignupResponse {
  status: 'OK' | 'USERNAME_TAKEN' | 'WEAK_PASSWORD';
  username?: string;
  secret?: string;
}
