// src/pages/Dashboard.tsx
// Candidate dashboard with registered examinations, instant Admit Card view, CBT exam launcher, and quick actions.

import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  CheckCircle,
  Clock,
  Bell,
  ChevronRight,
  FileCheck2,
  PlayCircle,
  MapPin,
  Calendar,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { examService } from '../services/examService';
import { notificationService } from '../services/notificationService';
import { CandidateAvatar } from '../components/CandidateAvatar';
import { AdmitCardModal } from '../components/AdmitCardModal';
import type { ExamApplicationResponse, NotificationDto } from '../types/api';

const Dashboard: React.FC = () => {
  const { profile, profileLoading } = useAuth();
  const [myExams, setMyExams] = useState<ExamApplicationResponse[]>([]);
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [examsLoading, setExamsLoading] = useState(true);
  const [notifsLoading, setNotifsLoading] = useState(true);

  // Admit Card Modal State
  const [selectedAdmitCardExamId, setSelectedAdmitCardExamId] = useState<string | null>(null);

  useEffect(() => {
    examService
      .getMyExams()
      .then(setMyExams)
      .catch(() => setMyExams([]))
      .finally(() => setExamsLoading(false));

    notificationService
      .getNotifications(0, 5)
      .then((page) => setNotifications(page.content))
      .catch(() => setNotifications([]))
      .finally(() => setNotifsLoading(false));
  }, []);

  const displayName = profile
    ? profile.fullName || `${profile.firstName || ''} ${profile.lastName || ''}`.trim() || 'Candidate'
    : 'Candidate';

  const completeness = profile?.completionPercentage ?? 0;
  const appliedExams = myExams.filter((e) => e.status === 'APPLIED' || e.status === 'CONFIRMED');

  const formatDate = (d?: string) => {
    if (!d) return 'Schedule TBA';
    return new Date(d).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  };

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-teal-700 via-teal-800 to-indigo-900 rounded-2xl p-6 text-white shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <CandidateAvatar
            photoAssetId={profile?.photoAssetId}
            name={displayName}
            size="lg"
            bordered={true}
          />
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-teal-200">
              Candidate Portal
            </span>
            <h1 className="text-2xl font-bold">
              {profileLoading ? 'Welcome back!' : `Welcome, ${displayName}!`}
            </h1>
            <p className="text-teal-100 text-xs mt-0.5">
              {new Date().toLocaleDateString('en-IN', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            </p>
          </div>
        </div>

        {/* Profile completeness */}
        {completeness < 100 && (
          <div className="mt-2 bg-white/10 rounded-xl p-3.5 backdrop-blur-sm border border-white/10 sm:max-w-xs w-full">
            <div className="flex justify-between items-center mb-1.5">
              <span className="text-xs font-medium">Profile Completion</span>
              <span className="text-xs font-bold">{completeness}%</span>
            </div>
            <div className="w-full bg-white/20 rounded-full h-1.5">
              <div
                className="bg-emerald-400 h-1.5 rounded-full transition-all duration-500"
                style={{ width: `${completeness}%` }}
              />
            </div>
            <p className="text-[11px] text-teal-100 mt-2">
              Complete details to streamline exam registration.{' '}
              <Link to="/profile" className="underline font-semibold text-white">
                Update →
              </Link>
            </p>
          </div>
        )}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        {[
          {
            label: 'Registered Examinations',
            value: appliedExams.length,
            icon: BookOpen,
            color: 'text-teal-700 bg-teal-50 dark:bg-teal-950/40 dark:text-teal-300',
          },
          {
            label: 'Profile Complete',
            value: `${completeness}%`,
            icon: CheckCircle,
            color: 'text-emerald-700 bg-emerald-50 dark:bg-emerald-950/40 dark:text-emerald-300',
          },
          {
            label: 'Unread Notifications',
            value: notifications.filter((n) => !n.isRead).length,
            icon: Bell,
            color: 'text-amber-700 bg-amber-50 dark:bg-amber-950/40 dark:text-amber-300',
          },
        ].map((stat) => (
          <div
            key={stat.label}
            className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:bg-slate-800 dark:border-slate-700"
          >
            <div className={`inline-flex p-2 rounded-lg ${stat.color} mb-2`}>
              <stat.icon className="w-5 h-5" />
            </div>
            <p className="text-2xl font-bold text-slate-800 dark:text-white">{stat.value}</p>
            <p className="text-xs text-slate-500 mt-0.5 dark:text-slate-400">{stat.label}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Registered Exams */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:bg-slate-800 dark:border-slate-700 flex flex-col">
          <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-700">
            <h2 className="font-bold text-slate-800 dark:text-white flex items-center gap-2 text-sm">
              <Clock className="w-4 h-4 text-teal-600" /> My Applied Examinations
            </h2>
            <Link
              to="/exams"
              className="text-xs font-semibold text-teal-700 hover:text-teal-800 flex items-center gap-1 dark:text-teal-400"
            >
              Browse all <ChevronRight className="w-3 h-3" />
            </Link>
          </div>
          <div className="p-5 flex-1">
            {examsLoading ? (
              <div className="space-y-3">
                {[1, 2].map((i) => (
                  <div
                    key={i}
                    className="h-20 bg-slate-100 rounded-xl animate-pulse dark:bg-slate-700"
                  />
                ))}
              </div>
            ) : appliedExams.length === 0 ? (
              <div className="text-center py-10 text-slate-400">
                <BookOpen className="w-10 h-10 mx-auto mb-2 text-slate-300 dark:text-slate-600" />
                <p className="text-sm font-medium text-slate-600 dark:text-slate-300">
                  No examinations applied yet.
                </p>
                <Link
                  to="/exams"
                  className="text-teal-700 text-xs font-semibold hover:underline mt-2 inline-block dark:text-teal-400"
                >
                  Explore upcoming examinations →
                </Link>
              </div>
            ) : (
              <div className="space-y-3.5">
                {appliedExams.map((app) => (
                  <div
                    key={app.applicationId}
                    className="rounded-xl border border-slate-200 bg-slate-50/60 p-4 transition dark:bg-slate-800/60 dark:border-slate-700 hover:border-slate-300"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <span className="rounded bg-teal-100/80 px-2 py-0.5 text-[10px] font-bold text-teal-800 dark:bg-teal-950 dark:text-teal-300">
                          {app.status}
                        </span>
                        <h3 className="mt-1 text-sm font-bold text-slate-900 dark:text-white line-clamp-1">
                          {app.examName || `Exam ID: ${app.examId.substring(0, 8)}...`}
                        </h3>
                      </div>
                      {app.hallTicketNumber && (
                        <div className="text-right shrink-0">
                          <span className="text-[10px] uppercase font-semibold text-slate-500 dark:text-slate-400">
                            Roll No
                          </span>
                          <p className="font-mono text-xs font-bold text-teal-800 dark:text-teal-300">
                            {app.hallTicketNumber}
                          </p>
                        </div>
                      )}
                    </div>

                    <div className="mt-2.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
                      {app.centreName && (
                        <span className="flex items-center gap-1">
                          <MapPin className="h-3 w-3 text-slate-400" />
                          {app.city || app.centreName}
                        </span>
                      )}
                      {app.examDate && (
                        <span className="flex items-center gap-1">
                          <Calendar className="h-3 w-3 text-slate-400" />
                          {formatDate(app.examDate)}
                        </span>
                      )}
                    </div>

                    {/* CTAs */}
                    <div className="mt-3.5 flex items-center gap-2 pt-2 border-t border-slate-200/60 dark:border-slate-700/60">
                      <button
                        onClick={() => setSelectedAdmitCardExamId(app.examId)}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-teal-300 bg-teal-50 px-3 py-1.5 text-xs font-bold text-teal-800 hover:bg-teal-100 dark:bg-teal-950/40 dark:border-teal-800 dark:text-teal-200 transition"
                      >
                        <FileCheck2 className="h-3.5 w-3.5" />
                        <span>Admit Card</span>
                      </button>
                      <Link
                        to={`/take-exam/${app.examId}`}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-teal-700 px-3.5 py-1.5 text-xs font-bold text-white hover:bg-teal-800 transition"
                      >
                        <PlayCircle className="h-3.5 w-3.5" />
                        <span>Take CBT Exam</span>
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Notifications */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:bg-slate-800 dark:border-slate-700 flex flex-col">
          <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-700">
            <h2 className="font-bold text-slate-800 dark:text-white flex items-center gap-2 text-sm">
              <Bell className="w-4 h-4 text-amber-500" /> Announcements & Alerts
            </h2>
          </div>
          <div className="p-5 flex-1">
            {notifsLoading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="h-12 bg-slate-100 rounded-lg animate-pulse dark:bg-slate-700"
                  />
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <div className="text-center py-10 text-slate-400">
                <Bell className="w-10 h-10 mx-auto mb-2 text-slate-300 dark:text-slate-600" />
                <p className="text-sm">No new notifications at this time.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {notifications.map((n) => (
                  <div
                    key={n.id}
                    className={`rounded-xl p-3.5 text-xs transition ${
                      n.isRead
                        ? 'bg-slate-50 dark:bg-slate-800'
                        : 'bg-teal-50/50 border border-teal-200 dark:bg-teal-950/20 dark:border-teal-900'
                    }`}
                  >
                    <p className="font-bold text-slate-900 dark:text-white">{n.title}</p>
                    <p className="text-slate-600 mt-0.5 dark:text-slate-300 leading-relaxed">
                      {n.body}
                    </p>
                    <p className="text-[10px] text-slate-400 mt-1.5">
                      {new Date(n.createdAt).toLocaleDateString('en-IN')}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:bg-slate-800 dark:border-slate-700">
        <h2 className="font-bold text-slate-800 dark:text-white mb-4 text-sm">Quick Actions</h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { to: '/exams', icon: '📝', label: 'Browse & Apply Exams' },
            { to: '/profile', icon: '👤', label: 'Candidate Profile' },
            { to: '/results', icon: '🏆', label: 'Scorecards & Results' },
            { to: '/password-management', icon: '🔒', label: 'Security & Password' },
          ].map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className="flex flex-col items-center gap-2 p-4 rounded-xl border border-slate-200 hover:border-teal-400 hover:bg-teal-50/30 transition text-center dark:border-slate-700 dark:hover:bg-slate-700/50"
            >
              <span className="text-2xl">{link.icon}</span>
              <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                {link.label}
              </span>
            </Link>
          ))}
        </div>
      </div>

      {/* Admit Card Modal */}
      <AdmitCardModal
        examId={selectedAdmitCardExamId}
        isOpen={!!selectedAdmitCardExamId}
        onClose={() => setSelectedAdmitCardExamId(null)}
      />
    </div>
  );
};

export default Dashboard;
