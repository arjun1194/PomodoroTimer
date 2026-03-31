#!/bin/bash
set -e

KEYSTORE_FILE="upload_keystore.jks"
KEYSTORE_PATH="app/$KEYSTORE_FILE"
ALIAS="upload"
PASS="PomodoroUpload2026!"

echo "======================================"
echo "Starting Upload Key Generation..."
echo "======================================"

# Generate the keystore using keytool ONLY if it doesn't exist
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Creating new keystore..."
    keytool -genkeypair -v \
      -keystore "$KEYSTORE_PATH" \
      -alias "$ALIAS" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storepass "$PASS" \
      -keypass "$PASS" \
      -dname "CN=Arjun, OU=Dev, O=PomodoroTimer, L=City, S=State, C=US"
else
    echo "Using existing keystore found at $KEYSTORE_PATH"
fi

echo ""
echo "======================================"
echo "Generating keystore.properties..."
echo "======================================"
echo "storePassword=$PASS" > keystore.properties
echo "keyPassword=$PASS" >> keystore.properties
echo "keyAlias=$ALIAS" >> keystore.properties
echo "storeFile=$KEYSTORE_FILE" >> keystore.properties

echo ""
echo "======================================"
echo "Updating .gitignore for Security..."
echo "======================================"
if ! grep -q "keystore.properties" .gitignore; then
  echo "" >> .gitignore
  echo "# Signing Keys" >> .gitignore
  echo "keystore.properties" >> .gitignore
fi

if ! grep -q "\*.jks" .gitignore; then
  echo "*.jks" >> .gitignore
fi

echo "Secrets secured."
echo ""
echo "======================================"
echo "Building the Signed App Bundle..."
echo "======================================"
./gradlew bundleRelease

echo ""
echo "======================================"
echo "✅ Done!"
echo "Your signed release bundle is ready for Google Play upload:"
echo "➡️  app/build/outputs/bundle/release/app-release.aab"
echo "======================================"
