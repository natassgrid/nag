import {
  Component,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';

export interface AssetRecord {
  id: string;
  name: string;
  type: 'IMAGE' | 'FORMULA' | 'AUDIO';
  size: string;
  associatedQuestion?: string;
}

@Component({
  selector: 'app-admin-assets',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
  ],
  templateUrl: './admin-assets.component.html',
  styleUrl: './admin-assets.component.scss',
})
export class AdminAssetsComponent {
  selectedType = 'ALL';

  assets = signal<AssetRecord[]>([
    {
      id: 'AST-101',
      name: 'Eigenvalue_Spectral_Decomp.svg',
      type: 'IMAGE',
      size: '42 KB',
      associatedQuestion: 'Q-8492 (Linear Algebra)',
    },
    {
      id: 'AST-102',
      name: 'RISC_Pipeline_Forwarding_Unit.png',
      type: 'IMAGE',
      size: '128 KB',
      associatedQuestion: 'Q-8493 (Computer Architecture)',
    },
    {
      id: 'AST-103',
      name: 'Benzene_Ring_Reaction_Mechanism.svg',
      type: 'IMAGE',
      size: '54 KB',
      associatedQuestion: 'Q-3921 (Chemistry)',
    },
    {
      id: 'AST-104',
      name: 'Listening_Comprehension_Passage.mp3',
      type: 'AUDIO',
      size: '2.4 MB',
      associatedQuestion: 'Q-7102 (English Linguistics)',
    },
  ]);

  filteredAssets = computed(() => {
    const type = this.selectedType;
    if (type === 'ALL') return this.assets();
    return this.assets().filter((a) => a.type === type);
  });

  uploadAsset(): void {
    const name = prompt('Enter filename of asset to anchor:');
    if (name) {
      this.assets.update((list) => [
        {
          id: 'AST-' + Math.floor(100 + Math.random() * 900),
          name,
          type: 'IMAGE',
          size: '64 KB',
        },
        ...list,
      ]);
    }
  }

  previewAsset(asset: AssetRecord): void {
    alert(`Asset Preview: ${asset.name} (${asset.type}, ${asset.size})`);
  }

  deleteAsset(id: string): void {
    this.assets.update((list) => list.filter((a) => a.id !== id));
  }
}
