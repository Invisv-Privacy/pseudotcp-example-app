#!/bin/sh

set -e

ANDROID_API_VERSION=33

echo 'Running gomobile bind...'

gomobile bind -target android -androidapi $ANDROID_API_VERSION -ldflags=-extldflags=-Wl,-soname,libgojni.so

mkdir -p ../../libs && cp bindings.aar ../../libs/
echo 'Moved .aar to Android libs. REMEMBER to sync project with gradle files in Android!'
