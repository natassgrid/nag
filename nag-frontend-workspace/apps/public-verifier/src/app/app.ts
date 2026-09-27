import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { GlobalNotificationComponent } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule, GlobalNotificationComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
