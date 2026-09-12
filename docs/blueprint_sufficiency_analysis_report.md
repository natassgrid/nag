# Blueprint Sufficiency & Question Bank Inventory Audit Report

**Execution Environment**: Live Docker PostgreSQL (`exam_platform` database)

- **Total Approved Questions in Question Bank**: `1436`
- **Total Blueprint Templates Evaluated**: `8`
- **Feasible Templates (Immediate Single Shift)**: `1 / 8`
- **Templates with Rule Shortages**: `7 / 8`
- **Total Immediate Question Deficit (Generation Blockers)**: `441` questions
- **Total Questions to Generate for 3x Multi-Shift Rotation Buffer**: `1544` questions

---

## 1. Blueprint Template Feasibility Scorecard

| # | Blueprint Template Name | Total Qs Needed | Rules Count | Starved Rules | Immediate Feasibility | Multi-Shift Buffer Status |
|---|---|---|---|---|---|---|
| 1 | SSC CGL Tier-1 (100 Questions - Standard) | 100 | 32 | 0 | ✅ **FEASIBLE** | 🟡 Low Headroom (22 rules <3x) |
| 2 | Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs) | 100 | 15 | 8 | ❌ **DEFICIT (8 rules)** | 🟡 Low Headroom (13 rules <3x) |
| 3 | RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) | 100 | 17 | 8 | ❌ **DEFICIT (8 rules)** | 🟡 Low Headroom (16 rules <3x) |
| 4 | UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) | 100 | 7 | 7 | ❌ **DEFICIT (7 rules)** | 🟡 Low Headroom (7 rules <3x) |
| 5 | UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) | 80 | 7 | 3 | ❌ **DEFICIT (3 rules)** | 🟡 Low Headroom (5 rules <3x) |
| 6 | SSC CGL Tier-2 (Paper-1 Core - 150 Qs) | 150 | 28 | 15 | ❌ **DEFICIT (15 rules)** | 🟡 Low Headroom (26 rules <3x) |
| 7 | State PSC Combined Prelims (General Studies - 150 Qs) | 150 | 8 | 8 | ❌ **DEFICIT (8 rules)** | 🟡 Low Headroom (8 rules <3x) |
| 8 | CTET / TET Paper-1 (Primary Teacher - 150 Qs) | 150 | 16 | 15 | ❌ **DEFICIT (15 rules)** | 🟡 Low Headroom (16 rules <3x) |

---

## 2. Blueprint-by-Blueprint Detailed Shortage Analysis

### 📋 SSC CGL Tier-1 (100 Questions - Standard)
- **Total Rules**: 32 | **Questions Needed Per Exam**: 100
- **Status**: ✅ All rules satisfied for immediate paper generation.

