#!/usr/bin/env python3
"""
Scans backend Java source files (excluding test code and build artifacts),
extracts String literals, and groups them by class and module.

Produces a structured report and generates proposed Java constant definitions
for com.examplatform.shared.constants.
"""

import os
import re
import sys
import json
import argparse
from pathlib import Path
from collections import defaultdict

# Regex to strip single and multi-line comments
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


def extract_literals_from_file(file_path: Path, min_length: int = 4):
    try:
        content = file_path.read_text(encoding="utf-8", errors="replace")
    except Exception as e:
        print(f"Warning: could not read {file_path}: {e}", file=sys.stderr)
        return []

    # Extract package and class name
    pkg_match = PACKAGE_REGEX.search(content)
    pkg = pkg_match.group(1) if pkg_match else "default"

    class_match = CLASS_REGEX.search(content)
    class_name = class_match.group(1) if class_match else file_path.stem
    full_class = f"{pkg}.{class_name}"

    # Remove @SuppressWarnings annotations before finding strings
    cleaned_code = SUPPRESS_WARNINGS_REGEX.sub('', content)

    # Remove comments while keeping line count aligned by replacing with newlines
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

            # Filter trivial or short strings
            if len(val.strip()) < min_length:
                continue

            results.append({
                "value": val,
                "line": line_idx,
                "class": full_class,
                "simple_class": class_name
            })

    return results


def find_all_literals(repo_root: Path, min_length: int = 4):
    backend_dir = repo_root / "backend"
    if not backend_dir.exists():
        print(f"Error: {backend_dir} not found", file=sys.stderr)
        sys.exit(1)

    all_occurrences = []

    for root, dirs, files in os.walk(backend_dir):
        # Ignore test directories and build output directories
        p = Path(root)
        parts = p.parts

        if "src" not in parts or "test" in parts or "build" in parts or "target" in parts or ".gradle" in parts:
            continue

        if "src" in parts:
            src_idx = parts.index("src")
            # Must be inside src/main/java
            if len(parts) > src_idx + 2 and parts[src_idx + 1] == "main" and parts[src_idx + 2] == "java":
                pass
            else:
                continue

        # Determine module name (relative to backend)
        try:
            rel_to_backend = p.relative_to(backend_dir)
            module_name = rel_to_backend.parts[0] if rel_to_backend.parts else "backend"
        except Exception:
            module_name = "unknown"

        for file in files:
            if file.endswith(".java"):
                file_path = p / file
                literals = extract_literals_from_file(file_path, min_length)
                for item in literals:
                    item["file"] = str(file_path.relative_to(repo_root)).replace("\\", "/")
                    item["module"] = module_name
                    all_occurrences.append(item)

    return all_occurrences


def format_constant_name(literal: str) -> str:
    # Convert string to UPPER_SNAKE_CASE
    cleaned = re.sub(r'[^A-Za-z0-9]+', '_', literal.strip())
    cleaned = re.sub(r'_+', '_', cleaned).strip('_')
    if not cleaned:
        cleaned = "CONSTANT"
    if cleaned[0].isdigit():
        cleaned = "CONST_" + cleaned
    return cleaned.upper()[:40]


def main():
    parser = argparse.ArgumentParser(description="Find and group Java String literals in backend sources.")
    parser.add_argument("--repo-root", type=str, default=".", help="Root path of the repository")
    parser.add_argument("--min-length", type=int, default=4, help="Minimum string literal length (default: 4)")
    parser.add_argument("--min-count", type=int, default=3, help="Minimum occurrence count (default: 3)")
    parser.add_argument("--cross-module-only", action="store_true", help="Filter for literals appearing in multiple modules")
    parser.add_argument("--output-json", type=str, default="build/reports/string_literals_grouped.json", help="Output JSON report path")
    parser.add_argument("--output-txt", type=str, default="build/reports/string_literals_report.txt", help="Output text summary path")
    parser.add_argument("--generate-constants", action="store_true", help="Generate Java constant proposals")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    print(f"Scanning backend sources in: {repo_root / 'backend'}")
    occurrences = find_all_literals(repo_root, min_length=args.min_length)
    print(f"Total String literal occurrences found: {len(occurrences)}")

    # Group by literal value
    grouped_by_value = defaultdict(list)
    for item in occurrences:
        grouped_by_value[item["value"]].append(item)

    # Filter by count and cross-module criteria
    filtered_items = []
    for val, items in grouped_by_value.items():
        if len(items) < args.min_count:
            continue
        modules = sorted(list(set(it["module"] for it in items)))
        classes = sorted(list(set(it["class"] for it in items)))

        if args.cross_module_only and len(modules) < 2:
            continue

        # Group occurrences by module -> class
        hierarchy = defaultdict(lambda: defaultdict(list))
        for it in items:
            hierarchy[it["module"]][it["class"]].append({
                "line": it["line"],
                "file": it["file"]
            })

        filtered_items.append({
            "literal": val,
            "total_count": len(items),
            "module_count": len(modules),
            "class_count": len(classes),
            "modules": modules,
            "classes": classes,
            "constant_name_suggestion": format_constant_name(val),
            "hierarchy": {m: dict(c) for m, c in hierarchy.items()}
        })

    # Sort descending by total_count, then module_count
    filtered_items.sort(key=lambda x: (x["module_count"], x["total_count"]), reverse=True)

    # Output JSON
    output_json_path = Path(args.output_json)
    output_json_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_json_path, "w", encoding="utf-8") as f:
        json.dump(filtered_items, f, indent=2)
    print(f"JSON report written to: {output_json_path}")

    # Output text summary
    output_txt_path = Path(args.output_txt)
    output_txt_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_txt_path, "w", encoding="utf-8") as f:
        f.write(f"String Literals Analysis Report (Min Length: {args.min_length}, Min Count: {args.min_count})\n")
        f.write(f"Total Unique Filtered Literals: {len(filtered_items)}\n")
        f.write("=" * 80 + "\n\n")

        for entry in filtered_items:
            f.write(f"Literal: \"{entry['literal']}\"\n")
            f.write(f"  Suggested Constant: {entry['constant_name_suggestion']}\n")
            f.write(f"  Occurrences: {entry['total_count']} across {entry['class_count']} classes in {entry['module_count']} module(s): {', '.join(entry['modules'])}\n")
            f.write("  Breakdown by Module & Class:\n")
            for mod, classes_map in entry["hierarchy"].items():
                f.write(f"    - Module [{mod}]:\n")
                for cls, locs in classes_map.items():
                    line_strs = [str(l["line"]) for l in locs]
                    f.write(f"        * {cls} (lines: {', '.join(line_strs)})\n")
            f.write("\n")

    print(f"Text report written to: {output_txt_path}")

    if args.generate_constants:
        print("\nProposed Constants for shared-lib (com.examplatform.shared.constants):\n")
        print("public final class SystemConstants {")
        print("    private SystemConstants() {}")
        seen_names = set()
        for entry in filtered_items[:50]:  # Top 50
            name = entry["constant_name_suggestion"]
            if name in seen_names:
                suffix = 2
                while f"{name}_{suffix}" in seen_names:
                    suffix += 1
                name = f"{name}_{suffix}"
            seen_names.add(name)
            # Escape value for Java string
            escaped_val = entry["literal"].replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")
            print(f'    public static final String {name} = "{escaped_val}"; // {entry["total_count"]} usages across {entry["module_count"]} modules')
        print("}")


if __name__ == "__main__":
    main()
