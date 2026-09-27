import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'nag-centre-facility-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './centre-facility-form.component.html',
  styleUrl: './centre-facility-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreFacilityFormComponent {
  readonly building = input<string>('');
  readonly floor = input<string>('');
  readonly labIdentifier = input<string>('');
  readonly totalCapacity = input<number>(250);
  readonly active = input<boolean>(true);

  readonly buildingChange = output<string>();
  readonly floorChange = output<string>();
  readonly labIdentifierChange = output<string>();
  readonly totalCapacityChange = output<number>();
  readonly activeChange = output<boolean>();
}
