import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  imports: [
    NgIf
  ]
})
export class HomeComponent implements OnInit {

  shift: string | null = null;

  constructor(private readonly httpClient: HttpClient) {
  }

  ngOnInit(): void {
    this.httpClient.get('totp-shift', {
      responseType: 'text',
      withCredentials: true
    }).subscribe(shiftResponse => this.shift = shiftResponse);
  }

}
