import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Mail, Phone, AlertCircle, Info } from 'lucide-react';

export const VerifyOtp: React.FC = () => {
  const { user, verifyOtp, resendOtp, otpSentTo } = useAuth();
  const navigate = useNavigate();

  const [otp, setOtp] = useState('');
  const [timer, setTimer] = useState(59);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Get masked values
  const email = otpSentTo?.email || user?.email || 'candidate@nag.gov.in';
  const mobile = otpSentTo?.mobile || user?.mobile || '9876543210';

  const maskEmail = (emailStr: string) => {
    const [name, domain] = emailStr.split('@');
    if (!name || !domain) return emailStr;
    return `${name.charAt(0)}***${name.charAt(name.length - 1)}@${domain}`;
  };

  const maskPhone = (phoneStr: string) => {
    if (phoneStr.length < 4) return phoneStr;
    return `******${phoneStr.slice(-4)}`;
  };

  useEffect(() => {
    if (timer > 0) {
      const interval = setInterval(() => setTimer((t) => t - 1), 1000);
      return () => clearInterval(interval);
    }
  }, [timer]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 6) {
      setError('OTP must be a 6-digit code');
      return;
    }

    setError('');
    setIsLoading(true);

    try {
      const isOk = await verifyOtp(otp);
      if (isOk) {
        navigate('/dashboard');
      } else {
        setError('Invalid OTP code. Use "123456" for mock verification');
      }
    } catch (err) {
      setError('Verification failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (timer > 0) return;
    setError('');
    setSuccess('');
    setIsLoading(true);
    try {
      await resendOtp();
      setTimer(59);
      setSuccess('Verification codes resent successfully.');
    } catch (err) {
      setError('Failed to resend OTP.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute top-0 left-0 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-indigo-500 rounded-full blur-3xl opacity-20 pointer-events-none"></div>
      <div className="absolute bottom-0 right-0 translate-x-1/2 translate-y-1/2 w-96 h-96 bg-indigo-600 rounded-full blur-3xl opacity-15 pointer-events-none"></div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md z-10">
        <div className="flex justify-center items-center space-x-2">
          <div className="h-10 w-10 bg-indigo-600 rounded-lg flex items-center justify-center text-white font-extrabold text-xl shadow-lg">
            N
          </div>
          <span className="text-2xl font-extrabold tracking-wider text-white">
            NAG <span className="text-indigo-400 font-medium text-lg">Candidate Portal</span>
          </span>
        </div>
        <h2 className="mt-6 text-center text-3xl font-extrabold text-white">
          Verify your credentials
        </h2>
        <p className="mt-2 text-center text-sm text-slate-400">
          A secure OTP has been sent to your registered contact channels.
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md z-10">
        <div className="bg-slate-800 py-8 px-4 shadow-xl border border-slate-700/50 sm:rounded-lg sm:px-10">
          <form className="space-y-6" onSubmit={handleSubmit}>
            {error && (
              <div className="rounded-md bg-red-950/30 border border-red-500/50 p-4">
                <div className="flex">
                  <AlertCircle className="h-5 w-5 text-red-400" />
                  <div className="ml-3">
                    <p className="text-sm font-medium text-red-300">{error}</p>
                  </div>
                </div>
              </div>
            )}

            {success && (
              <div className="rounded-md bg-green-950/30 border border-green-500/50 p-4">
                <div className="flex">
                  <ShieldCheck className="h-5 w-5 text-green-400" />
                  <div className="ml-3">
                    <p className="text-sm font-medium text-green-300">{success}</p>
                  </div>
                </div>
              </div>
            )}

            <div className="space-y-3 bg-slate-900/60 p-4 rounded-lg border border-slate-700/30">
              <div className="flex items-center text-sm text-slate-300">
                <Mail className="h-4.5 w-4.5 text-indigo-400 mr-2" />
                <span>Email OTP sent to: <span className="font-mono text-white">{maskEmail(email)}</span></span>
              </div>
              <div className="flex items-center text-sm text-slate-300 border-t border-slate-700/40 pt-2">
                <Phone className="h-4.5 w-4.5 text-indigo-400 mr-2" />
                <span>Mobile OTP sent to: <span className="font-mono text-white">{maskPhone(mobile)}</span></span>
              </div>
            </div>

            <div>
              <label htmlFor="otp-code" className="block text-sm font-medium text-slate-300 text-center mb-2">
                Enter 6-Digit Verification Code
              </label>
              <input
                id="otp-code"
                type="text"
                maxLength={6}
                required
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                placeholder="0 0 0 0 0 0"
                className="block w-full text-center tracking-[1em] text-xl font-extrabold py-3 bg-slate-900 border border-slate-700 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              />
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
                  'Verify Code'
                )}
              </button>
            </div>

            <div className="text-center">
              {timer > 0 ? (
                <p className="text-xs text-slate-400">
                  Resend OTP in <span className="text-white font-semibold">{timer}s</span>
                </p>
              ) : (
                <button
                  type="button"
                  onClick={handleResend}
                  className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 underline"
                >
                  Resend Verification OTP
                </button>
              )}
            </div>

            <div className="p-3 bg-indigo-950/20 border border-indigo-900/50 rounded-md">
              <div className="flex">
                <Info className="h-5 w-5 text-indigo-400 flex-shrink-0" />
                <div className="ml-2.5">
                  <h4 className="text-xs font-bold text-indigo-300">Prototype Bypass Hint:</h4>
                  <p className="text-xs text-indigo-200 mt-1">
                    Enter <span className="font-mono bg-slate-900 px-1 py-0.5 rounded text-white font-semibold">123456</span> or any 6 digits to verify and progress immediately.
                  </p>
                </div>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
export default VerifyOtp;
