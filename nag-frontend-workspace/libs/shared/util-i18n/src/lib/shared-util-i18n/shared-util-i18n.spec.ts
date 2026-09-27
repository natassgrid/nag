import { TestBed } from '@angular/core/testing';
import {
  I18nService,
  NagTranslatePipe,
  SUPPORTED_LANGUAGES,
} from './shared-util-i18n';

describe('shared-util-i18n', () => {
  let service: I18nService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [I18nService, NagTranslatePipe],
    });
    service = TestBed.inject(I18nService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should initialize with default English language', () => {
    expect(service.currentLanguage()).toBe('en');
    expect(SUPPORTED_LANGUAGES.length).toBe(13);
  });

  it('should restore language from localStorage if valid', () => {
    localStorage.setItem('nag_i18n_lang', 'hi');
    const newService = new I18nService();
    expect(newService.currentLanguage()).toBe('hi');
  });

  it('should fallback to en if localStorage contains invalid language', () => {
    localStorage.setItem('nag_i18n_lang', 'invalid_lang');
    const newService = new I18nService();
    expect(newService.currentLanguage()).toBe('en');
  });

  it('should change language and persist to localStorage', () => {
    service.setLanguage('ta');
    expect(service.currentLanguage()).toBe('ta');
    expect(localStorage.getItem('nag_i18n_lang')).toBe('ta');
  });

  it('should translate key in active language or fallback to English / key', () => {
    expect(service.translate('common.save')).toBe('Save');

    service.setLanguage('hi');
    expect(service.translate('common.save')).toBe('सहेजें');

    // Key present in en but not in ta
    service.setLanguage('ta');
    expect(service.translate('exam.title')).toBe('National Assessment Grid');

    // Missing key everywhere
    expect(service.translate('non.existent.key')).toBe('non.existent.key');
  });

  it('should interpolate parameters in translations', () => {
    const translated = service.translate('common.search', { query: 'math' });
    expect(translated).toBe('Search...');
  });

  it('should compute reactive translated signal', () => {
    const signal = service.translateSignal('common.save');
    expect(signal()).toBe('Save');

    service.setLanguage('hi');
    expect(signal()).toBe('सहेजें');
  });

  it('should transform key using NagTranslatePipe', () => {
    const pipe = TestBed.inject(NagTranslatePipe);
    expect(pipe.transform('common.cancel')).toBe('Cancel');

    service.setLanguage('hi');
    expect(pipe.transform('common.cancel')).toBe('रद्द करें');
  });
});
