#!/bin/bash
# =============================================================================
# Add AGPL-3.0-only license headers to all project source files
# Also removes duplicate headers if the script was run multiple times.
# Usage: bash scripts/add-license-headers.sh
# =============================================================================

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# --- License headers by file type ---

read -r -d '' HEADER_BLOCK << 'EOF'
/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
EOF

read -r -d '' HEADER_HASH << 'EOF'
EOF

read -r -d '' HEADER_SQL << 'EOF'
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors
EOF

read -r -d '' HEADER_HTML << 'EOF'
<!--
  National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
  Copyright (C) 2025 NAG Contributors
-->
EOF

read -r -d '' HEADER_CSS << 'EOF'
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */
EOF

# --- Counters ---
count=0
skipped=0
deduped=0

# --- Check if file has SPDX header ---
has_spdx() {
}

# --- Count SPDX occurrences ---
spdx_count() {
    local n
    echo "${n:-0}"
}

# --- Remove duplicate headers using awk ---
remove_duplicates() {
    local file="$1"
    local style="$2"  # block, hash, sql, html, css

    local tmpfile="${file}.dedup.tmp"

    case "$style" in
        block)
            # Remove second+ occurrence of /* ... SPDX ... */
            awk '
            BEGIN { found=0; in_block=0 }
            /^\/\*/ {
                if (in_block == 0) { in_block=1; block="" }
            }
            in_block {
                block = block $0 "\n"
                if (/\*\//) {
                    in_block=0
                        found++
                        if (found == 1) { printf "%s", block }
                    } else {
                        printf "%s", block
                    }
                    block=""
                }
                next
            }
            { print }
            ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            ;;
        hash)
            awk '
            BEGIN { found=0; in_dup=0 }
                if (found == 0) { found=1; print; next }
                else { in_dup=1; next }
            }
            in_dup && /^#/ { next }
            in_dup && /^[[:space:]]*$/ { in_dup=0; next }
            in_dup { in_dup=0 }
            { print }
            ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            ;;
        sql)
            awk '
            BEGIN { found=0; in_dup=0 }
                if (found == 0) { found=1; print; next }
                else { in_dup=1; next }
            }
            in_dup && /^--/ { next }
            in_dup && /^[[:space:]]*$/ { in_dup=0; next }
            in_dup { in_dup=0 }
            { print }
            ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            ;;
        html)
            awk '
            BEGIN { found=0; in_dup=0 }
                if (found == 0) { found=1; print; next }
                else { in_dup=1; next }
            }
            in_dup && /-->/ { in_dup=0; next }
            in_dup { next }
            { print }
            ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            ;;
        css)
            awk '
            BEGIN { found=0; in_dup=0 }
                if (found == 0) { found=1; print; next }
                else { in_dup=1; next }
            }
            in_dup && /\*\// { in_dup=0; next }
            in_dup { next }
            { print }
            ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            ;;
    esac
}

# --- Prepend header to file ---
prepend_header() {
    local file="$1"
    local header="$2"

    local tmpfile="${file}.header.tmp"

    # Handle shebang lines
    if head -1 "$file" | grep -q "^#!"; then
        head -1 "$file" > "$tmpfile"
        echo "" >> "$tmpfile"
        echo "$header" >> "$tmpfile"
        echo "" >> "$tmpfile"
        tail -n +2 "$file" >> "$tmpfile"
    else
        echo "$header" > "$tmpfile"
        echo "" >> "$tmpfile"
        cat "$file" >> "$tmpfile"
    fi

    mv "$tmpfile" "$file"
}

# --- Main processing function ---
process_file() {
    local file="$1"
    local header="$2"
    local style="$3"
    local label="$4"

    # Skip empty or missing files
    if [ ! -s "$file" ]; then
        return
    fi

    local n
    n=$(spdx_count "$file")

    if [ "$n" -gt 1 ] 2>/dev/null; then
        remove_duplicates "$file" "$style"
        deduped=$((deduped + 1))
        echo "  [dedup] $(basename "$file")"
    elif [ "$n" -eq 1 ] 2>/dev/null; then
        skipped=$((skipped + 1))
    else
        prepend_header "$file" "$header"
        count=$((count + 1))
        echo "  [$label] $(basename "$file")"
    fi
}

# --- Process files ---
echo "Adding license headers to project files..."
echo "Project root: $PROJECT_ROOT"
echo ""

# Java files
find "$PROJECT_ROOT/backend" -name "*.java" -not -path "*/build/*" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_BLOCK" "block" "java"
done

# TypeScript files
find "$PROJECT_ROOT/frontend/src" -name "*.ts" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_BLOCK" "block" "ts"
done

# Gradle files
find "$PROJECT_ROOT" -name "*.gradle" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_BLOCK" "block" "gradle"
done

# YAML files (infrastructure)
find "$PROJECT_ROOT/infrastructure" \( -name "*.yml" -o -name "*.yaml" \) 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_HASH" "hash" "yaml"
done

# Application YAML files (backend)
find "$PROJECT_ROOT/backend" -name "application*.yml" -not -path "*/build/*" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_HASH" "hash" "yaml"
done

# Shell scripts
find "$PROJECT_ROOT" -name "*.sh" -not -path "*/node_modules/*" -not -path "*/.git/*" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_HASH" "hash" "sh"
done

# Properties / conf / ini
find "$PROJECT_ROOT/infrastructure" \( -name "*.properties" -o -name "*.conf" -o -name "*.ini" \) 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_HASH" "hash" "conf"
done

# SQL files
find "$PROJECT_ROOT" -name "*.sql" -not -path "*/node_modules/*" -not -path "*/build/*" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_SQL" "sql" "sql"
done

# HTML files
find "$PROJECT_ROOT/frontend/src" -name "*.html" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_HTML" "html" "html"
done

# SCSS files
find "$PROJECT_ROOT/frontend/src" -name "*.scss" 2>/dev/null | while read -r file; do
    process_file "$file" "$HEADER_CSS" "css" "scss"
done

echo ""
echo "============================================="
echo "  Done!"
echo "  Headers added: $count"
echo "  Duplicates fixed: $deduped"
echo "  Already correct (skipped): $skipped"
echo "============================================="
