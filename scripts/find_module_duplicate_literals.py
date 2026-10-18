#!/usr/bin/env python3
"""
Scans backend Java source files for a specific module (or all modules),
identifies String literals duplicated within the SAME module (>= min_count),
and can generate a Java Constants class in the module's constants package.

Production code only: strictly excludes `src/test` and `build/`.
"""

import os
import re
import sys
import json
import argparse
from pathlib import Path
from collections import defaultdict

# Regex to strip single and multi-line comments while preserving newlines
COMMENT_REGEX = re.compile(
    r'(?://[^\n]*)|(?:/\*.*?\*/)',
    re.DOTALL
)

# Regex to match Java string literals with escape sequences
STRING_LITERAL_REGEX = re.compile(
    r'"((?:\\.|[^"\\])*)"'
)

# Skip annotations like @SuppressWarnings("...")
SUPPRESS_WARNINGS_REGEX = re.compile(
    r'@SuppressWarnings\s*\(\s*(?:\{[^}]*\}|"(?:\\.|[^"\\])*")\s*\)',
    re.DOTALL
)

PACKAGE_REGEX = re.compile(r'package\s+([\w\.]+)\s*;')
CLASS_REGEX = re.compile(r'\b(?:class|interface|enum|record)\s+([A-Za-z0-9_]+)\b')


def extract_literals_from_file(file_path: Path, min_length: int = 3):
    try:
        content = file_path.read_text(encoding="utf-8", errors="replace")
    except Exception as e:
        print(f"Warning: could not read {file_path}: {e}", file=sys.stderr)
        return []

    pkg_match = PACKAGE_REGEX.search(content)
    pkg = pkg_match.group(1) if pkg_match else "default"

    class_match = CLASS_REGEX.search(content)
    class_name = class_match.group(1) if class_match else file_path.stem
    full_class = f"{pkg}.{class_name}"

    cleaned_code = SUPPRESS_WARNINGS_REGEX.sub('', content)

    def comment_replacer(match):
        return '\n' * match.group(0).count('\n')

    code_no_comments = COMMENT_REGEX.sub(comment_replacer, cleaned_code)

    results = []
    lines = code_no_comments.split('\n')
    for line_idx, line in enumerate(lines, start=1):
        for match in STRING_LITERAL_REGEX.finditer(line):
            raw_val = match.group(1)
            # Unescape basic escapes
            val = raw_val.replace(r'\"', '"').replace(r'\\', '\\').replace(r'\n', '\n').replace(r'\t', '\t')

            # Filter trivial strings (like single chars, empty, placeholders)
            stripped = val.strip()
            if len(stripped) < min_length or stripped in {"{}", "%s", "%d", "", ",", ";", ":", "-", "_"}:
                continue

            results.append({
                "value": val,
                "line": line_idx,
                "class": full_class,
                "simple_class": class_name,
                "file": str(file_path).replace("\\", "/"),
                "package": pkg
            })

    return results


def scan_module_sources(module_dir: Path, min_length: int = 3):
    src_main_java = module_dir / "src" / "main" / "java"
    if not src_main_java.exists():
        return []

    occurrences = []
    for root, dirs, files in os.walk(src_main_java):
        p = Path(root)
        if "test" in p.parts or "build" in p.parts or "target" in p.parts:
            continue
        for file in files:
            if file.endswith(".java"):
                file_path = p / file
                occurrences.extend(extract_literals_from_file(file_path, min_length))
    return occurrences


def detect_base_package(occurrences: list) -> str:
    packages = [it["package"] for it in occurrences if it.get("package") and it["package"] != "default"]
    if not packages:
        return "com.examplatform"
    # Find longest common package prefix
    common_parts = packages[0].split('.')
    for p in packages[1:]:
        parts = p.split('.')
        common_parts = [cp for cp, pp in zip(common_parts, parts) if cp == pp]
        if not common_parts:
            break
    return '.'.join(common_parts) if common_parts else "com.examplatform"


