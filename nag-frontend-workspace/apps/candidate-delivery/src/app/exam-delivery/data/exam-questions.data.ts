import { ExamItem } from '../models';

export const PREVIEW_QUESTIONS: ExamItem[] = [
  {
    id: 'prev-1',
    order: 1,
    questionCode: 'SAMPLE-NAV-01',
    content:
      '**Sample Orientation Item**: Which section of the NAG candidate runtime environment allows you to monitor unanswered questions and mark items for later review?',
    options: [
      { id: 'opt-a', text: 'Top Proctoring Heartbeat Ribbon' },
      { id: 'opt-b', text: 'Right-hand Question Palette with color-coded item indicators' },
      { id: 'opt-c', text: 'Language selector dropdown' },
      { id: 'opt-d', text: 'Cryptographic hash audit proof ledger' },
    ],
    marks: 4,
    negativeMarks: 0,
    isVisited: true,
  },
  {
    id: 'prev-2',
    order: 2,
    questionCode: 'SAMPLE-APT-02',
    content:
      '**Sample Quantitative Aptitude**: A train traveling at a constant speed of $72\\text{ km/h}$ crosses a $200\\text{ m}$ long station platform in $20\\text{ seconds}$. What is the length of the train in meters?\\n\\n$$\\text{Speed} = 72 \\times \\frac{5}{18} = 20\\text{ m/s}$$',
    options: [
      { id: 'opt-a', text: '$150\\text{ m}$' },
      { id: 'opt-b', text: '$200\\text{ m}$' },
      { id: 'opt-c', text: '$250\\text{ m}$' },
      { id: 'opt-d', text: '$300\\text{ m}$' },
    ],
    marks: 4,
    negativeMarks: 1,
  },
  {
    id: 'prev-3',
    order: 3,
    questionCode: 'SAMPLE-SYS-03',
    content:
      '**Sample Architecture Pattern**: In the zero-trust assessment delivery architecture, candidate responses are periodically sealed using which cryptographic mechanism?',
    options: [
      { id: 'opt-a', text: 'Client LocalStorage base64 encoded strings' },
      { id: 'opt-b', text: 'Tamper-evident SHA-256 digital signature digests' },
      { id: 'opt-c', text: 'Unsigned plaintext JSON web tokens' },
      { id: 'opt-d', text: 'Browser cookie session tracking' },
    ],
    marks: 4,
    negativeMarks: 1,
  },
];

