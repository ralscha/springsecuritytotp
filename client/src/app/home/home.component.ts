import {Component, inject, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';


@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  imports: []
})
export class HomeComponent implements OnInit {
  shift: string | null = null;
  private readonly httpClient = inject(HttpClient);

  ngOnInit(): void {
    this.httpClient.get('totp-shift', {
      responseType: 'text',
      withCredentials: true
    }).subscribe(shiftResponse => this.shift = shiftResponse);
  }

}
