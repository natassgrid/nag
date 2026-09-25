export interface ScheduleResponse {
  id: string;
  examinationId: string;
  scheduleName: string;
  scheduleVersion: number;
  notificationNumber?: string;
  examDate: string;
  reserveDate?: string;
  timeZone: string;
  status: string;
  changeReason?: string;
  effectiveFrom?: string;
  previousVersionId?: string;
  createdBy?: string;
  modifiedBy?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateScheduleRequest {
  scheduleName: string;
  notificationNumber?: string;
  examDate: string;
  reserveDate?: string;
  timeZone: string;
}

export interface ScheduleTransitionRequest {
  targetStatus: string;
  comment?: string;
}

export interface AmendScheduleRequest {
  changeReason: string;
  scheduleName: string;
  notificationNumber?: string;
  examDate: string;
  reserveDate?: string;
  effectiveFrom?: string;
  timeZone: string;
}

export interface ShiftResponse {
  id: string;
  scheduleId: string;
  shiftNumber: number;
  shiftName?: string;
  reportingTime: string;
  gateClosingTime: string;
  loginStartTime: string;
  examStartTime: string;
  examEndTime: string;
  exitTime?: string;
  durationMinutes: number;
  bufferMinutes: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateShiftRequest {
  shiftNumber: number;
  shiftName?: string;
  reportingTime: string;
  gateClosingTime: string;
  loginStartTime: string;
  examStartTime: string;
  examEndTime: string;
  exitTime?: string;
  durationMinutes: number;
  bufferMinutes: number;
}