export const PRACTICE_QUESTIONS: ExamItem[] = [
  {
    id: 'prac-1',
    order: 1,
    questionCode: 'PRAC-ALGO-101',
    content:
      'What is the tightest worst-case asymptotic time complexity of building a Max-Heap from an unsorted array of $n$ elements using Floyd’s linear build-heap algorithm?\\n\\n$$\\sum_{h=0}^{\\lfloor \\lg n \\rfloor} \\left\\lceil \\frac{n}{2^{h+1}} \\right\\rceil O(h) = O(n)$$',
    options: [
      { id: 'opt-a', text: '$O(n \\log n)$' },
      { id: 'opt-b', text: '$O(n)$' },
      { id: 'opt-c', text: '$O(\\log n)$' },
      { id: 'opt-d', text: '$O(n^2)$' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-b',
    isVisited: true,
  },
  {
    id: 'prac-2',
    order: 2,
    questionCode: 'PRAC-MATH-202',
    content:
      'Compute the determinant of the $2 \\times 2$ covariance matrix given by:\\n\\n$$\\mathbf{\\Sigma} = \\begin{pmatrix} 4 & 2 \\\\ 2 & 3 \\end{pmatrix}$$',
    options: [
      { id: 'opt-a', text: '$8$' },
      { id: 'opt-b', text: '$12$' },
      { id: 'opt-c', text: '$10$' },
      { id: 'opt-d', text: '$16$' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-a',
  },
  {
    id: 'prac-3',
    order: 3,
    questionCode: 'PRAC-SYS-305',
    content:
      'In an operating system with a 32-bit virtual address space and a $4\\text{ KB}$ page size, calculate the number of page entries required in a single-level page table.\\n\\n$$\\text{Number of Pages} = \\frac{2^{32}}{2^{12}} = 2^{20}$$',
    options: [
      { id: 'opt-a', text: '$2^{10} = 1,024$' },
      { id: 'opt-b', text: '$2^{20} = 1,048,576$' },
      { id: 'opt-c', text: '$2^{12} = 4,096$' },
      { id: 'opt-d', text: '$2^{32} = 4,294,967,296$' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-b',
  },
  {
    id: 'prac-4',
    order: 4,
    questionCode: 'PRAC-DS-404',
    content:
      'Which data structure provides amortized $O(1)$ time complexity for both `find` and `union` operations with path compression and rank heuristics?',
    options: [
      { id: 'opt-a', text: 'Disjoint Set Union (Union-Find)' },
      { id: 'opt-b', text: 'Fibonacci Heap' },
      { id: 'opt-c', text: 'Red-Black Balanced Search Tree' },
      { id: 'opt-d', text: 'Trie Data Structure' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-a',
  },
  {
    id: 'prac-5',
    order: 5,
    questionCode: 'PRAC-NET-505',
    content:
      'In the TCP/IP protocol suite, which congestion control mechanism dynamically adjusts the congestion window (CWND) size upon encountering triple duplicate ACKs?',
    options: [
      { id: 'opt-a', text: 'Slow Start Exponential Growth' },
      { id: 'opt-b', text: 'Fast Retransmit and Fast Recovery' },
      { id: 'opt-c', text: 'Nagle Algorithm Packet Coalescing' },
      { id: 'opt-d', text: 'Sliding Window Flow Control' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-b',
  },
];

export const LIVE_QUESTIONS: ExamItem[] = [
  {
    id: 'live-1',
    order: 1,
    questionCode: 'NES-ALGO-101',
    content:
      'What is the tightest worst-case asymptotic time complexity of building a Max-Heap from an unsorted array of $n$ elements using Floyd’s algorithm?\\n\\n$$\\sum_{h=0}^{\\lfloor \\lg n \\rfloor} \\left\\lceil \\frac{n}{2^{h+1}} \\right\\rceil O(h) = O(n)$$',
    options: [
      { id: 'opt-a', text: '$O(n \\log n)$' },
      { id: 'opt-b', text: '$O(n)$' },
      { id: 'opt-c', text: '$O(\\log n)$' },
      { id: 'opt-d', text: '$O(n^2)$' },
    ],
    marks: 4,
    negativeMarks: 1,
    isVisited: true,
  },
  {
    id: 'live-2',
    order: 2,
    questionCode: 'NES-MATH-202',
    content:
      'Compute the determinant of the $2 \\times 2$ covariance matrix given by:\\n\\n$$\\mathbf{\\Sigma} = \\begin{pmatrix} 4 & 2 \\\\ 2 & 3 \\end{pmatrix}$$',
    options: [
      { id: 'opt-a', text: '$8$' },
      { id: 'opt-b', text: '$12$' },
      { id: 'opt-c', text: '$10$' },
      { id: 'opt-d', text: '$16$' },
    ],
    marks: 4,
    negativeMarks: 1,
  },
  {
    id: 'live-3',
    order: 3,
    questionCode: 'NES-SYS-305',
    content:
      'In an operating system with a 32-bit virtual address space and a 4 KB page size, calculate the number of entries in a single-level page table.',
    options: [
      { id: 'opt-a', text: '$2^{10} = 1,024$' },
      { id: 'opt-b', text: '$2^{20} = 1,048,576$' },
      { id: 'opt-c', text: '$2^{12} = 4,096$' },
      { id: 'opt-d', text: '$2^{32} = 4,294,967,296$' },
    ],
    marks: 4,
    negativeMarks: 1,
  },
];
