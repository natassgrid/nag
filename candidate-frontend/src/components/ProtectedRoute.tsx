// src/components/ProtectedRoute.tsx
// Guards routes based on auth state and JWT validity.

import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { tokenManager } from '../utils/tokenManager';

interface Props {
  children: React.ReactNode;
  /** Set true for the /verify route — allows pending (not-yet-verified) users */
  requirePending?: boolean;
}

const ProtectedRoute: React.FC<Props> = ({ children, requirePending = false }) => {
  const { isAuthenticated, isVerified, pendingUserId } = useAuth();
  const location = useLocation();

  // requirePending: user must be in OTP flow (has pendingUserId but not yet verified)
  if (requirePending) {
    if (pendingUserId) return <>{children}</>;
    // If already verified, send to dashboard
    if (isAuthenticated && isVerified) return <Navigate to="/dashboard" replace />;
    return <Navigate to="/register" replace />;
  }

  // Standard protected route: must be authenticated + verified + token not expired
  if (!isAuthenticated || !isVerified || !tokenManager.isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