def suggest_constant_name(literal: str) -> str:
    cleaned = literal.strip()

    # Prepend contextual prefixes if recognizable
    prefix = ""
    if cleaned.startswith("exam.") or cleaned.startswith("notifications.") or "topic" in cleaned.lower():
        prefix = "TOPIC_"
    elif cleaned in {"type", "comments", "assignedReviewer", "assignedReviewer2", "assignedReviewers",
                     "dualReviewRequired", "escalatedToController", "publisherId", "toState", "fromState"}:
        prefix = "KEY_"
    elif cleaned.startswith("QUESTION_") or cleaned in {"SUBMITTED_FOR_REVIEW", "REVIEWER_APPROVED", "RETURNED_TO_DRAFT"}:
        prefix = "EVENT_"
    elif cleaned.startswith("question."):
        prefix = "CONFIG_"

    sanitized = re.sub(r'[^A-Za-z0-9]+', '_', cleaned)
    sanitized = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', sanitized)
    sanitized = re.sub(r'_+', '_', sanitized).strip('_').upper()

    if not sanitized:
        sanitized = "CONST"
    if sanitized[0].isdigit():
        sanitized = "CONST_" + sanitized

    full_name = f"{prefix}{sanitized}" if prefix and not sanitized.startswith(prefix) else sanitized
    return full_name[:50]


