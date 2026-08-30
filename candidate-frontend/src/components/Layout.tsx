import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
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
  Bell
} from 'lucide-react';

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Profile Management', href: '/profile', icon: User },
    { name: 'Browse Exams', href: '/exams', icon: BookOpen },
    { name: 'Exam Results', href: '/results', icon: Award },
    { name: 'Password Management', href: '/password-management', icon: KeyRound },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Calculate profile completeness
  const calculateCompleteness = () => {
    if (!user) return 0;
    let score = 0;
    if (user.name) score += 10;
    if (user.email) score += 10;
    if (user.mobile) score += 10;
    if (user.dob) score += 10;
    if (user.gender) score += 10;
    if (user.address) score += 10;
    if (user.qualification) score += 15;
    if (user.photoUploaded) score += 10;
    if (user.signatureUploaded) score += 10;
    if (user.idProofUploaded) score += 5;
    return score;
  };

  const profileScore = calculateCompleteness();

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar for Desktop */}
      <aside className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0 bg-slate-900 text-white z-20">
        <div className="flex flex-col flex-grow pt-5 pb-4 overflow-y-auto">
          {/* Logo Area */}
          <div className="flex items-center flex-shrink-0 px-4 mb-6">
            <span className="text-xl font-extrabold tracking-wider bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">
              NAG CANDIDATE
            </span>
          </div>

          {/* User Brief */}
          <div className="px-4 py-3 mb-6 bg-slate-800/50 border-y border-slate-700/50 flex items-center space-x-3">
            <div className="h-10 w-10 rounded-full bg-indigo-600 flex items-center justify-center font-bold text-white uppercase shadow-inner">
              {user?.name ? user.name.charAt(0) : 'C'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold truncate text-slate-100">{user?.name}</p>
              <div className="flex items-center text-xs text-green-400 mt-0.5">
                <CheckCircle className="h-3.5 w-3.5 mr-1" /> Verified Candidate
              </div>
            </div>
          </div>

          {/* Nav Items */}
          <nav className="flex-1 px-2 space-y-1">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`group flex items-center px-3 py-2.5 text-sm font-medium rounded-md transition-colors ${
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
        </div>

        {/* Footer / Logout */}
        <div className="flex-shrink-0 flex border-t border-slate-800 p-4">
          <button
            onClick={handleLogout}
            className="group flex items-center w-full px-3 py-2.5 text-sm font-medium rounded-md text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-colors"
          >
            <LogOut className="mr-3 h-5 w-5" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Mobile Drawer Sidebar */}
      {sidebarOpen && (
        <div className="md:hidden fixed inset-0 z-40 flex">
          {/* Overlay */}
          <div className="fixed inset-0 bg-gray-600 bg-opacity-75" onClick={() => setSidebarOpen(false)}></div>

          {/* Drawer Content */}
          <div className="relative flex-1 flex flex-col max-w-xs w-full pt-5 pb-4 bg-slate-900 text-white">
            <div className="absolute top-0 right-0 -mr-12 pt-2">
              <button
                type="button"
                className="ml-1 flex items-center justify-center h-10 w-10 rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
                onClick={() => setSidebarOpen(false)}
              >
                <X className="h-6 w-6 text-white" />
              </button>
            </div>
            
            <div className="flex-shrink-0 flex items-center px-4 mb-6">
              <span className="text-xl font-extrabold tracking-wider bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">
                NAG CANDIDATE
              </span>
            </div>

            <div className="px-4 py-3 mb-6 bg-slate-800/50 border-y border-slate-700/50 flex items-center space-x-3">
              <div className="h-10 w-10 rounded-full bg-indigo-600 flex items-center justify-center font-bold text-white uppercase">
                {user?.name ? user.name.charAt(0) : 'C'}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold truncate text-slate-100">{user?.name}</p>
                <div className="flex items-center text-xs text-green-400 mt-0.5">
                  <CheckCircle className="h-3 w-3 mr-1" /> Verified Candidate
                </div>
              </div>
            </div>

            <nav className="flex-1 px-2 space-y-1 overflow-y-auto">
              {navigation.map((item) => {
                const isActive = location.pathname === item.href;
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    onClick={() => setSidebarOpen(false)}
                    className={`group flex items-center px-3 py-2.5 text-sm font-medium rounded-md transition-colors ${
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

            <div className="flex-shrink-0 flex border-t border-slate-800 p-4">
              <button
                onClick={handleLogout}
                className="group flex items-center w-full px-3 py-2.5 text-sm font-medium rounded-md text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-colors"
              >
                <LogOut className="mr-3 h-5 w-5" />
                Sign Out
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <div className="flex flex-col flex-1 md:pl-64">
        {/* Top Navbar */}
        <header className="sticky top-0 z-10 flex-shrink-0 flex h-16 bg-white border-b border-gray-200">
          <button
            type="button"
            className="px-4 border-r border-gray-200 text-gray-500 focus:outline-none md:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="h-6 w-6" />
          </button>
          
          <div className="flex-1 px-4 flex justify-between">
            {/* Left: Section name / search */}
            <div className="flex items-center">
              <span className="text-sm md:text-base font-semibold text-slate-800 uppercase tracking-wider">
                {navigation.find(nav => nav.href === location.pathname)?.name || 'Candidate Space'}
              </span>
            </div>

            {/* Right: Notifications & Profile Completeness */}
            <div className="ml-4 flex items-center md:ml-6 space-x-4">
              {/* Profile completeness meter */}
              <div className="hidden lg:flex flex-col items-end space-y-1">
                <div className="flex items-center text-xs text-gray-500">
                  <span className="font-semibold text-gray-700 mr-1">Profile Completeness:</span> {profileScore}%
                </div>
                <div className="w-48 bg-gray-200 rounded-full h-2">
                  <div 
                    className={`h-2 rounded-full transition-all duration-500 ${
                      profileScore < 50 ? 'bg-red-500' : profileScore < 85 ? 'bg-amber-500' : 'bg-green-500'
                    }`} 
                    style={{ width: `${profileScore}%` }}
                  ></div>
                </div>
              </div>

              {profileScore < 85 && (
                <Link to="/profile" className="hidden sm:flex items-center px-3 py-1 bg-amber-50 text-amber-800 text-xs font-semibold rounded-full border border-amber-200">
                  <AlertCircle className="h-3.5 w-3.5 mr-1" />
                  Complete Profile
                </Link>
              )}

              <button className="p-1 rounded-full text-gray-400 hover:text-gray-500 focus:outline-none">
                <span className="sr-only">View notifications</span>
                <Bell className="h-6 w-6" />
              </button>

              <div className="h-8 w-8 rounded-full bg-indigo-100 flex items-center justify-center font-bold text-indigo-700 border border-indigo-200 text-sm">
                {user?.name ? user.name.charAt(0) : 'C'}
              </div>
            </div>
          </div>
        </header>

        {/* Main Section */}
        <main className="flex-1 p-4 md:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>
    </div>
  );
};
export default Layout;
