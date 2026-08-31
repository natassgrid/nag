// src/components/Layout.tsx
// App shell with sidebar, header, real-time SSE notification bell.
// Uses real profile from AuthContext instead of mock user object.

import React, { useState, useEffect, useRef } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { notificationService } from '../services/notificationService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from './Toast';
import type { NotificationDto } from '../types/api';
import {
  LayoutDashboard,
  User,
  BookOpen,
  Award,
  KeyRound,
  LogOut,
  Menu,
  X,
  CheckCircle,
  AlertCircle,
  Bell,
} from 'lucide-react';

const Layout: React.FC = () => {
  const { profile, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [recentNotifs, setRecentNotifs] = useState<NotificationDto[]>([]);
  const [bellOpen, setBellOpen] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Profile', href: '/profile', icon: User },
    { name: 'Browse Exams', href: '/exams', icon: BookOpen },
    { name: 'Results', href: '/results', icon: Award },
    { name: 'Change Password', href: '/password-management', icon: KeyRound },
  ];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const displayName = profile?.fullName || (profile?.firstName && profile?.lastName ? `${profile.firstName} ${profile.lastName}` : 'Candidate');
  const initials = profile?.fullName
    ? profile.fullName.trim().split(/\s+/).map((n) => n[0]).join('').substring(0, 2).toUpperCase()
    : (profile?.firstName ? profile.firstName[0].toUpperCase() : 'C');
  const completeness = profile?.completionPercentage ?? 0;

  // Load initial notifications
  useEffect(() => {
    if (!tokenManager.isAuthenticated()) return;
    notificationService.getNotifications(0, 10)
      .then((page) => {
        setRecentNotifs(page.content);
        setUnreadCount(page.content.filter((n) => !n.isRead).length);
      })
      .catch(() => {});
  }, []);

  // Open SSE stream for real-time notifications
  useEffect(() => {
    if (!tokenManager.isAuthenticated()) return;
    const es = notificationService.openStream((notification) => {
      setRecentNotifs((prev) => [notification, ...prev].slice(0, 10));
      if (!notification.isRead) {
        setUnreadCount((c) => c + 1);
        toast.info(notification.title, notification.body);
      }
    });
    eventSourceRef.current = es;
    return () => {
      es.close();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleMarkRead = async (id: string) => {
    try {
      await notificationService.markAsRead(id);
      setRecentNotifs((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)),
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // non-critical
    }
  };

  const SidebarContent = () => (
    <>
      {/* Logo */}
      <div className="flex items-center flex-shrink-0 px-4 py-5">
        <BookOpen className="w-7 h-7 text-indigo-400 mr-2" />
        <span className="text-lg font-extrabold tracking-wider bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">
          NAG Candidate
        </span>
      </div>

      {/* User brief */}
      <div className="px-4 py-3 mb-4 bg-slate-800/50 border-y border-slate-700/50 flex items-center gap-3">
        <div className="h-10 w-10 rounded-full bg-indigo-600 flex items-center justify-center font-bold text-white">
          {initials}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold truncate text-slate-100">{displayName}</p>
          <div className="flex items-center text-xs text-green-400 mt-0.5">
            <CheckCircle className="h-3 w-3 mr-1" /> Verified Candidate
          </div>
        </div>
      </div>

      {/* Profile completeness bar */}
      {completeness < 100 && (
        <div className="mx-4 mb-4 p-3 bg-amber-900/20 border border-amber-700/30 rounded-lg">
          <div className="flex justify-between text-xs text-amber-300 mb-1">
            <span>Profile</span>
            <span>{completeness}%</span>
          </div>
          <div className="w-full bg-slate-700 rounded-full h-1.5">
            <div
              className="h-1.5 rounded-full bg-amber-400"
              style={{ width: `${completeness}%` }}
            />
          </div>
        </div>
      )}

      {/* Nav items */}
      <nav className="flex-1 px-2 space-y-1 overflow-y-auto">
        {navigation.map((item) => {
          const isActive = location.pathname === item.href;
          return (
            <Link
              key={item.name}
              to={item.href}
              onClick={() => setSidebarOpen(false)}
              className={`group flex items-center px-3 py-2.5 text-sm font-medium rounded-lg transition-colors ${
                isActive
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <item.icon className="mr-3 h-5 w-5 flex-shrink-0" />
              {item.name}
            </Link>
          );
        })}
      </nav>

      {/* Sign out */}
      <div className="flex-shrink-0 border-t border-slate-800 p-4">
        <button
          onClick={handleLogout}
          className="group flex items-center w-full px-3 py-2.5 text-sm font-medium rounded-lg text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-colors"
        >
          <LogOut className="mr-3 h-5 w-5" />
          Sign Out
        </button>
      </div>
    </>
  );

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Desktop sidebar */}
      <aside className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0 bg-slate-900 text-white z-20">
        <div className="flex flex-col flex-grow overflow-y-auto">
          <SidebarContent />
        </div>
      </aside>

      {/* Mobile drawer */}
      {sidebarOpen && (
        <div className="md:hidden fixed inset-0 z-40 flex">
          <div
            className="fixed inset-0 bg-gray-600/75"
            onClick={() => setSidebarOpen(false)}
          />
          <div className="relative flex flex-col max-w-xs w-full bg-slate-900 text-white">
            <button
              className="absolute top-2 right-2 p-2 text-white"
              onClick={() => setSidebarOpen(false)}
              aria-label="Close sidebar"
            >
              <X className="h-6 w-6" />
            </button>
            <SidebarContent />
          </div>
        </div>
      )}

      {/* Main content */}
      <div className="flex flex-col flex-1 md:pl-64">
        {/* Top navbar */}
        <header className="sticky top-0 z-10 flex h-16 bg-white border-b border-gray-200 items-center justify-between px-4">
          <div className="flex items-center gap-3">
            <button
              className="md:hidden text-gray-500"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open sidebar"
            >
              <Menu className="h-6 w-6" />
            </button>
            <span className="text-sm font-semibold text-slate-800 uppercase tracking-wider">
              {navigation.find((n) => n.href === location.pathname)?.name ?? 'Candidate Space'}
            </span>
          </div>

          <div className="flex items-center gap-4">
            {/* Profile completeness badge */}
            {completeness < 85 && (
              <Link
                to="/profile"
                className="hidden sm:flex items-center px-3 py-1 bg-amber-50 text-amber-800 text-xs font-semibold rounded-full border border-amber-200"
              >
                <AlertCircle className="h-3.5 w-3.5 mr-1" />
                Complete Profile ({completeness}%)
              </Link>
            )}

            {/* Notification bell with SSE */}
            <div className="relative">
              <button
                onClick={() => setBellOpen((v) => !v)}
                className="p-1.5 rounded-full text-gray-400 hover:text-gray-600 focus:outline-none relative"
                aria-label={`Notifications — ${unreadCount} unread`}
              >
                <Bell className="h-5 w-5" />
                {unreadCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-xs w-4 h-4 rounded-full flex items-center justify-center font-bold">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {/* Dropdown */}
              {bellOpen && (
                <div className="absolute right-0 top-8 w-80 bg-white rounded-xl shadow-xl border border-gray-100 z-50">
                  <div className="p-3 border-b border-gray-100 flex justify-between items-center">
                    <span className="font-semibold text-sm text-gray-800">Notifications</span>
                    <button
                      onClick={() => setBellOpen(false)}
                      className="text-gray-400 hover:text-gray-600"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  <div className="max-h-80 overflow-y-auto">
                    {recentNotifs.length === 0 ? (
                      <p className="text-center text-sm text-gray-400 py-8">No notifications</p>
                    ) : (
                      recentNotifs.map((n) => (
                        <button
                          key={n.id}
                          onClick={() => void handleMarkRead(n.id)}
                          className={`w-full text-left p-3 border-b border-gray-50 hover:bg-gray-50 transition ${
                            n.isRead ? '' : 'bg-indigo-50/50'
                          }`}
                        >
                          <p className={`text-sm ${n.isRead ? 'text-gray-600' : 'font-semibold text-gray-800'}`}>
                            {n.title}
                          </p>
                          <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">{n.body}</p>
                        </button>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Avatar */}
            <div className="h-8 w-8 rounded-full bg-indigo-100 flex items-center justify-center font-bold text-indigo-700 border border-indigo-200 text-xs">
              {initials}
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 p-4 md:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