### 📋 Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)
- **Total Rules**: 15 | **Questions Needed Per Exam**: 100
- **Status**: ❌ **INSUFFICIENT** (8 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| English Language and Comprehension | Comprehension Passage | `HARD` | 10 | **0** | ⛔ **-10** | 30 | 30 |
| English Language and Comprehension | Cloze Passage | `MEDIUM` | 5 | **1** | ⛔ **-4** | 15 | 14 |
| English Language and Comprehension | Sentence Shuffling | `MEDIUM` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| English Language and Comprehension | Vocabulary | `EASY` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| Quantitative Aptitude / Mathematical Abilities | Statistics and Probability | `HARD` | 10 | **1** | ⛔ **-9** | 30 | 29 |
| General Intelligence and Reasoning | Coding and Decoding | `EASY` | 5 | **1** | ⛔ **-4** | 15 | 14 |
| General Intelligence and Reasoning | Problem Solving | `HARD` | 10 | **1** | ⛔ **-9** | 30 | 29 |
| General Intelligence and Reasoning | Drawing Inferences | `MEDIUM` | 5 | **0** | ⛔ **-5** | 15 | 15 |

### 📋 RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)
- **Total Rules**: 17 | **Questions Needed Per Exam**: 100
- **Status**: ❌ **INSUFFICIENT** (8 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| General Awareness | Economic Scene | `EASY` | 4 | **0** | ⛔ **-4** | 12 | 12 |
| General Awareness | India and Neighbouring Countries | `MEDIUM` | 4 | **0** | ⛔ **-4** | 12 | 12 |
| Mathematics | Arithmetic | `MEDIUM` | 12 | **0** | ⛔ **-12** | 36 | 36 |
| Mathematics | Algebra | `MEDIUM` | 6 | **0** | ⛔ **-6** | 18 | 18 |
| Mathematics | Geometry | `HARD` | 4 | **0** | ⛔ **-4** | 12 | 12 |
| Mathematics | Trigonometry | `MEDIUM` | 4 | **0** | ⛔ **-4** | 12 | 12 |
| Mathematics | Statistics | `EASY` | 4 | **0** | ⛔ **-4** | 12 | 12 |
| General Intelligence and Reasoning | Coding and Decoding | `EASY` | 5 | **1** | ⛔ **-4** | 15 | 14 |

### 📋 UPSC CSE Prelims Paper-1 (General Studies - 100 Qs)
- **Total Rules**: 7 | **Questions Needed Per Exam**: 100
- **Status**: ❌ **INSUFFICIENT** (7 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| General Studies | Indian History | `HARD` | 18 | **0** | ⛔ **-18** | 54 | 54 |
| General Studies | Indian Polity | `HARD` | 18 | **0** | ⛔ **-18** | 54 | 54 |
| General Studies | Indian Geography | `MEDIUM` | 16 | **0** | ⛔ **-16** | 48 | 48 |
| General Studies | Indian Economy | `HARD` | 16 | **0** | ⛔ **-16** | 48 | 48 |
| General Studies | Science & Technology | `MEDIUM` | 12 | **0** | ⛔ **-12** | 36 | 36 |
| General Studies | Environment | `HARD` | 12 | **0** | ⛔ **-12** | 36 | 36 |
| General Studies | Current Affairs | `MEDIUM` | 8 | **0** | ⛔ **-8** | 24 | 24 |

### 📋 UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs)
- **Total Rules**: 7 | **Questions Needed Per Exam**: 80
- **Status**: ❌ **INSUFFICIENT** (3 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| English Language and Comprehension | Comprehension Passage | `HARD` | 28 | **0** | ⛔ **-28** | 84 | 84 |
| General Intelligence and Reasoning | Critical Thinking | `HARD` | 10 | **7** | ⛔ **-3** | 30 | 23 |
| Quantitative Aptitude / Mathematical Abilities | Number Systems | `HARD` | 6 | **4** | ⛔ **-2** | 18 | 14 |

### 📋 SSC CGL Tier-2 (Paper-1 Core - 150 Qs)
- **Total Rules**: 28 | **Questions Needed Per Exam**: 150
- **Status**: ❌ **INSUFFICIENT** (15 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| Quantitative Aptitude / Mathematical Abilities | Algebra | `HARD` | 5 | **2** | ⛔ **-3** | 15 | 13 |
| Quantitative Aptitude / Mathematical Abilities | Trigonometry | `MEDIUM` | 4 | **1** | ⛔ **-3** | 12 | 11 |
| Quantitative Aptitude / Mathematical Abilities | Mensuration | `HARD` | 4 | **2** | ⛔ **-2** | 12 | 10 |
| General Intelligence and Reasoning | Problem Solving | `HARD` | 8 | **1** | ⛔ **-7** | 24 | 23 |
| General Intelligence and Reasoning | Numerical Operations | `MEDIUM` | 4 | **1** | ⛔ **-3** | 12 | 11 |
| English Language and Comprehension | Comprehension Passage | `HARD` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| English Language and Comprehension | Cloze Passage | `MEDIUM` | 10 | **1** | ⛔ **-9** | 30 | 29 |
| English Language and Comprehension | Active and Passive Voice | `MEDIUM` | 5 | **1** | ⛔ **-4** | 15 | 14 |
| English Language and Comprehension | Direct and Indirect Narration | `MEDIUM` | 5 | **1** | ⛔ **-4** | 15 | 14 |
| English Language and Comprehension | Vocabulary | `EASY` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| General Awareness | Scientific Research | `MEDIUM` | 3 | **1** | ⛔ **-2** | 9 | 8 |
| Computer Knowledge | Computer Basics | `EASY` | 6 | **0** | ⛔ **-6** | 18 | 18 |
| Computer Knowledge | Software | `MEDIUM` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| Computer Knowledge | Internet and E-mail | `EASY` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| Computer Knowledge | Networking and Cyber Security | `MEDIUM` | 4 | **0** | ⛔ **-4** | 12 | 12 |

### 📋 State PSC Combined Prelims (General Studies - 150 Qs)
- **Total Rules**: 8 | **Questions Needed Per Exam**: 150
- **Status**: ❌ **INSUFFICIENT** (8 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| General Studies | Indian History | `HARD` | 25 | **0** | ⛔ **-25** | 75 | 75 |
| General Studies | Indian Geography | `MEDIUM` | 25 | **0** | ⛔ **-25** | 75 | 75 |
| General Studies | Indian Polity | `HARD` | 25 | **0** | ⛔ **-25** | 75 | 75 |
| General Studies | Indian Economy | `MEDIUM` | 20 | **0** | ⛔ **-20** | 60 | 60 |
| General Studies | Science & Technology | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| General Studies | Environment | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| General Studies | Current Affairs | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| General Intelligence and Reasoning | Problem Solving | `MEDIUM` | 10 | **8** | ⛔ **-2** | 30 | 22 |

### 📋 CTET / TET Paper-1 (Primary Teacher - 150 Qs)
- **Total Rules**: 16 | **Questions Needed Per Exam**: 150
- **Status**: ❌ **INSUFFICIENT** (15 starved rule(s))

| Subject | Topic | Difficulty | Needed | Available in DB | Immediate Deficit | 3x Target Buffer | 3x Buffer Need |
|---|---|---|---|---|---|---|---|
| Child Development and Pedagogy | Child Development & Learning Principles | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| Child Development and Pedagogy | Inclusive Education & Special Needs | `MEDIUM` | 8 | **0** | ⛔ **-8** | 24 | 24 |
| Child Development and Pedagogy | Assessment, Evaluation & CCE | `HARD` | 7 | **0** | ⛔ **-7** | 21 | 21 |
| English Language and Comprehension | Comprehension Passage | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| English Language and Comprehension | Grammar | `EASY` | 10 | **0** | ⛔ **-10** | 30 | 30 |
| English Language and Comprehension | Vocabulary | `EASY` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| General Hindi | αñàαñ¬αñáαñ┐αññ αñùαñªαÑìαñ»αñ╛αñéαñ╢ | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| General Hindi | αñ╕αñéαñºαñ┐ αñÅαñ╡αñé αñ╕αñéαñºαñ┐ αñ╡αñ┐αñÜαÑìαñ¢αÑçαñª | `MEDIUM` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| General Hindi | αñ╕αñ«αñ╛αñ╕ αñÅαñ╡αñé αñ╕αñ«αñ╛αñ╕ αñ╡αñ┐αñùαÑìαñ░αñ╣ | `EASY` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| General Hindi | αñ╡αñ╛αñòαÑìαñ» αñ╢αÑüαñªαÑìαñºαñ┐ αñÅαñ╡αñé αññαÑìαñ░αÑüαñƒαñ┐ αñ¬αñ╣αñÜαñ╛αñ¿ | `MEDIUM` | 5 | **0** | ⛔ **-5** | 15 | 15 |
| Mathematics | Arithmetic | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| Mathematics | Geometry | `EASY` | 8 | **0** | ⛔ **-8** | 24 | 24 |
| Mathematics | Mensuration | `MEDIUM` | 7 | **0** | ⛔ **-7** | 21 | 21 |
| General Studies | Environment | `MEDIUM` | 15 | **0** | ⛔ **-15** | 45 | 45 |
| General Studies | Science & Technology | `EASY` | 10 | **0** | ⛔ **-10** | 30 | 30 |

---

## 3. Global Question Generation Target Plan (By Subject & Topic)

The table below details the exact number of questions required per category to eliminate all blockers and build a resilient 3x multi-shift rotation inventory buffer.

| Subject | Topic | Difficulty | Current In DB | Max Needed (1 Shift) | Immediate Shortage | Recommended Generation (3x Buffer) | Impacted Blueprints |
|---|---|---|---|---|---|---|---|
| English Language and Comprehension | Comprehension Passage | `HARD` | 0 | 28 | **-28** (BLOCKER) | **+84** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs)<br>• UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) |
| General Studies | Indian History | `HARD` | 0 | 25 | **-25** (BLOCKER) | **+75** | • State PSC Combined Prelims (General Studies - 150 Qs)<br>• UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| General Studies | Indian Polity | `HARD` | 0 | 25 | **-25** (BLOCKER) | **+75** | • State PSC Combined Prelims (General Studies - 150 Qs)<br>• UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| General Studies | Indian Geography | `MEDIUM` | 0 | 25 | **-25** (BLOCKER) | **+75** | • State PSC Combined Prelims (General Studies - 150 Qs)<br>• UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| General Studies | Indian Economy | `MEDIUM` | 0 | 20 | **-20** (BLOCKER) | **+60** | • State PSC Combined Prelims (General Studies - 150 Qs) |
| General Studies | Indian Economy | `HARD` | 0 | 16 | **-16** (BLOCKER) | **+48** | • UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| Mathematics | Arithmetic | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs)<br>• RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| General Studies | Science & Technology | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • State PSC Combined Prelims (General Studies - 150 Qs)<br>• UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| General Studies | Current Affairs | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • State PSC Combined Prelims (General Studies - 150 Qs)<br>• UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| General Studies | Environment | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs)<br>• State PSC Combined Prelims (General Studies - 150 Qs) |
| Child Development and Pedagogy | Child Development & Learning Principles | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| English Language and Comprehension | Comprehension Passage | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| General Hindi | αñàαñ¬αñáαñ┐αññ αñùαñªαÑìαñ»αñ╛αñéαñ╢ | `MEDIUM` | 0 | 15 | **-15** (BLOCKER) | **+45** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| General Studies | Environment | `HARD` | 0 | 12 | **-12** (BLOCKER) | **+36** | • UPSC CSE Prelims Paper-1 (General Studies - 100 Qs) |
| English Language and Comprehension | Grammar | `EASY` | 0 | 10 | **-10** (BLOCKER) | **+30** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| General Studies | Science & Technology | `EASY` | 0 | 10 | **-10** (BLOCKER) | **+30** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| English Language and Comprehension | Cloze Passage | `MEDIUM` | 1 | 10 | **-9** (BLOCKER) | **+29** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Statistics and Probability | `HARD` | 1 | 10 | **-9** (BLOCKER) | **+29** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs) |
| General Intelligence and Reasoning | Problem Solving | `HARD` | 1 | 10 | **-9** (BLOCKER) | **+29** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| Child Development and Pedagogy | Inclusive Education & Special Needs | `MEDIUM` | 0 | 8 | **-8** (BLOCKER) | **+24** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| Mathematics | Geometry | `EASY` | 0 | 8 | **-8** (BLOCKER) | **+24** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| Child Development and Pedagogy | Assessment, Evaluation & CCE | `HARD` | 0 | 7 | **-7** (BLOCKER) | **+21** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| Mathematics | Mensuration | `MEDIUM` | 0 | 7 | **-7** (BLOCKER) | **+21** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| Mathematics | Algebra | `MEDIUM` | 0 | 6 | **-6** (BLOCKER) | **+18** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| Computer Knowledge | Computer Basics | `EASY` | 0 | 6 | **-6** (BLOCKER) | **+18** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| English Language and Comprehension | Sentence Shuffling | `MEDIUM` | 0 | 5 | **-5** (BLOCKER) | **+15** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs) |
| English Language and Comprehension | Vocabulary | `EASY` | 0 | 5 | **-5** (BLOCKER) | **+15** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• CTET / TET Paper-1 (Primary Teacher - 150 Qs)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Drawing Inferences | `MEDIUM` | 0 | 5 | **-5** (BLOCKER) | **+15** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs) |
| Computer Knowledge | Software | `MEDIUM` | 0 | 5 | **-5** (BLOCKER) | **+15** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| Computer Knowledge | Internet and E-mail | `EASY` | 0 | 5 | **-5** (BLOCKER) | **+15** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Hindi | αñ╕αñéαñºαñ┐ αñÅαñ╡αñé αñ╕αñéαñºαñ┐ αñ╡αñ┐αñÜαÑìαñ¢αÑçαñª | `MEDIUM` | 0 | 5 | **-5** (BLOCKER) | **+15** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| General Hindi | αñ╕αñ«αñ╛αñ╕ αñÅαñ╡αñé αñ╕αñ«αñ╛αñ╕ αñ╡αñ┐αñùαÑìαñ░αñ╣ | `EASY` | 0 | 5 | **-5** (BLOCKER) | **+15** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| General Hindi | αñ╡αñ╛αñòαÑìαñ» αñ╢αÑüαñªαÑìαñºαñ┐ αñÅαñ╡αñé αññαÑìαñ░αÑüαñƒαñ┐ αñ¬αñ╣αñÜαñ╛αñ¿ | `MEDIUM` | 0 | 5 | **-5** (BLOCKER) | **+15** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs) |
| General Intelligence and Reasoning | Coding and Decoding | `EASY` | 1 | 5 | **-4** (BLOCKER) | **+14** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| English Language and Comprehension | Active and Passive Voice | `MEDIUM` | 1 | 5 | **-4** (BLOCKER) | **+14** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| English Language and Comprehension | Direct and Indirect Narration | `MEDIUM` | 1 | 5 | **-4** (BLOCKER) | **+14** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Awareness | Economic Scene | `EASY` | 0 | 4 | **-4** (BLOCKER) | **+12** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| General Awareness | India and Neighbouring Countries | `MEDIUM` | 0 | 4 | **-4** (BLOCKER) | **+12** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| Mathematics | Geometry | `HARD` | 0 | 4 | **-4** (BLOCKER) | **+12** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| Mathematics | Trigonometry | `MEDIUM` | 0 | 4 | **-4** (BLOCKER) | **+12** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| Mathematics | Statistics | `EASY` | 0 | 4 | **-4** (BLOCKER) | **+12** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs) |
| Computer Knowledge | Networking and Cyber Security | `MEDIUM` | 0 | 4 | **-4** (BLOCKER) | **+12** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Critical Thinking | `HARD` | 7 | 10 | **-3** (BLOCKER) | **+23** | • SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs)<br>• UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Algebra | `HARD` | 2 | 5 | **-3** (BLOCKER) | **+13** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Trigonometry | `MEDIUM` | 1 | 4 | **-3** (BLOCKER) | **+11** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Numerical Operations | `MEDIUM` | 1 | 4 | **-3** (BLOCKER) | **+11** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Problem Solving | `MEDIUM` | 8 | 10 | **-2** (BLOCKER) | **+22** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• State PSC Combined Prelims (General Studies - 150 Qs)<br>• UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Number Systems | `HARD` | 4 | 6 | **-2** (BLOCKER) | **+14** | • UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Mensuration | `HARD` | 2 | 4 | **-2** (BLOCKER) | **+10** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Awareness | Scientific Research | `MEDIUM` | 1 | 3 | **-2** (BLOCKER) | **+8** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Awareness | Current Events | `MEDIUM` | 10 | 10 | 0 (Satisfied) | **+20** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Awareness | Everyday Science | `EASY` | 10 | 10 | 0 (Satisfied) | **+20** | • CTET / TET Paper-1 (Primary Teacher - 150 Qs)<br>• RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard) |
| Quantitative Aptitude / Mathematical Abilities | Fundamental Arithmetical Operations | `HARD` | 12 | 10 | 0 (Satisfied) | **+18** | • SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Statistics and Probability | `MEDIUM` | 7 | 6 | 0 (Satisfied) | **+11** | • SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs)<br>• UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) |
| General Intelligence and Reasoning | Venn Diagrams | `EASY` | 5 | 5 | 0 (Satisfied) | **+10** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard) |
| General Awareness | Geography | `MEDIUM` | 9 | 6 | 0 (Satisfied) | **+9** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Syllogistic Reasoning | `MEDIUM` | 7 | 5 | 0 (Satisfied) | **+8** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Series | `MEDIUM` | 8 | 5 | 0 (Satisfied) | **+7** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard) |
| General Awareness | History | `MEDIUM` | 11 | 6 | 0 (Satisfied) | **+7** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| English Language and Comprehension | Spot the Error | `MEDIUM` | 8 | 5 | 0 (Satisfied) | **+7** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Intelligence and Reasoning | Analogy | `EASY` | 9 | 5 | 0 (Satisfied) | **+6** | • RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| General Awareness | Economic Scene | `MEDIUM` | 7 | 4 | 0 (Satisfied) | **+5** | • SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| English Language and Comprehension | Synonyms and Homonyms | `MEDIUM` | 6 | 3 | 0 (Satisfied) | **+3** | • SSC CGL Tier-1 (100 Questions - Standard) |
| English Language and Comprehension | Cloze Passage | `HARD` | 6 | 3 | 0 (Satisfied) | **+3** | • SSC CGL Tier-1 (100 Questions - Standard) |
| General Intelligence and Reasoning | Classification | `EASY` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| General Intelligence and Reasoning | Coding and Decoding | `MEDIUM` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| General Intelligence and Reasoning | Numerical Operations | `EASY` | 4 | 2 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| General Awareness | Culture | `EASY` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| Quantitative Aptitude / Mathematical Abilities | Algebra | `MEDIUM` | 13 | 5 | 0 (Satisfied) | **+2** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard) |
| Quantitative Aptitude / Mathematical Abilities | Geometry | `HARD` | 13 | 5 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| English Language and Comprehension | Antonyms | `MEDIUM` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| English Language and Comprehension | Idioms and Phrases | `MEDIUM` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| English Language and Comprehension | One Word Substitution | `EASY` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| English Language and Comprehension | Improvement of Sentences | `MEDIUM` | 7 | 3 | 0 (Satisfied) | **+2** | • SSC CGL Tier-1 (100 Questions - Standard) |
| General Awareness | General Policy | `MEDIUM` | 8 | 3 | 0 (Satisfied) | **+1** | • SSC CGL Tier-1 (100 Questions - Standard)<br>• SSC CGL Tier-2 (Paper-1 Core - 150 Qs) |
| Quantitative Aptitude / Mathematical Abilities | Fundamental Arithmetical Operations | `MEDIUM` | 44 | 15 | 0 (Satisfied) | **+1** | • Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)<br>• SSC CGL Tier-1 (100 Questions - Standard)<br>• UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs) |
| English Language and Comprehension | Spellings | `EASY` | 5 | 2 | 0 (Satisfied) | **+1** | • SSC CGL Tier-1 (100 Questions - Standard) |

---

## 4. Subject-Level Inventory Summary

| Subject | Current Active Questions | Immediate Deficit | Recommended Total Generation (3x Buffer) |
|---|---|---|---|
| **General Studies** | 0 | **178** | **+534** |
| **English Language and Comprehension** | 64 | **80** | **+268** |
| **Mathematics** | 0 | **48** | **+144** |
| **Child Development and Pedagogy** | 0 | **30** | **+90** |
| **General Hindi** | 0 | **30** | **+90** |
| **General Intelligence and Reasoning** | 115 | **26** | **+151** |
| **Computer Knowledge** | 0 | **20** | **+60** |
| **Quantitative Aptitude / Mathematical Abilities** | 138 | **19** | **+111** |
| **General Awareness** | 63 | **10** | **+96** |
