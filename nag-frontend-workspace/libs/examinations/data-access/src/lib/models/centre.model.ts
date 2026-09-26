export interface CentreResponse {
  id: string;
  countryId?: number;
  countryName?: string;
  stateId?: number;
  stateName?: string;
  cityId?: number;
  cityName?: string;
  region?: string;
  state: string;
  district?: string;
  city: string;
  centreName: string;
  building?: string;
  floor?: string;
  laboratoryIdentifier?: string;
  totalCapacity: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCentreRequest {
  countryId?: number;
  stateId?: number;
  cityId?: number;
  region?: string;
  state: string;
  district?: string;
  city: string;
  centreName: string;
  building?: string;
  floor?: string;
  laboratoryIdentifier?: string;
  totalCapacity: number;
  active: boolean;
}

export interface SeatAllocationResponse {
  id: string;
  shiftId: string;
  centreId: string;
  centreName?: string;
  totalSeats: number;
  availableSeats: number;
  reservedSeats: number;
  pwdSeats: number;
  emergencyBufferSeats: number;
  femaleReservedSeats: number;
  specialCategorySeats: number;
  createdAt: string;
  updatedAt: string;
}

export interface SeatAllocationRequest {
  centreId: string;
  totalSeats: number;
  availableSeats: number;
  reservedSeats: number;
  pwdSeats: number;
  emergencyBufferSeats: number;
  femaleReservedSeats: number;
  specialCategorySeats: number;
}
