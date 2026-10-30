#!/usr/bin/env bash
# One-time setup and first run on a Mac: tools, the Kotlin framework, the Xcode project, then Kiddo in a simulator.
# Usage: app/iosApp/setup-mac.sh [simulator name]   (default: the first available iPhone)
set -euo pipefail
cd "$(dirname "$0")"

xcode-select -p >/dev/null || { echo "Install Xcode first (App Store), then: sudo xcode-select -s /Applications/Xcode.app"; exit 1; }
command -v brew >/dev/null || { echo "Homebrew is needed for JDK 17 and xcodegen: https://brew.sh"; exit 1; }
if ! /usr/libexec/java_home -v 17 >/dev/null 2>&1; then brew install --quiet openjdk@17; fi
export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home")"
command -v xcodegen >/dev/null || brew install --quiet xcodegen

# The shared modules also declare their Android target, so Gradle needs an Android SDK to configure them (not to
# build iOS).
if ! grep -q '^sdk.dir=' ../local.properties 2>/dev/null; then
  SDK="$HOME/Library/Android/sdk"
  if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    brew install --quiet --cask android-commandlinetools
    mkdir -p "$SDK/cmdline-tools" && ln -sfn "$(brew --prefix)/share/android-commandlinetools/cmdline-tools/latest" "$SDK/cmdline-tools/latest"
  fi
  COMPILE_SDK="$(grep '^android-compileSdk' ../gradle/libs.versions.toml | grep -oE '[0-9]+' | head -1)"
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
  "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platforms;android-$COMPILE_SDK" "build-tools;36.0.0"
  echo "sdk.dir=$SDK" > ../local.properties
fi

echo "Building the Kotlin framework for the simulator (first run downloads the Kotlin/Native toolchain)..."
SIM_TARGET=IosSimulatorArm64
[ "$(uname -m)" = "x86_64" ] && SIM_TARGET=IosX64
(cd .. && ./gradlew ":shared:linkDebugFramework$SIM_TARGET")

xcodegen generate

SIM="${1:-$(xcrun simctl list devices available | grep -m1 -oE 'iPhone [^(]+' | sed 's/ *$//')}"
echo "Building Kiddo for: $SIM"
xcodebuild -project Kiddo.xcodeproj -scheme Kiddo -configuration Debug \
  -destination "platform=iOS Simulator,name=$SIM" -derivedDataPath build build

APP="build/Build/Products/Debug-iphonesimulator/Kiddo.app"
xcrun simctl boot "$SIM" 2>/dev/null || true
open -a Simulator
xcrun simctl install booted "$APP"
xcrun simctl launch booted app.kiddo
echo "Kiddo is running. Open Kiddo.xcodeproj to run it on a device."
