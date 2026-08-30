import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Clock, 
  User, 
  HelpCircle, 
  AlertTriangle
} from 'lucide-react';

interface Question {
  id: number;
  text: string;
  options: string[];
  correctOptionIndex: number;
}

const MOCK_QUESTIONS: Question[] = [
  {
    id: 1,
    text: "Which of the following is an open-source initiative designed to serve as Digital Public Infrastructure (DPI) for secure, scalable assessments?",
    options: ["NAG (Next-generation Assessment Grid)", "Kubernetes Orchestrator", "Apache Kafka Streamer", "HashiCorp Vault Service"],
    correctOptionIndex: 0
  },
  {
    id: 2,
    text: "What type of architecture does NAG follow for scalable cloud deployments?",
    options: ["Monolithic architecture", "Cloud-native microservices architecture", "Peer-to-peer decentralized architecture", "Serverless-only event listeners"],
    correctOptionIndex: 1
  },
  {
    id: 3,
    text: "Which primary programming language and framework is used in the NAG backend?",
    options: ["Node.js & Express", "Python & Django", "Java & Spring Boot", "Go & Gin"],
    correctOptionIndex: 2
  },
  {
    id: 4,
    text: "In NAG, which tool is recommended to manage encrypted credentials and secrets securely?",
    options: ["PostgreSQL Database", "Redis Cache", "HashiCorp Vault", "Apache Kafka"],
    correctOptionIndex: 2
  },
  {
    id: 5,
    text: "What database is primarily configured in NAG for persistent relational records like candidate profiles?",
    options: ["Redis", "PostgreSQL", "OpenSearch", "MongoDB"],
    correctOptionIndex: 1
  },
  {
    id: 6,
    text: "Which protocol is utilized to authenticate candidate logins securely using tokens?",
    options: ["OAuth2 / JWT", "FTP", "SNMP", "SMTP"],
    correctOptionIndex: 0
  },
  {
    id: 7,
    text: "What is the primary script execution engine used by browser frontends (like Angular/React) to build dynamic DOMs?",
    options: ["Python Engine", "V8 JavaScript/TypeScript Engine", "Java Virtual Machine", "C++ compiler"],
    correctOptionIndex: 1
  },
  {
    id: 8,
    text: "What observability tool stack is typically used to trace microservice latency in NAG?",
    options: ["Prometheus & Grafana & OpenTelemetry", "Nginx logs only", "Chrome Developer Tools", "System Event Viewers"],
    correctOptionIndex: 0
  }
];

