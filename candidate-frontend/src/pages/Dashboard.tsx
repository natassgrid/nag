// src/pages/Dashboard.tsx
// Connected dashboard — loads exams from examService, results from resultService,
// notifications from notificationService, profile from AuthContext.

import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen, CheckCircle, Clock, Bell, ChevronRight,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { examService } from '../services/examService';
import { notificationService } from '../services/notificationService';
import type { ExamApplicationResponse, NotificationDto } from '../types/api';

const Dashboard: React.FC = () => {
  const { profile, profileLoading } = useAuth();
  const [myExams, setMyExams] = useState<ExamApplicationResponse[]>([]);
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [examsLoading, setExamsLoading] = useState(true);
  const [notifsLoading, setNotifsLoading] = useState(true);

  useEffect(() => {
    examService.getMyExams()
      .then(setMyExams)
      .catch(() => setMyExams([]))
      .finally(() => setExamsLoading(false));

    notificationService.getNotifications(0, 5)
      .then((page) => setNotifications(page.content))
      .catch(() => setNotifications([]))
      .finally(() => setNotifsLoading(false));
  }, []);

  const displayName = profile
    ? `${profile.firstName} ${profile.lastName}`
    : 'Candidate';

  const completeness = profile?.completionPercentage ?? 0;

  const appliedExams = myExams.filter((e) => e.status === 'APPLIED' || e.status === 'CONFIRMED');

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-2xl p-6 text-white shadow-lg">
        <h1 className="text-2xl font-bold mb-1">
          {profileLoading ? 'Welcome back!' : `Welcome, ${displayName}!`}
        </h1>
        <p className="text-indigo-200 text-sm">
          {new Date().toLocaleDateString('en-IN', {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
          })}
        </p>

        {/* Profile completeness */}
        {completeness < 100 && (
          <div className="mt-4 bg-white/10 rounded-xl p-4">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium">Profile Completion</span>
              <span className="text-sm font-bold">{completeness}%</span>
            </div>
            <div className="w-full bg-white/20 rounded-full h-2">
              <div
                className="bg-white h-2 rounded-full transition-all duration-500"
                style={{ width: `${completeness}%` }}
              />
            </div>
            <p className="text-xs text-indigo-200 mt-2">
              Complete your profile to apply for exams.{' '}
              <Link to="/profile" className="underline text-white font-medium">
                Complete now →
              </Link>
            </p>
          </div>
        )}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        {[
          { label: 'Registered Exams', value: appliedExams.length, icon: BookOpen, color: 'text-indigo-600 bg-indigo-100' },
          { label: 'Profile Complete', value: `${completeness}%`, icon: CheckCircle, color: 'text-green-600 bg-green-100' },
          { label: 'Notifications', value: notifications.filter((n) => !n.isRead).length, icon: Bell, color: 'text-amber-600 bg-amber-100' },
        ].map((stat) => (
          <div key={stat.label} className="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
            <div className={`inline-flex p-2 rounded-lg ${stat.color} mb-2`}>
              <stat.icon className="w-5 h-5" />
            </div>
            <p className="text-2xl font-bold text-gray-800">{stat.value}</p>
            <p className="text-xs text-gray-500 mt-0.5">{stat.label}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Registered Exams */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between p-5 border-b border-gray-100">
            <h2 className="font-semibold text-gray-800 flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-500" /> Registered Exams
            </h2>
            <Link to="/exams" className="text-xs text-indigo-600 hover:underline flex items-center gap-1">
              Browse all <ChevronRight className="w-3 h-3" />
            </Link>
          </div>
          <div className="p-5">
            {examsLoading ? (
              <div className="space-y-3">
                {[1, 2].map((i) => (
                  <div key={i} className="h-14 bg-gray-100 rounded-lg animate-pulse" />
                ))}
              </div>
            ) : appliedExams.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <BookOpen className="w-10 h-10 mx-auto mb-2 text-gray-300" />
                <p className="text-sm">No exams registered yet.</p>
                <Link to="/exams" className="text-indigo-600 text-sm hover:underline mt-1 inline-block">
                  Browse available exams →
                </Link>
              </div>
            ) : (
              <div className="space-y-3">
                {appliedExams.map((app) => (
                  <div key={app.applicationId} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div>
                      <p className="text-sm font-medium text-gray-800 truncate max-w-[180px]">
                        {app.applicationId}
                      </p>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        app.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                      }`}>
                        {app.status}
                      </span>
                    </div>
                    {app.hallTicketNumber && (
                      <p className="text-xs text-gray-500">Hall Ticket: {app.hallTicketNumber}</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Notifications */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between p-5 border-b border-gray-100">
            <h2 className="font-semibold text-gray-800 flex items-center gap-2">
              <Bell className="w-4 h-4 text-amber-500" /> Notifications
            </h2>
          </div>
          <div className="p-5">
            {notifsLoading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-12 bg-gray-100 rounded-lg animate-pulse" />
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <Bell className="w-10 h-10 mx-auto mb-2 text-gray-300" />
                <p className="text-sm">No notifications yet.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {notifications.map((n) => (
                  <div key={n.id} className={`p-3 rounded-lg ${n.isRead ? 'bg-gray-50' : 'bg-indigo-50 border border-indigo-100'}`}>
                    <p className="text-sm font-medium text-gray-800">{n.title}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{n.body}</p>
                    <p className="text-xs text-gray-400 mt-1">
                      {new Date(n.createdAt).toLocaleDateString('en-IN')}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Links */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
        <h2 className="font-semibold text-gray-800 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { to: '/profile', icon: '👤', label: 'Complete Profile' },
            { to: '/exams', icon: '📋', label: 'Browse Exams' },
            { to: '/results', icon: '🏆', label: 'View Results' },
            { to: '/password-management', icon: '🔒', label: 'Change Password' },
          ].map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className="flex flex-col items-center gap-2 p-4 rounded-xl border border-gray-200 hover:border-indigo-300 hover:bg-indigo-50 transition text-center"
            >
              <span className="text-2xl">{link.icon}</span>
              <span className="text-xs font-medium text-gray-700">{link.label}</span>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
