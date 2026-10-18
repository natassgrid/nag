#!/usr/bin/env python3
import json
from pathlib import Path

report_path = Path("build/reports/module_duplicates_question_bank.json")
if not report_path.exists():
    print("Report not found.")
    exit(0)

with open(report_path, "r", encoding="utf-8") as f:
    dups = json.load(f)

print(f"Total duplicate literals found in question-bank-service: {len(dups)}\n")

categories = {
    "Security Expressions (@PreAuthorize)": [],
    "REST Endpoints & Route Mappings": [],
    "Domain Field Names & Map Keys": [],
    "Validation & Error Messages": [],
    "Domain Enums & Canonical Values": [],
    "Logging & Tech Identifiers": []
}

for d in dups:
    lit = d["literal"]
    if any(k in lit for k in ["hasRole", "hasAnyRole", "permitAll", "isAuthenticated"]):
        categories["Security Expressions (@PreAuthorize)"].append(d)
    elif lit.startswith("/") or ("{" in lit and "}" in lit):
        categories["REST Endpoints & Route Mappings"].append(d)
    elif any(k in lit.lower() for k in ["required", "not found", "cannot", "must", "successfully", "failed"]):
        categories["Validation & Error Messages"].append(d)
    elif lit in {"TEXT", "JEE_MAIN", "MIXED", "KNOWLEDGE", "REMEMBERING", "RECALL", "MEMORY",
                 "COMPREHENSION", "UNDERSTANDING", "CONCEPTUAL", "APPLICATION", "APPLYING",
                 "ANALYSIS", "ANALYZING", "EVALUATION", "EVALUATING", "SYNTHESIS", "CREATING",
                 "CREATION", "BEGINNER", "SIMPLE", "INTERMEDIATE", "MODERATE", "NORMAL",
                 "ADVANCED", "EXPERT", "DIFFICULT", "MCQ", "MSQ", "ALL", "REJECTED"}:
        categories["Domain Enums & Canonical Values"].append(d)
    elif any(lit == k for k in [
        "topic", "state", "questionType", "subtopic", "cognitiveLevel", "chapter",
        "subjectId", "explanation", "comments", "answerKey", "subject_id", "topic_id",
        "topicId", "options", "author_id", "reviewer_id", "question_type", "tenant_id",
        "description", "actorId", "toState", "fromState", "isCorrect", "updatedAt",
        "translationId", "references", "subtopicId", "totalProcessed", "totalFailed",
        "content", "title", "has_images", "encryption_key_id", "benchmark_time_seconds",
        "question_id", "optionsJson", "manifest.json", "languageCode", "jobId",
        "target_lang", "translated_text", "recordId"
    ]):
        categories["Domain Field Names & Map Keys"].append(d)
    else:
        categories["Logging & Tech Identifiers"].append(d)

for cat_name, items in categories.items():
    print(f"=== {cat_name} ({len(items)} constants) ===")
    sorted_items = sorted(items, key=lambda x: (x["class_count"], x["total_count"]), reverse=True)
    for it in sorted_items[:8]:
        classes = [c.split(".")[-1] for c in it["classes"]]
        classes_str = ", ".join(classes[:3]) + (f" (+{len(classes)-3} more)" if len(classes) > 3 else "")
        print(f"  • \"{it['literal']}\" -> {it['constant_name_suggestion']}")
        print(f"      Count: {it['total_count']} occurrences across {it['class_count']} classes ({classes_str})")
    print()
