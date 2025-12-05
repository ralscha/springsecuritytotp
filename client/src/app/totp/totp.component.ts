import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from 'primeng/api';
import {noop} from 'rxjs';
import {FormsModule} from '@angular/forms';
import {KeyFilterModule} from 'primeng/keyfilter';
import {InputTextModule} from 'primeng/inputtext';

import {ButtonDirective} from 'primeng/button';

@Component({
  selector: 'app-totp',
  templateUrl: './totp.component.html',
  imports: [FormsModule, InputTextModule, KeyFilterModule, ButtonDirective],
  styleUrl: './totp.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
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

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async handleError(error: any): Promise<void> {
    let message: string;
    if (typeof error === 'string') {
      message = error;
    } else {
      message = `Unexpected error: ${error.statusText}`;
    }

    this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: message});
  }
}
