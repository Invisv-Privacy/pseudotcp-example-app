#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Define colors for better output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ANDROID_API_VERSION=33
OUTPUT_DIR="../../libs"
OUTPUT_FILE="bindings.aar"

# Check if gomobile is installed
if ! command -v gomobile &> /dev/null; then
    echo -e "${YELLOW}Error: gomobile is not installed.${NC}"
    echo -e "Please install it with: ${BLUE}go install golang.org/x/mobile/cmd/gomobile@latest${NC}"
    echo -e "Then initialize it with: ${BLUE}gomobile init${NC}"
    exit 1
fi

go get golang.org/x/mobile/bind

echo -e "${BLUE}=== Building Go code with gomobile ===${NC}"
echo -e "${GREEN}Target:${NC} Android API $ANDROID_API_VERSION"
echo -e "${GREEN}Output:${NC} $OUTPUT_FILE"

# Run gomobile bind
echo -e "${BLUE}Running gomobile bind...${NC}"
gomobile bind -target android -androidapi $ANDROID_API_VERSION -ldflags=-extldflags=-Wl,-soname,libgojni.so

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Copy the output file
cp "$OUTPUT_FILE" "$OUTPUT_DIR/"

echo -e "${GREEN}✓ Successfully built and moved $OUTPUT_FILE to Android libs.${NC}"
echo -e "${YELLOW}IMPORTANT:${NC} Remember to sync project with Gradle files in Android Studio!"
echo -e "You can do this by clicking ${BLUE}'Sync Project with Gradle Files'${NC} in Android Studio."
