import React, { createContext, useContext, useState, useEffect } from 'react';

export interface UserProfile {
  name: string;
  email: string;
  mobile: string;
  dob?: string;
  gender?: string;
  category?: string;
  address?: string;
  qualification?: string;
  university?: string;
  passingYear?: string;
  percentage?: string;
  photoUploaded?: boolean;
  signatureUploaded?: boolean;
  idProofUploaded?: boolean;
  registeredExams: string[]; // Exam IDs
  completedExams: {
    examId: string;
    examName: string;
    score: number;
    totalQuestions: number;
    percentile: number;
    rank: number;
    date: string;
  }[];
}

interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isVerified: boolean;
  otpSentTo: { email: string; mobile: string } | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;
  register: (name: string, email: string, mobile: string, password: string) => Promise<void>;
  verifyOtp: (otp: string) => Promise<boolean>;
  resendOtp: () => Promise<void>;
  updateProfile: (details: Partial<UserProfile>) => void;
  applyForExam: (examId: string) => void;
  submitExamResult: (examId: string, examName: string, score: number, totalQuestions: number) => void;
  changePassword: (oldPass: string, newPass: string) => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const DEFAULT_USER: UserProfile = {
  name: "John Doe",
  email: "candidate@nag.gov.in",
  mobile: "9876543210",
  dob: "2000-01-01",
  gender: "Male",
  category: "General",
  address: "123, Admin Colony, New Delhi, 110001",
  qualification: "B.Tech in Computer Science",
  university: "State Technical University",
  passingYear: "2023",
  percentage: "82.5",
  photoUploaded: true,
  signatureUploaded: true,
  idProofUploaded: true,
  registeredExams: ["EXAM001"],
  completedExams: [
    {
      examId: "EXAM000",
      examName: "Foundation Assessment Test 2025",
      score: 85,
      totalQuestions: 100,
      percentile: 98.4,
      rank: 142,
      date: "2025-11-15"
    }
  ]
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(() => {
    const saved = localStorage.getItem('nag_candidate_user');
    return saved ? JSON.parse(saved) : DEFAULT_USER;
  });

  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return localStorage.getItem('nag_candidate_auth') === 'true';
  });

  const [isVerified, setIsVerified] = useState<boolean>(() => {
    return localStorage.getItem('nag_candidate_verified') === 'true';
  });

  const [otpSentTo, setOtpSentTo] = useState<{ email: string; mobile: string } | null>(() => {
    const saved = localStorage.getItem('nag_candidate_otp_sent');
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem('nag_candidate_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('nag_candidate_user');
    }
  }, [user]);

  useEffect(() => {
    localStorage.setItem('nag_candidate_auth', String(isAuthenticated));
  }, [isAuthenticated]);

  useEffect(() => {
    localStorage.setItem('nag_candidate_verified', String(isVerified));
  }, [isVerified]);

  useEffect(() => {
    if (otpSentTo) {
      localStorage.setItem('nag_candidate_otp_sent', JSON.stringify(otpSentTo));
    } else {
      localStorage.removeItem('nag_candidate_otp_sent');
    }
  }, [otpSentTo]);

  const login = async (email: string, password: string): Promise<boolean> => {
    // Simple mock authentication
    // Check default credentials or registered credentials
    await new Promise((resolve) => setTimeout(resolve, 800)); // Simulate API call
    
    if (email === "candidate@nag.gov.in" && password === "password123") {
      setIsAuthenticated(true);
      setIsVerified(true);
      return true;
    }

    if (user && email === user.email && password === "password123") {
      setIsAuthenticated(true);
      setIsVerified(true);
      return true;
    }

    // Allow mock login for any credentials for prototype ease
    if (user && email === user.email) {
      setIsAuthenticated(true);
      setIsVerified(true);
      return true;
    }

    return false;
  };

  const logout = () => {
    setIsAuthenticated(false);
    setIsVerified(false);
    setOtpSentTo(null);
  };

  const register = async (name: string, email: string, mobile: string, password: string) => {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    
    // Create new profile skeleton
    const newProfile: UserProfile = {
      name,
      email,
      mobile,
      registeredExams: [],
      completedExams: []
    };
    
    // Keep reference to mock password
    console.log("Mock registered with password length:", password.length);
    
    setUser(newProfile);
    setIsAuthenticated(true); // Logged in but not verified
    setIsVerified(false);
    setOtpSentTo({ email, mobile });
  };

  const verifyOtp = async (otp: string): Promise<boolean> => {
    await new Promise((resolve) => setTimeout(resolve, 800));
    if (otp === "123456" || otp.length === 6) { // Mock code
      setIsVerified(true);
      setOtpSentTo(null);
      return true;
    }
    return false;
  };

  const resendOtp = async () => {
    await new Promise((resolve) => setTimeout(resolve, 500));
  };

  const updateProfile = (details: Partial<UserProfile>) => {
    setUser((prev) => {
      if (!prev) return null;
      return { ...prev, ...details };
    });
  };

  const applyForExam = (examId: string) => {
    setUser((prev) => {
      if (!prev) return null;
      if (prev.registeredExams.includes(examId)) return prev;
      return {
        ...prev,
        registeredExams: [...prev.registeredExams, examId]
      };
    });
  };

  const submitExamResult = (examId: string, examName: string, score: number, totalQuestions: number) => {
    setUser((prev) => {
      if (!prev) return null;
      
      // Remove from registered, add to completed
      const newRegistered = prev.registeredExams.filter(id => id !== examId);
      
      const newCompletedItem = {
        examId,
        examName,
        score,
        totalQuestions,
        percentile: parseFloat((70 + Math.random() * 29).toFixed(2)), // Random mock percentile 70-99%
        rank: Math.floor(Math.random() * 500) + 1,
        date: new Date().toISOString().split('T')[0]
      };

      return {
        ...prev,
        registeredExams: newRegistered,
        completedExams: [...prev.completedExams, newCompletedItem]
      };
    });
  };

  const changePassword = async (oldPass: string, newPass: string): Promise<boolean> => {
    await new Promise((resolve) => setTimeout(resolve, 800));
    return oldPass.length > 0 && newPass.length > 0; // Simulate check
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isVerified,
        otpSentTo,
        login,
        logout,
        register,
        verifyOtp,
        resendOtp,
        updateProfile,
        applyForExam,
        submitExamResult,
        changePassword
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
