// src/App.tsx
// Root router with ErrorBoundary + ToastProvider wrapping all routes.

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './components/Toast';
import { ErrorBoundary } from './components/ErrorBoundary';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import Login from './pages/Login';
import Register from './pages/Register';
import VerifyOtp from './pages/VerifyOtp';
import Dashboard from './pages/Dashboard';
import Profile from './pages/Profile';
import PasswordMgmt from './pages/PasswordMgmt';
import BrowseExams from './pages/BrowseExams';
import TakeExam from './pages/TakeExam';
import Results from './pages/Results';

const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              {/* OTP verification — requires pending registration */}
              <Route
                path="/verify"
                element={
                  <ProtectedRoute requirePending>
                    <VerifyOtp />
                  </ProtectedRoute>
                }
              />

              {/* Full-screen exam — no sidebar layout (supports /take-exam/:examId and /take-exam/:examId/:shiftId) */}
              <Route
                path="/take-exam/:examId"
                element={
                  <ProtectedRoute>
                    <ErrorBoundary>
                      <TakeExam />
                    </ErrorBoundary>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/take-exam/:examId/:shiftId"
                element={
                  <ProtectedRoute>
                    <ErrorBoundary>
                      <TakeExam />
                    </ErrorBoundary>
                  </ProtectedRoute>
                }
              />

              {/* Authenticated routes with sidebar layout */}
              <Route
                element={
                  <ProtectedRoute>
                    <Layout />
                  </ProtectedRoute>
                }
              >
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/password-management" element={<PasswordMgmt />} />
                <Route path="/exams" element={<BrowseExams />} />
                <Route path="/results" element={<Results />} />
              </Route>

              {/* Catch-all */}
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </ErrorBoundary>
  );
};

export default App;
