import React, { useState, useEffect } from 'react';
import {
  CatalogController,
  CatalogState,
  CatalogExam,
  ExamCategory,
  ExamFilterStatus,
  CatalogSortOption,
} from './catalog-controller';

export interface ExamCatalogScreenProps {
  controller: CatalogController;
  onApplyExam?: (examId: string) => void;
  onPracticeExam?: (examId: string) => void;
  onNavigateToWallet?: () => void;
}

const CATEGORIES: ExamCategory[] = ['ALL', 'UPSC', 'SSC', 'NTA', 'STATE_PSC', 'BANKING'];

export const ExamCatalogScreen: React.FC<ExamCatalogScreenProps> = ({
  controller,
  onApplyExam,
  onPracticeExam,
  onNavigateToWallet,
}) => {
  const [state, setState] = useState<CatalogState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s: CatalogState) => {
      setState(s);
    });
  }, [controller]);

  const filteredExams = controller.getFilteredExams();

  return (
    <div
      data-testid="exam-catalog-screen"
      style={{
        maxWidth: '560px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '16px',
        }}
      >
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A365D', margin: 0 }}>
            Examination Catalog
          </h2>
          <p style={{ fontSize: '12px', color: '#718096', margin: '2px 0 0 0' }}>
            Discover and apply for Central & State Public Service Exams
          </p>
        </div>

        {onNavigateToWallet && (
          <button
            data-testid="catalog-to-wallet-btn"
            onClick={onNavigateToWallet}
            style={{
              padding: '8px 12px',
              backgroundColor: '#EDF2F7',
              border: '1px solid #CBD5E0',
              borderRadius: '6px',
              fontSize: '12px',
              fontWeight: '600',
              color: '#2B6CB0',
              cursor: 'pointer',
            }}
          >
            Admit Cards 🎟️
          </button>
        )}
      </div>

      {/* Real-time Search Input */}
      <div style={{ marginBottom: '12px' }}>
        <input
          data-testid="catalog-search-input"
          type="search"
          placeholder="Search by exam name, code, or conducting agency..."
          value={state.searchQuery}
          onChange={(e) => controller.setSearchQuery(e.target.value)}
          style={{
            width: '100%',
            padding: '12px 14px',
            border: '1px solid #CBD5E0',
            borderRadius: '8px',
            fontSize: '14px',
            outline: 'none',
            boxSizing: 'border-box',
          }}
        />
      </div>

      {/* Category Filter Pills */}
      <div
        data-testid="category-filter-pills"
        style={{
          display: 'flex',
          gap: '8px',
          overflowX: 'auto',
          paddingBottom: '6px',
          marginBottom: '12px',
        }}
      >
        {CATEGORIES.map((cat) => {
          const isSelected = state.selectedCategory === cat;
          return (
            <button
              key={cat}
              data-testid={`cat-pill-${cat}`}
              onClick={() => controller.setCategory(cat)}
              style={{
                padding: '6px 14px',
                borderRadius: '16px',
                border: isSelected ? '1px solid #2B6CB0' : '1px solid #E2E8F0',
                backgroundColor: isSelected ? '#2B6CB0' : '#FFFFFF',
                color: isSelected ? '#FFFFFF' : '#4A5568',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {cat === 'ALL' ? 'All Bodies' : cat}
            </button>
          );
        })}
      </div>

      {/* Filter & Sort Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '10px',
          padding: '10px 12px',
          backgroundColor: '#F7FAFC',
          borderRadius: '8px',
          marginBottom: '16px',
          border: '1px solid #E2E8F0',
        }}
      >
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <select
            data-testid="catalog-status-select"
            value={state.selectedStatus}
            onChange={(e) => controller.setStatus(e.target.value as ExamFilterStatus)}
            style={{
              padding: '6px 8px',
              fontSize: '12px',
              borderRadius: '6px',
              border: '1px solid #CBD5E0',
              backgroundColor: '#FFFFFF',
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="OPEN">Applications Open</option>
            <option value="CLOSING_SOON">Closing Soon (≤7 Days)</option>
            <option value="UPCOMING">Upcoming</option>
          </select>

          <select
            data-testid="catalog-sort-select"
            value={state.selectedSort}
            onChange={(e) => controller.setSort(e.target.value as CatalogSortOption)}
            style={{
              padding: '6px 8px',
              fontSize: '12px',
              borderRadius: '6px',
              border: '1px solid #CBD5E0',
              backgroundColor: '#FFFFFF',
            }}
          >
            <option value="DATE_ASC">Exam Date (Earliest)</option>
            <option value="FEE_ASC">Fee (Low to High)</option>
            <option value="FEE_DESC">Fee (High to Low)</option>
            <option value="TITLE_ASC">Title (A-Z)</option>
          </select>
        </div>

        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '12px',
            color: '#2D3748',
            fontWeight: '600',
            cursor: 'pointer',
          }}
        >
          <input
            data-testid="only-eligible-checkbox"
            type="checkbox"
            checked={state.onlyEligible}
            onChange={(e) => controller.setOnlyEligible(e.target.checked)}
          />
          Eligible Only
        </label>
      </div>

      {/* Candidate Eligibility Context Strip */}
      {state.eligibilityProfile && (
        <div
          data-testid="eligibility-profile-strip"
          style={{
            padding: '8px 12px',
            backgroundColor: '#EBF8FF',
            borderRadius: '6px',
            fontSize: '12px',
            color: '#2B6CB0',
            marginBottom: '16px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <span>
            Profile: <strong>{state.eligibilityProfile.age} yrs</strong> | <strong>{state.eligibilityProfile.qualification}</strong> | Category: <strong>{state.eligibilityProfile.category}</strong>
          </span>
          <span style={{ fontSize: '11px', fontWeight: '700' }}>
            {state.eligibilityProfile.category === 'SC' || state.eligibilityProfile.category === 'ST'
              ? 'Fee Waiver: 100%'
              : 'Standard Fees'}
          </span>
        </div>
      )}

      {/* Exams Results List */}
      <div data-testid="exam-card-list" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {filteredExams.length === 0 ? (
          <div
            data-testid="no-exams-found"
            style={{
              padding: '36px',
              textAlign: 'center',
              backgroundColor: '#F7FAFC',
              borderRadius: '8px',
              border: '1px dashed #CBD5E0',
            }}
          >
            <p style={{ margin: 0, color: '#718096', fontSize: '14px' }}>
              No examinations matching the selected filters.
            </p>
          </div>
        ) : (
          filteredExams.map((exam: CatalogExam) => {
            const eligibility = controller.checkEligibility(exam);
            const isOpen = exam.status === 'REGISTRATION_OPEN';

            return (
              <div
                key={exam.id}
                data-testid={`exam-card-${exam.id}`}
                style={{
                  padding: '16px',
                  backgroundColor: '#FFFFFF',
                  borderRadius: '10px',
                  border: '1px solid #E2E8F0',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.04)',
                }}
              >
                {/* Agency & Status Row */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: '700',
                      color: '#2B6CB0',
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px',
                    }}
                  >
                    {exam.conductingBody}
                  </span>

                  <span
                    data-testid={`exam-status-${exam.id}`}
                    style={{
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: '700',
                      backgroundColor:
                        exam.status === 'REGISTRATION_OPEN'
                          ? '#C6F6D5'
                          : exam.status === 'UPCOMING'
                          ? '#FEFCBF'
                          : '#FED7D7',
                      color:
                        exam.status === 'REGISTRATION_OPEN'
                          ? '#22543D'
                          : exam.status === 'UPCOMING'
                          ? '#744210'
                          : '#9B2C2C',
                    }}
                  >
                    {exam.status === 'REGISTRATION_OPEN'
                      ? 'REGISTRATION OPEN'
                      : exam.status === 'UPCOMING'
                      ? 'UPCOMING'
                      : 'CLOSED'}
                  </span>
                </div>

                {/* Title */}
                <h3 style={{ margin: '0 0 6px 0', fontSize: '15px', fontWeight: '700', color: '#1A202C' }}>
                  {exam.title}
                </h3>
                <p style={{ margin: '0 0 12px 0', fontSize: '12px', color: '#4A5568', lineHeight: 1.4 }}>
                  {exam.description}
                </p>

                {/* Key Particulars Grid */}
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(3, 1fr)',
                    gap: '8px',
                    padding: '10px',
                    backgroundColor: '#F7FAFC',
                    borderRadius: '6px',
                    marginBottom: '12px',
                    fontSize: '12px',
                  }}
                >
                  <div>
                    <span style={{ color: '#718096', fontSize: '10px', textTransform: 'uppercase', display: 'block' }}>
                      Exam Date
                    </span>
                    <strong style={{ color: '#2D3748' }}>{exam.examDate}</strong>
                  </div>
                  <div>
                    <span style={{ color: '#718096', fontSize: '10px', textTransform: 'uppercase', display: 'block' }}>
                      Apply Before
                    </span>
                    <strong style={{ color: '#C53030' }}>{exam.registrationEndDate}</strong>
                  </div>
                  <div>
                    <span style={{ color: '#718096', fontSize: '10px', textTransform: 'uppercase', display: 'block' }}>
                      Application Fee
                    </span>
                    <strong style={{ color: '#2B6CB0' }}>
                      {eligibility.feeWaiverApplicable ? '₹0 (Fee Waiver)' : `₹${exam.feeAmount}`}
                    </strong>
                  </div>
                </div>

                {/* Eligibility Status Pill */}
                <div
                  data-testid={`eligibility-badge-${exam.id}`}
                  style={{
                    padding: '6px 10px',
                    borderRadius: '6px',
                    fontSize: '11px',
                    fontWeight: '600',
                    backgroundColor: eligibility.eligible ? '#F0FFF4' : '#FFF5F5',
                    color: eligibility.eligible ? '#22543D' : '#9B2C2C',
                    border: eligibility.eligible ? '1px solid #9AE6B4' : '1px solid #FEB2B2',
                    marginBottom: '14px',
                  }}
                >
                  {eligibility.eligible ? '✓ You meet all eligibility criteria' : `✕ Ineligible: ${eligibility.reasons[0]}`}
                </div>

                {/* Action Buttons */}
                <div style={{ display: 'flex', gap: '10px' }}>
                  {onApplyExam && (
                    <button
                      data-testid={`apply-exam-btn-${exam.id}`}
                      disabled={!isOpen}
                      onClick={() => onApplyExam(exam.id)}
                      style={{
                        flex: 1,
                        padding: '10px',
                        backgroundColor: isOpen ? '#2B6CB0' : '#E2E8F0',
                        color: isOpen ? '#FFFFFF' : '#A0AEC0',
                        fontWeight: '600',
                        fontSize: '13px',
                        borderRadius: '6px',
                        border: 'none',
                        cursor: isOpen ? 'pointer' : 'not-allowed',
                      }}
                    >
                      Apply Now
                    </button>
                  )}

                  {onPracticeExam && (
                    <button
                      data-testid={`practice-cbt-btn-${exam.id}`}
                      onClick={() => onPracticeExam(exam.id)}
                      style={{
                        flex: 1,
                        padding: '10px',
                        backgroundColor: '#EDF2F7',
                        color: '#2B6CB0',
                        border: '1px solid #CBD5E0',
                        fontWeight: '600',
                        fontSize: '13px',
                        borderRadius: '6px',
                        cursor: 'pointer',
                      }}
                    >
                      Practice Mock CBT
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
