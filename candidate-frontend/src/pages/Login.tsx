import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Mail, Lock, ArrowRight, ShieldCheck, AlertCircle, Info } from 'lucide-react';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [emailOrPhone, setEmailOrPhone] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Forgot password screen toggle
  const [showForgot, setShowForgot] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotSuccess, setForgotSuccess] = useState('');
  const [forgotError, setForgotError] = useState('');

  const from = location.state?.from?.pathname || '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!emailOrPhone || !password) {
      setError('Please fill in all fields');
      return;
    }
    
    setError('');
    setIsLoading(true);
    
    try {
      const success = await login(emailOrPhone, password);
      if (success) {
        navigate(from, { replace: true });
      } else {
        setError('Invalid credentials. Hint: use candidate@nag.gov.in and password123');
      }
    } catch (err) {
      setError('Failed to sign in. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleForgotSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forgotEmail) {
      setForgotError('Please enter your email or phone number');
      return;
    }
    setForgotError('');
    setIsLoading(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      setForgotSuccess(`A simulated password reset OTP has been sent to ${forgotEmail}.`);
    } catch (err) {
      setForgotError('Failed to send reset request.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background blobs for modern look */}
      <div className="absolute top-0 left-0 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-indigo-500 rounded-full blur-3xl opacity-20 pointer-events-none"></div>
      <div className="absolute bottom-0 right-0 translate-x-1/2 translate-y-1/2 w-96 h-96 bg-blue-500 rounded-full blur-3xl opacity-20 pointer-events-none"></div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md z-10">
        <div className="flex justify-center items-center space-x-2">
          <div className="h-10 w-10 bg-indigo-600 rounded-lg flex items-center justify-center text-white font-extrabold text-xl shadow-lg shadow-indigo-600/30">
            N
          </div>
          <span className="text-2xl font-extrabold tracking-wider text-white">
            NAG <span className="text-indigo-400 font-medium text-lg">Candidate Portal</span>
          </span>
        </div>
        <h2 className="mt-6 text-center text-3xl font-extrabold text-white">
          {showForgot ? 'Reset your password' : 'Sign in to your account'}
        </h2>
        <p className="mt-2 text-center text-sm text-slate-400">
          Or{' '}
          <Link to="/register" className="font-medium text-indigo-400 hover:text-indigo-300">
            register for a new candidate account
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md z-10">
        <div className="bg-slate-800 py-8 px-4 shadow-xl border border-slate-700/50 sm:rounded-lg sm:px-10">
          {!showForgot ? (
            /* Regular Login Form */
            <form className="space-y-6" onSubmit={handleSubmit}>
              {error && (
                <div className="rounded-md bg-red-950/30 border border-red-500/50 p-4">
                  <div className="flex">
                    <AlertCircle className="h-5 w-5 text-red-400" aria-hidden="true" />
                    <div className="ml-3">
                      <h3 className="text-sm font-medium text-red-300">{error}</h3>
                    </div>
                  </div>
                </div>
              )}

              <div>
                <label htmlFor="email" className="block text-sm font-medium text-slate-300">
                  Email Address / Mobile Number
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail className="h-5 w-5 text-slate-400" />
                  </div>
                  <input
                    id="email"
                    name="email"
                    type="text"
                    required
                    value={emailOrPhone}
                    onChange={(e) => setEmailOrPhone(e.target.value)}
                    placeholder="candidate@nag.gov.in"
                    className="block w-full pl-10 pr-3 py-2 bg-slate-900 border border-slate-700 rounded-md text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center">
                  <label htmlFor="password" className="block text-sm font-medium text-slate-300">
                    Password
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setShowForgot(true);
                      setError('');
                    }}
                    className="text-xs font-medium text-indigo-400 hover:text-indigo-300"
                  >
                    Forgot password?
                  </button>
                </div>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-slate-400" />
                  </div>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="block w-full pl-10 pr-3 py-2 bg-slate-900 border border-slate-700 rounded-md text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-md shadow-sm text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors disabled:opacity-50"
                >
                  {isLoading ? (
                    <div className="h-5 w-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  ) : (
                    <>
                      Sign In <ArrowRight className="ml-2 h-4 w-4" />
                    </>
                  )}
                </button>
              </div>

              {/* Developer Assist block */}
              <div className="mt-6 p-3 bg-indigo-950/20 border border-indigo-900/50 rounded-md">
                <div className="flex">
                  <Info className="h-5 w-5 text-indigo-400 flex-shrink-0" />
                  <div className="ml-2.5">
                    <h4 className="text-xs font-bold text-indigo-300">Prototype Demo Credentials:</h4>
                    <p className="text-xs text-indigo-200 mt-1">
                      Email: <span className="font-mono bg-slate-900 px-1 py-0.5 rounded text-white select-all">candidate@nag.gov.in</span><br/>
                      Password: <span className="font-mono bg-slate-900 px-1 py-0.5 rounded text-white select-all">password123</span>
                    </p>
                  </div>
                </div>
              </div>
            </form>
          ) : (
            /* Forgot Password Form */
            <form className="space-y-6" onSubmit={handleForgotSubmit}>
              {forgotSuccess && (
                <div className="rounded-md bg-green-950/30 border border-green-500/50 p-4">
                  <div className="flex">
                    <ShieldCheck className="h-5 w-5 text-green-400" />
                    <div className="ml-3">
                      <p className="text-sm font-medium text-green-300">{forgotSuccess}</p>
                      <button
                        type="button"
                        onClick={() => {
                          setShowForgot(false);
                          setForgotSuccess('');
                        }}
                        className="text-xs font-bold text-green-400 underline mt-2 block"
                      >
                        Back to Login
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {forgotError && (
                <div className="rounded-md bg-red-950/30 border border-red-500/50 p-4">
                  <div className="flex">
                    <AlertCircle className="h-5 w-5 text-red-400" />
                    <div className="ml-3">
                      <p className="text-sm font-medium text-red-300">{forgotError}</p>
                    </div>
                  </div>
                </div>
              )}

              {!forgotSuccess && (
                <>
                  <p className="text-slate-300 text-sm">
                    Enter the email address or mobile number associated with your account, and we will send you an OTP to reset your password.
                  </p>
                  <div>
                    <label htmlFor="forgot-email" className="block text-sm font-medium text-slate-300">
                      Email Address / Mobile Number
                    </label>
                    <div className="mt-1 relative rounded-md shadow-sm">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Mail className="h-5 w-5 text-slate-400" />
                      </div>
                      <input
                        id="forgot-email"
                        type="text"
                        required
                        value={forgotEmail}
                        onChange={(e) => setForgotEmail(e.target.value)}
                        placeholder="Enter your registered email or phone"
                        className="block w-full pl-10 pr-3 py-2 bg-slate-900 border border-slate-700 rounded-md text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                      />
                    </div>
                  </div>

                  <div>
                    <button
                      type="submit"
                      disabled={isLoading}
                      className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-md shadow-sm text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors disabled:opacity-50"
                    >
                      {isLoading ? (
                        <div className="h-5 w-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      ) : (
                        'Send OTP'
                      )}
                    </button>
                  </div>
                </>
              )}

              <div className="text-center mt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowForgot(false);
                    setError('');
                    setForgotError('');
                    setForgotSuccess('');
                  }}
                  className="text-sm font-semibold text-slate-400 hover:text-slate-300"
                >
                  Cancel and go back
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
export default Login;
