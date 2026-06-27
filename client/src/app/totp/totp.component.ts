import {Component, inject, OnInit} from '@angular/core';
import {take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from '../message.service';
import {noop} from 'rxjs';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-totp',
  templateUrl: './totp.component.html',
  imports: [FormsModule],
  styleUrl: './totp.component.css'
})
export class TotpComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    // are we in the correct phase
    this.authService.authentication$.pipe(take(1)).subscribe((flow) => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      } else if (flow !== 'TOTP') {
        this.router.navigate(['signin'], {replaceUrl: true});
      }
    });
  }

  async verifyTotp(code: string): Promise<void> {
    this.authService.verifyTotp(code).subscribe(noop, (err) => this.handleError(err));
  }

  async handleError(error: unknown): Promise<void> {
    let message: string;
    if (typeof error === 'string') {
      message = error;
    } else if (this.hasStatusText(error)) {
      message = `Unexpected error: ${error.statusText}`;
    } else {
      message = 'Unexpected error';
    }

    this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: message});
  }

  private hasStatusText(error: unknown): error is {statusText: string} {
    return (
      typeof error === 'object' &&
      error !== null &&
      'statusText' in error &&
      typeof error.statusText === 'string'
    );
  }
}