def generate_java_constants_class(package_name: str, class_name: str, duplicates: list) -> str:
    lines = [
        "/*",
        " * SPDX-License-Identifier: AGPL-3.0-only",
        " *",
        " * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform",
        " * Copyright (C) 2026 NAG Contributors",
        " *",
        " * This program is free software: you can redistribute it and/or modify",
        " * it under the terms of the GNU Affero General Public License as published",
        " * by the Free Software Foundation, version 3 of the License.",
        " *",
        " * This program is distributed in the hope that it will be useful,",
        " * but WITHOUT ANY WARRANTY; without even the implied warranty of",
        " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the",
        " * GNU Affero General Public License for more details.",
        " *",
        " * You should have received a copy of the GNU Affero General Public License",
        " * along with this program. If not, see <https://www.gnu.org/licenses/>.",
        " */",
        "",
        f"package {package_name};",
        "",
        "/**",
        f" * Constants used across question bank module components.",
        " * Consolidated duplicate string literals.",
        " */",
        f"public final class {class_name} {{",
        "",
        f"    private {class_name}() {{",
        "        // Utility class, prevent instantiation",
        "    }",
        ""
    ]

    seen_names = set()
    for item in duplicates:
        val = item["literal"]
        cname = item["constant_name_suggestion"]
        if cname in seen_names:
            idx = 2
            while f"{cname}_{idx}" in seen_names:
                idx += 1
            cname = f"{cname}_{idx}"
        seen_names.add(cname)

        escaped = val.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")
        count = item["total_count"]
        class_count = item["class_count"]
        lines.append(f'    public static final String {cname} = "{escaped}"; // {count} occurrences across {class_count} classes')

    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Find String literals duplicated within the same module.")
    parser.add_argument("--repo-root", type=str, default=".", help="Root path of repository")
    parser.add_argument("--module", type=str, default=None, help="Specific module to analyze (e.g. question-bank-service)")
    parser.add_argument("--file", type=str, default=None, help="Specific file to focus on within the module")
    parser.add_argument("--min-length", type=int, default=3, help="Minimum string literal length (default: 3)")
    parser.add_argument("--min-count", type=int, default=2, help="Minimum occurrences in module (default: 2)")
    parser.add_argument("--output-json", type=str, default=None, help="Output JSON path")
    parser.add_argument("--output-txt", type=str, default=None, help="Output text summary path")
    parser.add_argument("--generate-java", action="store_true", help="Generate Java constants class file")
    parser.add_argument("--package-name", type=str, default=None, help="Java package for generated constants")
    parser.add_argument("--class-name", type=str, default=None, help="Java class name for generated constants")
    parser.add_argument("--output-java", type=str, default=None, help="Target file path for Java constants class")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    backend_dir = repo_root / "backend"

    if args.file:
        file_path = Path(args.file).resolve()
        try:
            rel = file_path.relative_to(backend_dir)
            mod_name = rel.parts[0]
            target_modules = [backend_dir / mod_name]
        except Exception:
            target_modules = [backend_dir / args.module] if args.module else []
    elif args.module:
        target_modules = [backend_dir / args.module]
    else:
        target_modules = [p for p in backend_dir.iterdir() if p.is_dir() and (p / "src" / "main" / "java").exists()]

    for mod_dir in target_modules:
        mod_name = mod_dir.name
        print(f"\n=======================================================")
        print(f"Scanning module: {mod_name}")
        print(f"=======================================================")

        occurrences = scan_module_sources(mod_dir, min_length=args.min_length)
        print(f"Total String literal occurrences extracted: {len(occurrences)}")

        base_pkg = detect_base_package(occurrences)

        grouped = defaultdict(list)
        for it in occurrences:
            grouped[it["value"]].append(it)

        # If a specific file was requested, filter to literals present in that file
        file_literals = None
        if args.file:
            target_file_path = Path(args.file).resolve()
            f_occurrences = extract_literals_from_file(target_file_path, min_length=args.min_length)
            file_literals = set(it["value"] for it in f_occurrences)
            print(f"Focusing on {len(file_literals)} distinct literals present in {target_file_path.name}")

        duplicates = []
        for val, items in grouped.items():
            if file_literals is not None and val not in file_literals:
                continue
            if len(items) >= args.min_count:
                classes = sorted(list(set(it["class"] for it in items)))
                by_class = defaultdict(list)
                for it in items:
                    by_class[it["class"]].append(it["line"])

                duplicates.append({
                    "literal": val,
                    "total_count": len(items),
                    "class_count": len(classes),
                    "classes": classes,
                    "occurrences_by_class": dict(by_class),
                    "constant_name_suggestion": suggest_constant_name(val)
                })

        duplicates.sort(key=lambda x: (x["class_count"], x["total_count"]), reverse=True)
        print(f"Found {len(duplicates)} duplicate literals repeated >= {args.min_count} times")

        # Display duplicates
        print("\nLiterals:")
        for d in duplicates[:30]:
            print(f"  [{d['total_count']}x across {d['class_count']} classes] \"{d['literal']}\" -> {d['constant_name_suggestion']}")

        if args.output_json:
            out_json = Path(args.output_json)
            out_json.parent.mkdir(parents=True, exist_ok=True)
            with open(out_json, "w", encoding="utf-8") as f:
                json.dump(duplicates, f, indent=2)
            print(f"\nJSON written to: {out_json}")

        if args.output_txt:
            out_txt = Path(args.output_txt)
            out_txt.parent.mkdir(parents=True, exist_ok=True)
            with open(out_txt, "w", encoding="utf-8") as f:
                f.write(f"Duplicate String Literals in Module: {mod_name}\n")
                f.write(f"Criteria: Min Length={args.min_length}, Min Count={args.min_count}\n\n")
                for d in duplicates:
                    f.write(f"Literal: \"{d['literal']}\"\n")
                    f.write(f"  Constant Suggestion: {d['constant_name_suggestion']}\n")
                    f.write(f"  Count: {d['total_count']} across {d['class_count']} classes\n")
                    for cls, lines in d["occurrences_by_class"].items():
                        f.write(f"    * {cls}: lines {lines}\n")
                    f.write("\n")
            print(f"Text report written to: {out_txt}")

        if args.generate_java:
            pkg_name = args.package_name or f"{base_pkg}.constants"
            cls_name = args.class_name or f"{''.join(w.capitalize() for w in mod_name.split('-'))}Constants"

            java_code = generate_java_constants_class(pkg_name, cls_name, duplicates)

            if args.output_java:
                out_java = Path(args.output_java)
            else:
                out_java = mod_dir / "src" / "main" / "java" / Path(*pkg_name.split('.')) / f"{cls_name}.java"

            out_java.parent.mkdir(parents=True, exist_ok=True)
            out_java.write_text(java_code, encoding="utf-8")
            print(f"\nGenerated Java Constants Class: {out_java}")


if __name__ == "__main__":
    main()
