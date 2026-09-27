import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { AdminAssetsComponent } from './admin-assets.component';
import { AssetService } from './asset.service';

describe('AdminAssetsComponent', () => {
  let component: AdminAssetsComponent;
  let fixture: ComponentFixture<AdminAssetsComponent>;
  let mockAssetService: {
    searchAssets: jest.Mock;
    formatFileSize: jest.Mock;
    getDownloadUrl: jest.Mock;
    getAssetStreamingUrl: jest.Mock;
    deleteAsset: jest.Mock;
    archiveAsset: jest.Mock;
    restoreAsset: jest.Mock;
  };

  beforeEach(async () => {
    mockAssetService = {
      searchAssets: jest.fn().mockReturnValue(
        of({
          content: [
            {
              id: 'ast-1',
              fileName: 'diagram.png',
              assetType: 'IMAGE',
              fileSize: 1024,
              status: 'ACTIVE',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
        })
      ),
      formatFileSize: jest.fn().mockReturnValue('1.00 KB'),
      getDownloadUrl: jest.fn().mockReturnValue('/api/v1/assets/ast-1/download'),
      getAssetStreamingUrl: jest.fn().mockReturnValue('/api/v1/assets/ast-1/download'),
      deleteAsset: jest.fn().mockReturnValue(of(undefined)),
      archiveAsset: jest.fn().mockReturnValue(of({})),
      restoreAsset: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [AdminAssetsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AssetService, useValue: mockAssetService },
        { provide: MatDialog, useValue: { open: jest.fn() } },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAssetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load assets on init and compute storage metrics', () => {
    expect(mockAssetService.searchAssets).toHaveBeenCalled();
    expect(component.assets().length).toBe(1);
    expect(component.imageCount()).toBe(1);
    expect(component.totalStorageFormatted()).toBe('1.00 KB');
  });

  it('should toggle view mode between grid and table', () => {
    component.viewMode.set('table');
    expect(component.viewMode()).toBe('table');
  });
});
