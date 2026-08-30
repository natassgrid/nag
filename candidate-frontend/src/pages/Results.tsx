// src/pages/Results.tsx
// Connected results page → GET /api/v1/results/{candidateId}
// PDF download → GET /api/v1/results/{candidateId}/scorecard

import React, { useEffect, useState } from 'react';
import {
  Trophy, Download, CheckCircle, XCircle, BarChart3, Loader,
} from 'lucide-react';
import { resultService } from '../services/resultService';
import { examService } from '../services/examService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from '../components/Toast';
import type { ExamApplicationResponse, ResultDto } from '../types/api';

const Results: React.FC = () => {
  const { toast } = useToast();
  const userId = tokenManager.getUserId();

  const [myExams, setMyExams] = useState<ExamApplicationResponse[]>([]);
  const [selectedExamId, setSelectedExamId] = useState<string>('');
  const [result, setResult] = useState<ResultDto | null>(null);
  const [loadingExams, setLoadingExams] = useState(true);
  const [loadingResult, setLoadingResult] = useState(false);
  const [downloading, setDownloading] = useState(false);

  // Load candidate's completed exams
  useEffect(() => {
    if (!userId) return;
    examService.getMyExams()
      .then((apps) => {
        const confirmed = apps.filter((a) => a.status === 'CONFIRMED');
        setMyExams(confirmed);
        if (confirmed.length > 0) {
          setSelectedExamId(confirmed[0].examId);
        }
      })
      .catch(() => toast.error('Failed to load exams'))
      .finally(() => setLoadingExams(false));
  }, [userId, toast]);

  // Load result when selected exam changes
  useEffect(() => {
    if (!userId || !selectedExamId) return;
    setLoadingResult(true);
    setResult(null);
    resultService.getResult(userId, selectedExamId)
      .then(setResult)
      .catch(() => {
        // Result may not be published yet
        setResult(null);
      })
      .finally(() => setLoadingResult(false));
  }, [userId, selectedExamId]);

  const handleDownload = async () => {
    if (!userId) return;
    setDownloading(true);
    try {
      const blob = await resultService.downloadScorecard(userId);
      resultService.downloadScorecardFile(blob, userId);
      toast.success('Downloaded!', 'Your scorecard PDF has been saved.');
    } catch {
      toast.error('Download failed', 'Scorecard may not be available yet.');
    } finally {
      setDownloading(false);
    }
  };

  const percentage = result
    ? Math.round((result.rawScore / result.totalMarks) * 100)
    : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">My Results</h1>
        <p className="text-gray-500 text-sm mt-1">View your examination results and download scorecards.</p>
      </div>

      {/* Exam selector */}
      {loadingExams ? (
        <div className="h-10 bg-gray-200 rounded-lg animate-pulse w-48" />
      ) : myExams.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Trophy className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="font-medium text-gray-500">No results available yet</p>
          <p className="text-sm">Results will appear here after your exams are completed and published.</p>
        </div>
      ) : (
        <>
          <select
            value={selectedExamId}
            onChange={(e) => setSelectedExamId(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {myExams.map((app) => (
              <option key={app.examId} value={app.examId}>
                {app.examId}
              </option>
            ))}
          </select>

          {loadingResult ? (
            <div className="flex items-center justify-center py-16">
              <Loader className="w-8 h-8 animate-spin text-indigo-500" />
            </div>
          ) : !result ? (
            <div className="text-center py-16 text-gray-400 bg-white rounded-2xl border border-gray-100">
              <BarChart3 className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <p className="font-medium text-gray-500">Result not published yet</p>
              <p className="text-sm">Check back after the result declaration date.</p>
            </div>
          ) : (
            <>
              {/* Result Card */}
              <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-2xl p-6 text-white shadow-lg">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-bold mb-1">{result.examTitle}</h2>
                    <p className="text-indigo-200 text-sm">
                      {result.status === 'PUBLISHED' ? '✅ Result Published' : `Status: ${result.status}`}
                    </p>
                  </div>
                  <Trophy className="w-12 h-12 text-yellow-300 opacity-80" />
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6">
                  {[
                    { label: 'Score', value: `${result.rawScore}/${result.totalMarks}` },
                    { label: 'Percentage', value: `${percentage}%` },
                    { label: 'Percentile', value: result.percentile ? `${result.percentile.toFixed(2)}%ile` : 'N/A' },
                    { label: 'Rank', value: result.rank ? `#${result.rank}` : 'N/A' },
                  ].map((s) => (
                    <div key={s.label} className="bg-white/10 rounded-xl p-3 text-center">
                      <p className="text-xl font-bold">{s.value}</p>
                      <p className="text-xs text-indigo-200 mt-1">{s.label}</p>
                    </div>
                  ))}
                </div>

                <div className="mt-4 flex items-center gap-3">
                  <span className={`text-sm font-semibold px-3 py-1 rounded-full ${result.qualified ? 'bg-green-400/30 text-green-100' : 'bg-red-400/30 text-red-100'}`}>
                    {result.qualified ? '✓ QUALIFIED' : '✗ NOT QUALIFIED'}
                  </span>
                  <span className="text-xs text-indigo-200">
                    Qualifying score: {result.qualifyingScore} marks
                  </span>
                </div>
              </div>

              {/* Section-wise breakdown */}
              {result.sectionResults?.length > 0 && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                  <h3 className="font-semibold text-gray-800 mb-4">Section-wise Analysis</h3>
                  <div className="space-y-4">
                    {result.sectionResults.map((sec) => {
                      const pct = Math.round((sec.marks / sec.totalMarks) * 100);
                      return (
                        <div key={sec.sectionId}>
                          <div className="flex justify-between text-sm mb-1">
                            <span className="font-medium text-gray-700">{sec.sectionName}</span>
                            <span className="text-gray-500">{sec.marks}/{sec.totalMarks} ({pct}%)</span>
                          </div>
                          <div className="w-full bg-gray-100 rounded-full h-2">
                            <div
                              className="h-2 rounded-full bg-indigo-500 transition-all"
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                          <div className="flex gap-4 mt-1 text-xs text-gray-400">
                            <span className="flex items-center gap-1">
                              <CheckCircle className="w-3 h-3 text-green-500" /> {sec.correct} correct
                            </span>
                            <span className="flex items-center gap-1">
                              <XCircle className="w-3 h-3 text-red-400" /> {sec.incorrect} incorrect
                            </span>
                            <span>{sec.attempted} attempted</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Download */}
              {result.scorecardPdfRef && (
                <button
                  onClick={handleDownload}
                  disabled={downloading}
                  className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-6 py-3 rounded-xl font-medium transition"
                >
                  {downloading ? <Loader className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                  {downloading ? 'Downloading…' : 'Download Scorecard PDF'}
                </button>
              )}
            </>
          )}
        </>
      )}
    </div>
  );
};

export default Results;
