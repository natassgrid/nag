#!/usr/bin/env python3
"""
Splits PMD CPD (Copy-Paste Detector) report into individual problem files
and generates a summary index for analysis and refactoring prioritization.
"""

import os
import re
import sys
import json
import argparse
from pathlib import Path

SEPARATOR = "====================================================================="

def parse_header(header_lines):
    lines_count = 0
    tokens_count = 0
    files = []

    dup_match = re.search(r"Found a (\d+) line \((\d+) tokens\) duplication", header_lines[0])
    if dup_match:
        lines_count = int(dup_match.group(1))
        tokens_count = int(dup_match.group(2))

    for line in header_lines[1:]:
        file_match = re.search(r"Starting at line (\d+) of (.+)", line.strip())
        if file_match:
            line_no = int(file_match.group(1))
            filepath = file_match.group(2).strip()
            files.append({"line": line_no, "file": filepath})

    return lines_count, tokens_count, files

def split_cpd_report(report_path, output_dir):
    report_file = Path(report_path)
    if not report_file.exists():
        print(f"Error: Report file not found at {report_path}", file=sys.stderr)
        sys.exit(1)

    out_path = Path(output_dir)
    out_path.mkdir(parents=True, exist_ok=True)

    print(f"Reading {report_path}...")
    with open(report_file, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()

    blocks = content.split(SEPARATOR)
    problems = []

    print(f"Found {len(blocks)} sections. Processing...")

    for idx, block in enumerate(blocks, start=1):
        block_str = block.strip()
        if not block_str:
            continue

        lines = block_str.splitlines()
        header_lines = []
        code_lines = []
        in_header = True

        for line in lines:
            if in_header:
                if line.startswith("Found a ") or line.startswith("Starting at line ") or line.strip() == "" or "duplication in the following files:" in line:
                    if line.strip():
                        header_lines.append(line)
                else:
                    in_header = False
                    code_lines.append(line)
            else:
                code_lines.append(line)

        lines_count, tokens_count, files = parse_header(header_lines)

        problem_filename = f"problem_{idx:04d}.txt"
        problem_filepath = out_path / problem_filename

        with open(problem_filepath, "w", encoding="utf-8") as pf:
            pf.write(block_str + "\n")

        problems.append({
            "id": idx,
            "filename": problem_filename,
            "lines": lines_count,
            "tokens": tokens_count,
            "file_count": len(files),
            "files": files,
            "preview": "\n".join(code_lines[:5]) if code_lines else ""
        })

    # Sort problems by tokens (descending), then lines (descending)
    ranked_problems = sorted(problems, key=lambda p: (p["tokens"], p["lines"]), reverse=True)

    index_file = out_path / "index.json"
    with open(index_file, "w", encoding="utf-8") as jf:
        json.dump({
            "total_problems": len(problems),
            "problems": problems,
            "top_by_tokens": [p["id"] for p in ranked_problems[:100]]
        }, jf, indent=2)

    summary_file = out_path / "summary.md"
    with open(summary_file, "w", encoding="utf-8") as sf:
        sf.write("# CPD Duplication Summary\n\n")
        sf.write(f"Total problems split: {len(problems)}\n\n")
        sf.write("## Top 30 Duplications by Token Count\n\n")
        sf.write("| ID | Tokens | Lines | Files Count | First File |\n")
        sf.write("|---|---|---|---|---|\n")
        for p in ranked_problems[:30]:
            first_file = Path(p["files"][0]["file"]).name if p["files"] else "unknown"
            sf.write(f"| [{p['filename']}]({p['filename']}) | {p['tokens']} | {p['lines']} | {p['file_count']} | `{first_file}` |\n")

    print(f"Successfully split {len(problems)} problems into {out_path}")
    print(f"Index written to {index_file}")
    print(f"Summary written to {summary_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Split PMD CPD report into smaller problem files.")
    parser.add_argument(
        "--input", "-i",
        default="F:/code/IdeaProjects/nag/build/reports/cpd/cpd-report.txt",
        help="Path to cpd-report.txt"
    )
    parser.add_argument(
        "--output-dir", "-o",
        default="F:/code/IdeaProjects/nag/build/reports/cpd/problems",
        help="Directory to save split problem files"
    )
    args = parser.parse_args()
    split_cpd_report(args.input, args.output_dir)
