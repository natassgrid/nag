import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireVerification?: boolean;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requireVerification = true 
}) => {
  const { isAuthenticated, isVerified } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // Redirect to login page
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requireVerification && !isVerified) {
    // Redirect to OTP verification page
    return <Navigate to="/verify" replace />;
  }

  if (location.pathname === '/verify' && isVerified) {
    // If they are verified and try to visit verification page, take them to dashboard
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};
export default ProtectedRoute;
