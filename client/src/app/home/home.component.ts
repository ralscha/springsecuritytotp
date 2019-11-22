import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  shift: string = null;

  constructor(private readonly httpClient: HttpClient) {
  }

  ngOnInit() {
    this.httpClient.get(`${environment.SERVER_URL}/totp-shift`, {
      responseType: 'text',
      withCredentials: true
    }).subscribe(shiftResponse => this.shift = shiftResponse);
  }

}
