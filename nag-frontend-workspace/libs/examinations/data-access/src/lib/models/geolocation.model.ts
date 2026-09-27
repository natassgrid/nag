export interface GeoCountry {
  id: number;
  name: string;
  iso2: string;
  iso3: string;
  phoneCode: string;
  capital: string;
  currency: string;
  region: string;
  subregion: string;
  active: boolean;
}

export interface GeoState {
  id: number;
  name: string;
  countryId: number;
  stateCode: string;
  type: string;
  active: boolean;
}

export interface GeoCity {
  id: number;
  name: string;
  stateId: number;
  countryId: number;
  latitude: number;
  longitude: number;
  active: boolean;
}
