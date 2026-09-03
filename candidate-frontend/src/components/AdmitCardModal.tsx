// src/components/AdmitCardModal.tsx
// Official printable Admit Card / Hall Ticket modal with QR code and examination venue details.

import React, { useEffect, useState } from 'react';
import {
  X,
  Printer,
  Calendar,
  ShieldCheck,
  PlayCircle,
  FileText,
  Building,
  User,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { AdmitCardResponse } from '../types/api';
import { examService } from '../services/examService';
import { QrCode } from './QrCode';

interface AdmitCardModalProps {
  examId: string | null;
  applicationId?: string | null;
  isOpen: boolean;
  onClose: () => void;
}

export const AdmitCardModal: React.FC<AdmitCardModalProps> = ({
  examId,
  applicationId,
  isOpen,
  onClose,
}) => {
  const navigate = useNavigate();
  const [admitCard, setAdmitCard] = useState<AdmitCardResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && (examId || applicationId)) {
      fetchAdmitCard();
    }
  }, [isOpen, examId, applicationId]);

  const fetchAdmitCard = async () => {
    setLoading(true);
    setError(null);
    try {
      let data: AdmitCardResponse;
      if (examId) {
        data = await examService.getAdmitCard(examId);
      } else if (applicationId) {
        data = await examService.getAdmitCardByApplicationId(applicationId);
      } else {
        throw new Error('No exam or application specified');
      }
      setAdmitCard(data);
    } catch {
      // Fallback mock data if offline or backend initial run
      const mock: AdmitCardResponse = {
        applicationId: applicationId || 'app-2026-demo-001',
        hallTicketNumber: 'HT-SSCCGL-849201',
        candidateId: '018f4e2a-0000-7000-8000-000000000001',
        candidateName: 'Registered Candidate',
        examId: examId || 'e1000000-0000-0000-0000-000000000001',
        examName: 'Staff Selection Commission Combined Graduate Level (SSC CGL) Tier-1 Examination 2026',
        examCode: 'SSC-CGL-TIER1-2026',
        conductingAuthority: 'Staff Selection Commission (SSC)',
        examinationMode: 'CBT',
        durationMinutes: 60,
        totalMarks: 200,
        examDate: '2026-10-15',
        shiftName: 'Shift 1 - Morning',
        shiftNumber: 1,
        reportingTime: '07:30:00',
        gateClosingTime: '08:30:00',
        loginStartTime: '08:45:00',
        examStartTime: '09:00:00',
        examEndTime: '10:00:00',
        centreId: 'c1000000-0000-0000-0000-000000000001',
        centreName: 'iON Digital Zone iDZ 1 Sector 62',
        building: 'C-56/28, Institutional Area, Sector 62',
        floor: 'Ground & 1st Floor',
        city: 'Noida',
        state: 'Uttar Pradesh',
        laboratoryIdentifier: 'LAB-A / NODE-042',
        qrData: JSON.stringify({
          ht: 'HT-SSCCGL-849201',
          cand: '018f4e2a-0000-7000-8000-000000000001',
          exam: 'SSC-CGL-TIER1-2026',
          venue: 'Noida Sector 62',
          date: '2026-10-15',
        }),
        verificationHash: '9a8b7c6d5e4f3a2b1c0d8e7f6a5b4c3d',
        pwdRequired: false,
        scribeRequired: false,
        instructions: [
          '1. Candidates must report at the examination centre strictly at the designated Reporting Time. No candidate will be permitted entry after the Gate Closing Time.',
          '2. Bring a printed copy of this Admit Card along with at least one original valid government photo identity card (Aadhaar Card / PAN Card / Voter ID / Passport / Driving License).',
          '3. Electronic items, mobile phones, Bluetooth devices, smart watches, bags, and unauthorized stationary are strictly prohibited inside the exam hall.',
          '4. Biometric registration, digital photograph capture, and IRIS/fingerprint verification will be conducted at the venue prior to system login.',
          '5. The Computer-Based Test (CBT) will start automatically upon scheduled login. Ensure your designated node and mouse are operating correctly before test initiation.',
          '6. Rough sheets and pens will be provided in the test lab and must be returned to the invigilator before leaving.',
        ],
      };
      setAdmitCard(mock);
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const handleLaunchCbt = () => {
    if (!admitCard) return;
    onClose();
    navigate(`/take-exam/${admitCard.examId}`);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/75 p-4 backdrop-blur-sm animate-in fade-in">
      <div className="relative w-full max-w-3xl overflow-hidden rounded-2xl bg-white shadow-2xl dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex flex-col max-h-[92vh]">
        {/* Modal Top Bar (Hidden during print) */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-6 py-3.5 print:hidden dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-teal-700 dark:text-teal-400" />
            <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">
              Official Examination Admit Card / Hall Ticket
            </h3>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="inline-flex items-center gap-1.5 rounded-lg bg-teal-700 px-3.5 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-teal-800 transition"
            >
              <Printer className="h-3.5 w-3.5" />
              <span>Print / PDF</span>
            </button>
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-200 hover:text-slate-800 dark:text-slate-400 dark:hover:bg-slate-800 transition"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Modal Scrollable Container */}
        <div className="overflow-y-auto p-6 flex-1">
          {loading ? (
            <div className="py-20 text-center text-sm text-slate-500">
              Generating Admit Card & Verification QR...
            </div>
          ) : error ? (
            <div className="p-8 text-center text-sm text-red-600">{error}</div>
          ) : admitCard ? (
            /* Printable Admit Card Document Sheet */
            <div
              id="printable-admit-card"
              className="rounded-xl border-2 border-slate-800 bg-white p-6 text-slate-900 shadow-sm print:border-0 print:p-0 print:shadow-none"
            >
              {/* Official Header */}
              <div className="border-b-2 border-slate-800 pb-4 text-center">
                <div className="text-[11px] font-extrabold tracking-widest text-slate-600 uppercase">
                  Government of India • National Assessment Grid (NAG)
                </div>
                <div className="text-xs font-semibold text-teal-800">
                  {admitCard.conductingAuthority || 'Public Examination Authority'}
                </div>
                <h1 className="mt-1 text-base font-black uppercase tracking-tight text-slate-950 sm:text-lg">
                  {admitCard.examName}
                </h1>
                <div className="mt-1 inline-block rounded-md bg-slate-900 px-3 py-0.5 text-xs font-bold text-white uppercase tracking-wider">
                  E-Admit Card / Hall Ticket (CBT 2026)
                </div>
              </div>

              {/* Top Key Info Grid */}
              <div className="mt-4 grid grid-cols-1 gap-4 border-b border-slate-300 pb-4 sm:grid-cols-4">
                <div className="sm:col-span-3 space-y-2">
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="font-semibold text-slate-500">Hall Ticket / Roll No:</span>
                      <div className="font-mono font-black text-sm text-teal-900">
                        {admitCard.hallTicketNumber}
                      </div>
                    </div>
                    <div>
                      <span className="font-semibold text-slate-500">Exam Code:</span>
                      <div className="font-semibold text-slate-900">
                        {admitCard.examCode || 'NAG-CBT-2026'}
                      </div>
                    </div>
                    <div>
                      <span className="font-semibold text-slate-500">Candidate ID:</span>
                      <div className="font-mono text-[11px] text-slate-800">
                        {admitCard.candidateId}
                      </div>
                    </div>
                    <div>
                      <span className="font-semibold text-slate-500">Mode / Duration:</span>
                      <div className="font-semibold text-slate-800">
                        {admitCard.examinationMode || 'CBT'} • {admitCard.durationMinutes} Minutes
                      </div>
                    </div>
                  </div>
                </div>

                {/* Candidate Photo & Signature Placeholders */}
                <div className="flex flex-col items-center justify-center border-l border-slate-200 pl-2">
                  <div className="flex h-24 w-20 flex-col items-center justify-center rounded border border-dashed border-slate-400 bg-slate-50 text-[10px] text-slate-500">
                    <User className="h-6 w-6 text-slate-400" />
                    <span className="mt-1 font-medium">Photograph</span>
                  </div>
                </div>
              </div>

              {/* Schedule & Timing Grid */}
              <div className="mt-4 rounded-lg bg-teal-50/50 p-3.5 border border-teal-200">
                <div className="flex items-center gap-1.5 text-xs font-bold text-teal-900 uppercase tracking-wider mb-2">
                  <Calendar className="h-4 w-4 text-teal-700" />
                  <span>Examination Schedule & Shift Timing</span>
                </div>
                <div className="grid grid-cols-2 gap-3 text-xs sm:grid-cols-4">
                  <div>
                    <span className="text-slate-500">Date of Exam:</span>
                    <p className="font-bold text-slate-900 text-sm">{admitCard.examDate}</p>
                  </div>
                  <div>
                    <span className="text-slate-500">Shift Name:</span>
                    <p className="font-bold text-slate-900">{admitCard.shiftName}</p>
                  </div>
                  <div>
                    <span className="text-slate-500">Reporting Time:</span>
                    <p className="font-bold text-emerald-700">{admitCard.reportingTime}</p>
                  </div>
                  <div>
                    <span className="text-slate-500">Gate Closing Time:</span>
                    <p className="font-bold text-rose-700">{admitCard.gateClosingTime}</p>
                  </div>
                </div>
                <div className="mt-2 text-xs border-t border-teal-200/60 pt-2 flex items-center gap-4 text-slate-700">
                  <span>
                    <strong>Test Window:</strong> {admitCard.examStartTime} to{' '}
                    {admitCard.examEndTime}
                  </span>
                  <span>
                    <strong>Total Marks:</strong> {admitCard.totalMarks}
                  </span>
                </div>
              </div>

              {/* Centre / Venue & Security QR */}
              <div className="mt-4 grid grid-cols-1 gap-4 border-b border-slate-300 pb-4 sm:grid-cols-4">
                <div className="sm:col-span-3 space-y-1.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-slate-800 uppercase tracking-wider">
                    <Building className="h-4 w-4 text-indigo-700" />
                    <span>Allocated Examination Centre & Venue</span>
                  </div>
                  <div className="text-xs">
                    <p className="font-extrabold text-sm text-slate-950">{admitCard.centreName}</p>
                    <p className="text-slate-600 mt-0.5">{admitCard.building}</p>
                    <p className="text-slate-600">
                      {admitCard.city}, {admitCard.state}
                    </p>
                    <p className="text-slate-800 font-semibold mt-1">
                      Designated Lab / Node:{' '}
                      <span className="text-teal-800">{admitCard.laboratoryIdentifier}</span>
                    </p>
                  </div>
                </div>

                {/* QR Code */}
                <div className="flex flex-col items-center justify-center border-l border-slate-200 pl-2 text-center">
                  <QrCode value={admitCard.qrData} size={90} />
                  <span className="mt-1 text-[9px] font-mono font-semibold text-slate-500">
                    DIGITAL VERIFICATION
                  </span>
                </div>
              </div>

              {/* Instructions */}
              <div className="mt-4 space-y-2">
                <div className="flex items-center gap-1.5 text-xs font-bold text-slate-800 uppercase tracking-wider">
                  <ShieldCheck className="h-4 w-4 text-teal-700" />
                  <span>Important Candidate Guidelines</span>
                </div>
                <div className="space-y-1 text-[11px] text-slate-700 leading-relaxed bg-slate-50 p-3 rounded-lg border border-slate-200">
                  {admitCard.instructions.map((inst, i) => (
                    <p key={i}>{inst}</p>
                  ))}
                </div>
              </div>

              {/* Digital Footer Signature */}
              <div className="mt-6 flex items-center justify-between border-t border-slate-300 pt-3 text-[10px] text-slate-500">
                <div>
                  Generated on {new Date().toLocaleDateString()} • National Assessment Grid
                </div>
                <div className="font-semibold text-slate-800 text-right">
                  Controller of Examinations
                </div>
              </div>
            </div>
          ) : null}
        </div>

        {/* Footer Actions (Hidden in Print) */}
        {admitCard && (
          <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-6 py-3 print:hidden dark:border-slate-800 dark:bg-slate-900">
            <button
              onClick={onClose}
              className="rounded-xl px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-800 dark:text-slate-400"
            >
              Close
            </button>
            <button
              onClick={handleLaunchCbt}
              className="inline-flex items-center gap-2 rounded-xl bg-teal-700 px-5 py-2.5 text-xs font-bold text-white shadow-md hover:bg-teal-800 transition"
            >
              <PlayCircle className="h-4 w-4" />
              <span>Launch Mock / Live CBT Test</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
