import {Component, OnInit} from '@angular/core';
import {take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MessageService} from 'primeng/api';
import {noop} from 'rxjs';

@Component({
  selector: 'app-totp',
  templateUrl: './totp.component.html',
  styleUrls: ['./totp.component.css']
})
export class TotpComponent implements OnInit {

  constructor(private readonly router: Router,
              private readonly messageService: MessageService,
              private readonly authService: AuthService) {
  }

  ngOnInit() {
    // are we in the correct phase
    this.authService.authentication$.pipe(take(1)).subscribe(flow => {
      if (flow === 'AUTHENTICATED') {
        this.router.navigate(['home'], {replaceUrl: true});
      } else if (flow !== 'TOTP') {
        this.router.navigate(['signin'], {replaceUrl: true});
      }
    });
  }

  async verifyTotp(code: string) {
    this.authService.verifyTotp(code).subscribe(noop, err => this.handleError(err));
  }

  async handleError(error: any) {
    let message: string;
    if (typeof error === 'string') {
      message = error;
    } else {
      message = `Unexpected error: ${error.statusText}`;
    }

    this.messageService.add({key: 'tst', severity: 'error', summary: 'Error', detail: message});
  }

}
