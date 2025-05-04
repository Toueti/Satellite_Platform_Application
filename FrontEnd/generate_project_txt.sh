#!/bin/bash

# Script to write all code files from a Next.js project into a single text file
# Includes file name and relative path before each file's code

# Define the output file
OUTPUT_FILE="project_codebase.txt"

# Clear the output file if it exists
> "$OUTPUT_FILE"

# Define directories and files to exclude
EXCLUDE_DIRS="node_modules .next .git out"
EXCLUDE_FILES="*.log *.lock *.md *.txt *.json *.lock *.yaml *.yml *.gitignore *.env* *.png *.jpg *.jpeg *.gif *.bmp *.svg *.webp"

# Function to check if a file should be included
should_include_file() {
    local file="$1"
    # Check if file matches excluded patterns
    for pattern in $EXCLUDE_FILES; do
        if [[ "$file" == *"$pattern" ]]; then
            return 1
        fi
    done
    return 0
}

# Find all files, excluding specified directories
find . -type f | while read -r file; do
    # Skip files in excluded directories
    skip=false
    for dir in $EXCLUDE_DIRS; do
        if [[ "$file" == *"$dir"* ]]; then
            skip=true
            break
        fi
    done

    # Process file if not skipped
    if [ "$skip" = false ] && should_include_file "$file"; then
        # Get the file name and relative path
        relative_path="$file"
        file_name=$(basename "$file")
        echo "===== File: $file_name, Path: $relative_path =====" >> "$OUTPUT_FILE"
        cat "$file" >> "$OUTPUT_FILE"
        echo -e "\n\n" >> "$OUTPUT_FILE"
    fi
done

echo "Codebase has been written to $OUTPUT_FILE"