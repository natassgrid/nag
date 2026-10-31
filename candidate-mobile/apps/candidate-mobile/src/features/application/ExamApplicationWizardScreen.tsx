import React, { useState, useEffect } from 'react';
import {
  ApplicationWizardController,
  ApplicationWizardState,
  ALL_CENTRES_INDIA,
  STANDARD_SHIFTS,
} from './application-wizard-controller';

export interface ExamApplicationWizardScreenProps {
  controller: ApplicationWizardController;
  onApplicationCompleted?: (hallTicketNumber: string) => void;
  onCancel?: () => void;
}

export const ExamApplicationWizardScreen: React.FC<ExamApplicationWizardScreenProps> = ({
  controller,
  onApplicationCompleted,
  onCancel,
}) => {
  const [state, setState] = useState<ApplicationWizardState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s: ApplicationWizardState) => {
      setState(s);
    });
  }, [controller]);

  const handleNext = () => {
    controller.nextStep();
  };

  const handleBack = () => {
    controller.prevStep();
  };

  const handleSubmit = async () => {
    const receipt = await controller.submitApplication();
    if (receipt && onApplicationCompleted) {
      onApplicationCompleted(receipt.hallTicketNumber);
    }
  };

  return (
    <div
      data-testid="exam-application-wizard-screen"
      style={{
        maxWidth: '520px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Title Header */}
      <div style={{ marginBottom: '16px' }}>
        <span style={{ fontSize: '11px', fontWeight: '700', color: '#2B6CB0', textTransform: 'uppercase' }}>
          National Assessment Grid
        </span>
        <h2 style={{ fontSize: '18px', fontWeight: '700', margin: '2px 0 4px 0', color: '#1A365D' }}>
          {state.examTitle}
        </h2>
        <span style={{ fontSize: '12px', color: '#718096' }}>
          Online Application Form & Centre Allocation
        </span>
      </div>

      {/* 3-Step Progress Indicator */}
      <div
        data-testid="wizard-step-indicator"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '20px',
          padding: '12px',
          backgroundColor: '#F7FAFC',
          borderRadius: '8px',
          border: '1px solid #E2E8F0',
        }}
      >
        {[
          { num: 1, label: 'Centres' },
          { num: 2, label: 'Shift & PwD' },
          { num: 3, label: 'Review' },
        ].map((s, idx) => {
          const isActive = state.currentStep === s.num;
          const isDone =
            (state.currentStep === 2 && s.num === 1) ||
            (state.currentStep === 3 && s.num <= 2) ||
            state.currentStep === 'SUCCESS';

          return (
            <React.Fragment key={s.num}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span
                  data-testid={`step-badge-${s.num}`}
                  style={{
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px',
                    fontWeight: '700',
                    backgroundColor: isActive ? '#2B6CB0' : isDone ? '#38A169' : '#CBD5E0',
                    color: '#FFFFFF',
                  }}
                >
                  {isDone ? '✓' : s.num}
                </span>
                <span
                  style={{
                    fontSize: '12px',
                    fontWeight: isActive ? '700' : '500',
                    color: isActive ? '#2B6CB0' : '#4A5568',
                  }}
                >
                  {s.label}
                </span>
              </div>
              {idx < 2 && (
                <div
                  style={{
                    flex: 1,
                    height: '2px',
                    margin: '0 8px',
                    backgroundColor: isDone ? '#38A169' : '#E2E8F0',
                  }}
                />
              )}
            </React.Fragment>
          );
        })}
      </div>

      {/* Error Banner */}
      {state.error && (
        <div
          data-testid="wizard-error-banner"
          style={{
            padding: '10px 14px',
            marginBottom: '16px',
            backgroundColor: '#FFF5F5',
            color: '#C53030',
            borderRadius: '6px',
            fontSize: '13px',
            border: '1px solid #FEB2B2',
          }}
        >
          {state.error}
        </div>
      )}

      {/* STEP 1: Examination Centre Preferences */}
      {state.currentStep === 1 && (
        <div data-testid="wizard-step-1-content">
          <h3 style={{ fontSize: '15px', fontWeight: '700', marginBottom: '8px' }}>
            Step 1: Choose 3 Preferred Examination Centres
          </h3>
          <p style={{ fontSize: '12px', color: '#718096', marginBottom: '16px', lineHeight: 1.4 }}>
            Select 3 distinct examination centre locations in order of preference. The system requires all 3 choices to be unique.
          </p>

          <div style={{ marginBottom: '14px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '4px' }}>
              1st Choice Centre (Highest Preference)
            </label>
            <select
              data-testid="select-centre-1"
              value={state.centre1Id}
              onChange={(e) => controller.setCentrePreference(1, e.target.value)}
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #CBD5E0', fontSize: '13px' }}
            >
              {ALL_CENTRES_INDIA.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} — {c.city}, {c.state} ({c.code})
                </option>
              ))}
            </select>
          </div>

          <div style={{ marginBottom: '14px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '4px' }}>
              2nd Choice Centre
            </label>
            <select
              data-testid="select-centre-2"
              value={state.centre2Id}
              onChange={(e) => controller.setCentrePreference(2, e.target.value)}
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #CBD5E0', fontSize: '13px' }}
            >
              {ALL_CENTRES_INDIA.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} — {c.city}, {c.state} ({c.code})
                </option>
              ))}
            </select>
          </div>

          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '4px' }}>
              3rd Choice Centre
            </label>
            <select
              data-testid="select-centre-3"
              value={state.centre3Id}
              onChange={(e) => controller.setCentrePreference(3, e.target.value)}
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #CBD5E0', fontSize: '13px' }}
            >
              {ALL_CENTRES_INDIA.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} — {c.city}, {c.state} ({c.code})
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            {onCancel && (
              <button
                type="button"
                onClick={onCancel}
                style={{ padding: '10px 16px', background: 'none', border: '1px solid #CBD5E0', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' }}
              >
                Cancel
              </button>
            )}
            <button
              data-testid="step1-next-btn"
              type="button"
              onClick={handleNext}
              style={{
                marginLeft: 'auto',
                padding: '10px 20px',
                backgroundColor: '#2B6CB0',
                color: '#FFFFFF',
                borderRadius: '6px',
                border: 'none',
                fontWeight: '600',
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Continue to Step 2 →
            </button>
          </div>
        </div>
      )}

      {/* STEP 2: Shift Preferences & PwD/Scribe Assistance */}
      {state.currentStep === 2 && (
        <div data-testid="wizard-step-2-content">
          <h3 style={{ fontSize: '15px', fontWeight: '700', marginBottom: '8px' }}>
            Step 2: Examination Shift & PwD Accommodations
          </h3>

          <div style={{ marginBottom: '18px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '8px' }}>
              Select Shift Preference:
            </label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {STANDARD_SHIFTS.map((shift) => {
                const isSelected = state.selectedShiftId === shift.id;
                return (
                  <label
                    key={shift.id}
                    data-testid={`shift-card-${shift.id}`}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      padding: '12px',
                      borderRadius: '8px',
                      border: isSelected ? '2px solid #2B6CB0' : '1px solid #CBD5E0',
                      backgroundColor: isSelected ? '#EBF8FF' : '#FFFFFF',
                      cursor: 'pointer',
                    }}
                  >
                    <input
                      type="radio"
                      name="shiftPreference"
                      value={shift.id}
                      checked={isSelected}
                      onChange={() => controller.setShiftPreference(shift.id)}
                      style={{ marginTop: '3px', marginRight: '10px' }}
                    />
                    <div>
                      <strong style={{ fontSize: '13px', color: '#1A202C' }}>{shift.name}</strong>
                      <div style={{ fontSize: '12px', color: '#2B6CB0', marginTop: '2px' }}>
                        Timing: {shift.timing}
                      </div>
                      <div style={{ fontSize: '11px', color: '#718096' }}>{shift.reporting}</div>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>

          {/* PwD Section */}
          <div
            style={{
              padding: '14px',
              backgroundColor: '#F7FAFC',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
              marginBottom: '20px',
            }}
          >
            <label style={{ display: 'flex', alignItems: 'center', fontSize: '13px', fontWeight: '600', cursor: 'pointer' }}>
              <input
                data-testid="pwd-required-checkbox"
                type="checkbox"
                checked={state.pwdRequired}
                onChange={(e) =>
                  controller.setPwdAccommodations(
                    e.target.checked,
                    state.pwdCategory,
                    state.scribeRequired,
                    state.scribeLanguage
                  )
                }
                style={{ marginRight: '8px' }}
              />
              Person with Benchmark Disability (PwD / Divyangjan)
            </label>

            {state.pwdRequired && (
              <div style={{ marginTop: '12px', paddingLeft: '24px' }}>
                <div style={{ marginBottom: '10px' }}>
                  <label style={{ display: 'block', fontSize: '12px', color: '#4A5568', marginBottom: '4px' }}>
                    Disability Category:
                  </label>
                  <select
                    data-testid="pwd-category-select"
                    value={state.pwdCategory || ''}
                    onChange={(e) =>
                      controller.setPwdAccommodations(
                        true,
                        e.target.value,
                        state.scribeRequired,
                        state.scribeLanguage
                      )
                    }
                    style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #CBD5E0', fontSize: '12px' }}
                  >
                    <option value="">Select Disability Category</option>
                    <option value="LOCOMOTOR">Locomotor Disability (≥40%)</option>
                    <option value="VISUAL_IMPAIRMENT">Blindness / Low Vision</option>
                    <option value="HEARING_IMPAIRMENT">Deaf and Hard of Hearing</option>
                    <option value="SPEECH_LANGUAGE">Speech and Language Disability</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'flex', alignItems: 'center', fontSize: '12px', color: '#4A5568', cursor: 'pointer' }}>
                    <input
                      data-testid="scribe-required-checkbox"
                      type="checkbox"
                      checked={state.scribeRequired}
                      onChange={(e) =>
                        controller.setPwdAccommodations(
                          true,
                          state.pwdCategory,
                          e.target.checked,
                          state.scribeLanguage
                        )
                      }
                      style={{ marginRight: '6px' }}
                    />
                    Request Scribe Assistance & Compensatory Time (+20 mins/hr)
                  </label>
                </div>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <button
              data-testid="step2-back-btn"
              type="button"
              onClick={handleBack}
              style={{ padding: '10px 16px', background: 'none', border: '1px solid #CBD5E0', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' }}
            >
              ← Back to Centres
            </button>
            <button
              data-testid="step2-next-btn"
              type="button"
              onClick={handleNext}
              style={{
                padding: '10px 20px',
                backgroundColor: '#2B6CB0',
                color: '#FFFFFF',
                borderRadius: '6px',
                border: 'none',
                fontWeight: '600',
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Review Application →
            </button>
          </div>
        </div>
      )}

      {/* STEP 3: Review & Submission */}
      {state.currentStep === 3 && (
        <div data-testid="wizard-step-3-content">
          <h3 style={{ fontSize: '15px', fontWeight: '700', marginBottom: '8px' }}>
            Step 3: Review Details & Submit Application
          </h3>

          <div
            style={{
              padding: '14px',
              backgroundColor: '#FFFFFF',
              borderRadius: '8px',
              border: '1px solid #CBD5E0',
              marginBottom: '16px',
            }}
          >
            <div style={{ marginBottom: '10px', paddingBottom: '10px', borderBottom: '1px solid #E2E8F0' }}>
              <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Examination</span>
              <p style={{ margin: '2px 0 0 0', fontWeight: '700', fontSize: '14px' }}>{state.examTitle}</p>
            </div>

            <div style={{ marginBottom: '10px', paddingBottom: '10px', borderBottom: '1px solid #E2E8F0' }}>
              <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Centre Preferences</span>
              <ol style={{ margin: '4px 0 0 0', paddingLeft: '18px', fontSize: '12px', color: '#2D3748' }}>
                <li>{ALL_CENTRES_INDIA.find((c) => c.id === state.centre1Id)?.name} ({ALL_CENTRES_INDIA.find((c) => c.id === state.centre1Id)?.city})</li>
                <li>{ALL_CENTRES_INDIA.find((c) => c.id === state.centre2Id)?.name} ({ALL_CENTRES_INDIA.find((c) => c.id === state.centre2Id)?.city})</li>
                <li>{ALL_CENTRES_INDIA.find((c) => c.id === state.centre3Id)?.name} ({ALL_CENTRES_INDIA.find((c) => c.id === state.centre3Id)?.city})</li>
              </ol>
            </div>

            <div style={{ marginBottom: '10px' }}>
              <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Selected Shift</span>
              <p style={{ margin: '2px 0 0 0', fontSize: '13px', fontWeight: '600', color: '#2B6CB0' }}>
                {STANDARD_SHIFTS.find((s) => s.id === state.selectedShiftId)?.name}
              </p>
            </div>

            {state.pwdRequired && (
              <div style={{ fontSize: '12px', color: '#744210', backgroundColor: '#FEFCBF', padding: '6px 8px', borderRadius: '4px' }}>
                PwD Accommodations Requested: {state.pwdCategory} {state.scribeRequired ? '(With Scribe)' : ''}
              </div>
            )}
          </div>

          {/* Declaration Checkbox */}
          <div style={{ marginBottom: '18px', padding: '12px', backgroundColor: '#F7FAFC', borderRadius: '6px', border: '1px solid #E2E8F0' }}>
            <label style={{ display: 'flex', alignItems: 'flex-start', fontSize: '12px', color: '#2D3748', cursor: 'pointer' }}>
              <input
                data-testid="declaration-checkbox"
                type="checkbox"
                checked={state.instructionsAccepted}
                onChange={(e) => controller.setInstructionsAccepted(e.target.checked)}
                style={{ marginTop: '2px', marginRight: '8px' }}
              />
              <span>
                I hereby declare that all particulars furnished by me are true and complete. I understand that any false statement will result in immediate disqualification and legal liability under the Public Examinations (Prevention of Unfair Means) Act 2024.
              </span>
            </label>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <button
              data-testid="step3-back-btn"
              type="button"
              onClick={handleBack}
              style={{ padding: '10px 16px', background: 'none', border: '1px solid #CBD5E0', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' }}
            >
              ← Back
            </button>
            <button
              data-testid="submit-application-btn"
              type="button"
              disabled={state.isSubmitting || !state.instructionsAccepted}
              onClick={handleSubmit}
              style={{
                padding: '12px 24px',
                backgroundColor: state.instructionsAccepted ? '#22543D' : '#A0AEC0',
                color: '#FFFFFF',
                borderRadius: '6px',
                border: 'none',
                fontWeight: '700',
                fontSize: '14px',
                cursor: state.instructionsAccepted ? 'pointer' : 'not-allowed',
              }}
            >
              {state.isSubmitting ? 'Submitting Application...' : 'Submit & Issue Admit Card 🎟️'}
            </button>
          </div>
        </div>
      )}

      {/* SUCCESS MODAL / RECEIPT */}
      {state.currentStep === 'SUCCESS' && state.applicationReceipt && (
        <div
          data-testid="application-success-modal"
          style={{
            padding: '24px',
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            border: '2px solid #38A169',
            textAlign: 'center',
            boxShadow: '0 4px 12px rgba(56, 161, 105, 0.15)',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: '#C6F6D5',
              color: '#22543D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '24px',
              margin: '0 auto 12px auto',
            }}
          >
            ✓
          </div>
          <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#22543D', margin: '0 0 6px 0' }}>
            Application Submitted Successfully!
          </h3>
          <p style={{ fontSize: '13px', color: '#4A5568', margin: '0 0 16px 0' }}>
            Your application and tamper-evident digital Hall Ticket have been issued into your Offline Wallet.
          </p>

          <div
            style={{
              padding: '12px',
              backgroundColor: '#F7FAFC',
              borderRadius: '8px',
              marginBottom: '16px',
              textAlign: 'left',
              fontSize: '12px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ color: '#718096' }}>Application Number:</span>
              <strong data-testid="receipt-app-number" style={{ fontFamily: 'monospace' }}>
                {state.applicationReceipt.applicationNumber}
              </strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ color: '#718096' }}>Hall Ticket Number:</span>
              <strong data-testid="receipt-hall-ticket" style={{ fontFamily: 'monospace', color: '#2B6CB0' }}>
                {state.applicationReceipt.hallTicketNumber}
              </strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#718096' }}>Submission Timestamp:</span>
              <span>{new Date(state.applicationReceipt.submissionTimestamp).toLocaleTimeString()}</span>
            </div>
          </div>

          <button
            data-testid="view-in-wallet-btn"
            onClick={() => onApplicationCompleted && onApplicationCompleted(state.applicationReceipt!.hallTicketNumber)}
            style={{
              width: '100%',
              padding: '12px',
              backgroundColor: '#2B6CB0',
              color: '#FFFFFF',
              fontWeight: '600',
              fontSize: '14px',
              borderRadius: '6px',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            Open Hall Ticket in Offline Wallet
          </button>
        </div>
      )}
    </div>
  );
};