export const TakeExam: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { submitExamResult } = useAuth();

  const examName = id === 'EXAM001' ? 'National Entrance Examination (Graduate) 2026' : 'AI & Machine Learning Scholarship Test';
  
  // Timer state (90 minutes in seconds = 5400)
  const [timeLeft, setTimeLeft] = useState(5400);

  // Exam answering state
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<{ [key: number]: number }>({}); // questionId -> selectedOptionIndex
  const [statusMap, setStatusMap] = useState<{ [key: number]: 'visited' | 'answered' | 'review' | 'answered-review' }>({
    1: 'visited' // First question is visited by default
  });

  // Submit Modal
  const [showSubmitModal, setShowSubmitModal] = useState(false);

  // Timer Effect
  useEffect(() => {
    if (timeLeft <= 0) {
      handleAutoSubmit();
      return;
    }
    const timer = setInterval(() => setTimeLeft((t) => t - 1), 1000);
    return () => clearInterval(timer);
  }, [timeLeft]);

  // Format Time
  const formatTime = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleSelectOption = (optionIndex: number) => {
    const qId = MOCK_QUESTIONS[currentIndex].id;
    setAnswers(prev => ({ ...prev, [qId]: optionIndex }));
  };

  const handleSaveNext = () => {
    const qId = MOCK_QUESTIONS[currentIndex].id;
    const isAnswered = answers[qId] !== undefined;

    // Update status
    setStatusMap(prev => ({
      ...prev,
      [qId]: isAnswered ? 'answered' : 'visited'
    }));

    // Move to next question if not last
    if (currentIndex < MOCK_QUESTIONS.length - 1) {
      const nextQId = MOCK_QUESTIONS[currentIndex + 1].id;
      setCurrentIndex(prev => prev + 1);
      // Mark next as visited if not already set
      if (!statusMap[nextQId]) {
        setStatusMap(prev => ({ ...prev, [nextQId]: 'visited' }));
      }
    }
  };

  const handleMarkReviewNext = () => {
    const qId = MOCK_QUESTIONS[currentIndex].id;
    const isAnswered = answers[qId] !== undefined;

    setStatusMap(prev => ({
      ...prev,
      [qId]: isAnswered ? 'answered-review' : 'review'
    }));

    if (currentIndex < MOCK_QUESTIONS.length - 1) {
      const nextQId = MOCK_QUESTIONS[currentIndex + 1].id;
      setCurrentIndex(prev => prev + 1);
      if (!statusMap[nextQId]) {
        setStatusMap(prev => ({ ...prev, [nextQId]: 'visited' }));
      }
    }
  };

  const handleClearResponse = () => {
    const qId = MOCK_QUESTIONS[currentIndex].id;
    setAnswers(prev => {
      const copy = { ...prev };
      delete copy[qId];
      return copy;
    });
    setStatusMap(prev => ({
      ...prev,
      [qId]: 'visited'
    }));
  };

  const handlePaletteClick = (index: number) => {
    setCurrentIndex(index);
    const qId = MOCK_QUESTIONS[index].id;
    if (!statusMap[qId]) {
      setStatusMap(prev => ({ ...prev, [qId]: 'visited' }));
    }
  };

  const getPaletteColorClass = (qId: number) => {
    const status = statusMap[qId];
    if (status === 'answered') return 'bg-green-600 text-white border-green-700';
    if (status === 'review') return 'bg-purple-600 text-white border-purple-700';
    if (status === 'answered-review') return 'bg-indigo-700 text-white border-indigo-900 ring-2 ring-purple-300';
    if (status === 'visited') return 'bg-red-500 text-white border-red-600';
    return 'bg-gray-100 text-gray-700 border-gray-300';
  };

  // Counting statistics
  const answeredCount = Object.keys(answers).length;
  const reviewCount = Object.values(statusMap).filter(v => v === 'review').length;
  const answeredReviewCount = Object.values(statusMap).filter(v => v === 'answered-review').length;
  const notAnsweredCount = Object.values(statusMap).filter(v => v === 'visited').length;
  const notVisitedCount = MOCK_QUESTIONS.length - Object.keys(statusMap).length;

  const handleAutoSubmit = () => {
    executeSubmission();
  };

  const executeSubmission = () => {
    // Calculate final score
    let score = 0;
    MOCK_QUESTIONS.forEach(q => {
      if (answers[q.id] === q.correctOptionIndex) {
        score += 1;
      }
    });

    submitExamResult(
      id || 'EXAM001',
      examName,
      score,
      MOCK_QUESTIONS.length
    );
    navigate('/results');
  };

  return (
    <div className="fixed inset-0 bg-slate-100 flex flex-col z-50 overflow-hidden font-sans">
      {/* Exam Taker Header */}
      <header className="bg-slate-900 text-white h-16 px-4 md:px-6 flex items-center justify-between flex-shrink-0 border-b border-slate-800">
        <div className="flex items-center space-x-3">
          <div className="h-8 w-8 bg-indigo-600 rounded flex items-center justify-center font-black text-sm">N</div>
          <div>
            <h1 className="text-xs md:text-sm font-bold truncate max-w-[200px] md:max-w-md">{examName}</h1>
            <p className="text-[10px] text-slate-400">Subject: General Aptitude & Computer Sciences</p>
          </div>
        </div>

        {/* Timer Block */}
        <div className="flex items-center space-x-2 bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 shadow-inner">
          <Clock className="h-4.5 w-4.5 text-indigo-400 animate-pulse" />
          <span className="font-mono text-xs md:text-sm font-bold text-slate-100">
            Time Left: <span className={timeLeft < 300 ? 'text-red-500 font-extrabold animate-pulse' : 'text-green-400'}>{formatTime(timeLeft)}</span>
          </span>
        </div>
      </header>

      {/* Main Split Section */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Side: Question Display */}
        <div className="flex-1 flex flex-col justify-between overflow-y-auto bg-white p-4 md:p-6 lg:p-8">
          <div className="max-w-3xl w-full mx-auto space-y-6">
            {/* Question title and navigation */}
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h2 className="text-base font-extrabold text-slate-800 flex items-center">
                <HelpCircle className="h-5 w-5 mr-2 text-indigo-600" />
                Question {currentIndex + 1} of {MOCK_QUESTIONS.length}
              </h2>
              <span className="text-xs bg-indigo-50 text-indigo-700 font-bold px-2 py-0.5 rounded">
                Marks: +1.0 | -0.25
              </span>
            </div>

            {/* Question Text */}
            <div className="bg-slate-50 p-5 rounded-xl border border-slate-200/60 shadow-sm">
              <p className="text-slate-800 font-semibold text-sm md:text-base leading-relaxed whitespace-pre-line">
                {MOCK_QUESTIONS[currentIndex].text}
              </p>
            </div>

            {/* Options */}
            <div className="space-y-3">
              {MOCK_QUESTIONS[currentIndex].options.map((option, idx) => {
                const isSelected = answers[MOCK_QUESTIONS[currentIndex].id] === idx;
                return (
                  <button
                    key={idx}
                    onClick={() => handleSelectOption(idx)}
                    className={`w-full text-left p-4 rounded-xl border text-sm transition-all flex items-center ${
                      isSelected
                        ? 'border-indigo-600 bg-indigo-50/50 text-indigo-900 font-bold shadow-sm'
                        : 'border-gray-200 hover:bg-slate-50 text-slate-700'
                    }`}
                  >
                    <span className={`h-5 w-5 rounded-full border flex items-center justify-center mr-3 flex-shrink-0 text-xs font-semibold ${
                      isSelected ? 'border-indigo-600 bg-indigo-600 text-white' : 'border-gray-400 text-gray-500'
                    }`}>
                      {String.fromCharCode(65 + idx)}
                    </span>
                    {option}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Action Footer */}
          <div className="border-t border-gray-200 pt-4 mt-8 bg-white flex flex-wrap gap-3 justify-between items-center max-w-3xl w-full mx-auto">
            <div className="flex space-x-2">
              <button
                onClick={handleClearResponse}
                className="px-4 py-2 border border-gray-300 hover:bg-gray-50 text-gray-700 text-xs font-bold rounded-lg transition-colors"
              >
                Clear Response
              </button>
              <button
                onClick={handleMarkReviewNext}
                className="px-4 py-2 bg-purple-100 hover:bg-purple-200 text-purple-800 text-xs font-bold rounded-lg transition-colors"
              >
                Mark for Review & Next
              </button>
            </div>
            
            <div className="flex space-x-2">
              <button
                onClick={handleSaveNext}
                className="px-5 py-2 bg-green-600 hover:bg-green-700 text-white text-xs font-bold rounded-lg transition-colors shadow-sm"
              >
                {currentIndex === MOCK_QUESTIONS.length - 1 ? 'Save & Review Palette' : 'Save & Next'}
              </button>
            </div>
          </div>
        </div>

        {/* Right Side: Question Palette & Candidate Profile */}
        <aside className="w-80 bg-slate-900 text-white flex flex-col justify-between border-l border-slate-800 flex-shrink-0 hidden lg:flex">
          {/* Candidate Profile Details */}
          <div className="p-4 bg-slate-950 border-b border-slate-800 flex items-center space-x-3">
            <div className="h-12 w-12 rounded bg-indigo-600 flex items-center justify-center font-bold text-white shadow-inner uppercase">
              <User className="h-6 w-6" />
            </div>
            <div>
              <p className="text-xs text-slate-400">Candidate Code: CND-782</p>
              <p className="text-sm font-bold truncate text-slate-100">Jane Doe</p>
            </div>
          </div>

          {/* Question grid palette */}
          <div className="flex-1 p-4 overflow-y-auto space-y-4">
            <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-400">Question Palette</h3>
            <div className="grid grid-cols-4 gap-2.5">
              {MOCK_QUESTIONS.map((q, idx) => (
                <button
                  key={q.id}
                  onClick={() => handlePaletteClick(idx)}
                  className={`h-10 w-10 border rounded-lg font-bold text-xs flex items-center justify-center transition-all ${getPaletteColorClass(q.id)} ${
                    currentIndex === idx ? 'ring-2 ring-indigo-400 ring-offset-2 ring-offset-slate-900 scale-105' : ''
                  }`}
                >
                  {q.id}
                </button>
              ))}
            </div>
          </div>

          {/* Palette Legend Summary & Submit */}
          <div className="p-4 border-t border-slate-800 bg-slate-950 space-y-4">
            <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-300">
              <div className="flex items-center"><span className="h-3 w-3 bg-green-600 rounded mr-1.5"></span>Answered ({answeredCount})</div>
              <div className="flex items-center"><span className="h-3 w-3 bg-red-500 rounded mr-1.5"></span>Not Answered ({notAnsweredCount})</div>
              <div className="flex items-center"><span className="h-3 w-3 bg-purple-600 rounded mr-1.5"></span>Marked Review ({reviewCount})</div>
              <div className="flex items-center"><span className="h-3 w-3 bg-indigo-700 rounded mr-1.5"></span>Ans & Review ({answeredReviewCount})</div>
              <div className="flex items-center"><span className="h-3 w-3 bg-gray-100 rounded mr-1.5"></span>Not Visited ({notVisitedCount})</div>
            </div>

            <button
              onClick={() => setShowSubmitModal(true)}
              className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg font-extrabold text-xs shadow-md uppercase tracking-wider transition-colors"
            >
              Submit Exam
            </button>
          </div>
        </aside>
      </div>

      {/* Mobile Submit Button (Floating for small screens) */}
      <div className="lg:hidden p-3 bg-white border-t border-gray-200 flex space-x-2">
        <button 
          onClick={() => {
            // cycle previous
            if (currentIndex > 0) setCurrentIndex(prev => prev - 1);
          }}
          disabled={currentIndex === 0}
          className="flex-1 py-2 border border-gray-300 rounded-lg text-xs font-bold text-gray-700 disabled:opacity-30"
        >
          Previous
        </button>
        <button 
          onClick={() => {
            // cycle next
            if (currentIndex < MOCK_QUESTIONS.length - 1) setCurrentIndex(prev => prev + 1);
          }}
          disabled={currentIndex === MOCK_QUESTIONS.length - 1}
          className="flex-1 py-2 border border-gray-300 rounded-lg text-xs font-bold text-gray-700 disabled:opacity-30"
        >
          Next
        </button>
        <button
          onClick={() => setShowSubmitModal(true)}
          className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold"
        >
          Submit Exam
        </button>
      </div>

      {/* Confirmation Submit Modal */}
      {showSubmitModal && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full overflow-hidden border border-gray-200">
            <div className="p-5 border-b border-gray-100 flex items-center bg-red-50 text-red-800">
              <AlertTriangle className="h-5 w-5 mr-2 text-red-600" />
              <h3 className="font-extrabold text-sm uppercase tracking-wider">Confirm Exam Submission</h3>
            </div>
            
            <div className="p-5 space-y-4">
              <p className="text-xs text-gray-500">
                You are about to submit your response sheets. Once submitted, answers cannot be edited.
              </p>
              
              <div className="bg-slate-50 p-4 rounded-lg border border-slate-200/60 text-xs space-y-2 text-slate-700">
                <div className="flex justify-between font-semibold"><span>Total Questions:</span> <span className="text-slate-900">{MOCK_QUESTIONS.length}</span></div>
                <div className="flex justify-between text-green-700 font-semibold"><span>Answered Questions:</span> <span>{answeredCount}</span></div>
                <div className="flex justify-between text-red-600 font-semibold"><span>Unanswered / Visited:</span> <span>{notAnsweredCount}</span></div>
                <div className="flex justify-between text-purple-700 font-semibold"><span>Marked for Review:</span> <span>{reviewCount + answeredReviewCount}</span></div>
              </div>

              <p className="text-xs text-slate-800 font-bold text-center">
                Are you sure you want to end the test?
              </p>
            </div>

            <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end space-x-2">
              <button
                onClick={() => setShowSubmitModal(false)}
                className="px-4 py-2 border border-gray-300 hover:bg-gray-100 rounded-lg text-xs font-bold text-gray-700 transition-colors"
              >
                Back to Exam
              </button>
              <button
                onClick={executeSubmission}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg text-xs font-bold text-white transition-colors flex items-center"
              >
                Yes, Submit Test
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
export default TakeExam;
