import {ChangeDetectionStrategy, Component} from '@angular/core';
import {ButtonDirective} from 'primeng/button';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-signup-okay',
  templateUrl: './signup-okay.component.html',
  imports: [ButtonDirective, RouterLink],
  styleUrl: './signup-okay.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SignupOkayComponent {}
