#!/usr/bin/env bash
# ==============================================================================
# DocuSheet - Script de Compilación de Rust para Android (build_rust.sh)
# Compila rust-core para las 4 arquitecturas compatibles:
# - arm64-v8a (aarch64-linux-android)
# - armeabi-v7a (armv7-linux-androideabi)
# - x86 (i686-linux-android)
# - x86_64 (x86_64-linux-android)
# ==============================================================================
set -e

NDK_DIR="${ANDROID_NDK_HOME:-/opt/android/sdk/ndk/28.2.13676358}"
TOOLCHAIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64"
BIN="$TOOLCHAIN/bin"
API=28

CARGO_BIN="${HOME}/.cargo/bin/cargo"
if [ ! -f "$CARGO_BIN" ]; then
    CARGO_BIN="cargo"
fi

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
RUST_DIR="$PROJECT_ROOT/rust-core"
JNILIBS_DIR="$PROJECT_ROOT/app/src/main/jniLibs"

echo "=== Compilando DocuSheet Rust Core para Android (API $API) ==="

# 1. ARM64-V8A
echo "--> Compilando arm64-v8a..."
mkdir -p "$JNILIBS_DIR/arm64-v8a"
CC_aarch64_linux_android="$BIN/aarch64-linux-android${API}-clang" \
CXX_aarch64_linux_android="$BIN/aarch64-linux-android${API}-clang++" \
AR_aarch64_linux_android="$BIN/llvm-ar" \
CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$BIN/aarch64-linux-android${API}-clang" \
"$CARGO_BIN" build --release --target aarch64-linux-android --manifest-path "$RUST_DIR/Cargo.toml"
cp "$RUST_DIR/target/aarch64-linux-android/release/libdocusheet_rust.so" "$JNILIBS_DIR/arm64-v8a/"

# 2. ARMEABI-V7A
echo "--> Compilando armeabi-v7a..."
mkdir -p "$JNILIBS_DIR/armeabi-v7a"
CC_armv7_linux_androideabi="$BIN/armv7a-linux-androideabi${API}-clang" \
CXX_armv7_linux_androideabi="$BIN/armv7a-linux-androideabi${API}-clang++" \
AR_armv7_linux_androideabi="$BIN/llvm-ar" \
CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="$BIN/armv7a-linux-androideabi${API}-clang" \
"$CARGO_BIN" build --release --target armv7-linux-androideabi --manifest-path "$RUST_DIR/Cargo.toml"
cp "$RUST_DIR/target/armv7-linux-androideabi/release/libdocusheet_rust.so" "$JNILIBS_DIR/armeabi-v7a/"

# 3. X86
echo "--> Compilando x86..."
mkdir -p "$JNILIBS_DIR/x86"
CC_i686_linux_android="$BIN/i686-linux-android${API}-clang" \
CXX_i686_linux_android="$BIN/i686-linux-android${API}-clang++" \
AR_i686_linux_android="$BIN/llvm-ar" \
CARGO_TARGET_I686_LINUX_ANDROID_LINKER="$BIN/i686-linux-android${API}-clang" \
"$CARGO_BIN" build --release --target i686-linux-android --manifest-path "$RUST_DIR/Cargo.toml"
cp "$RUST_DIR/target/i686-linux-android/release/libdocusheet_rust.so" "$JNILIBS_DIR/x86/"

# 4. X86_64
echo "--> Compilando x86_64..."
mkdir -p "$JNILIBS_DIR/x86_64"
CC_x86_64_linux_android="$BIN/x86_64-linux-android${API}-clang" \
CXX_x86_64_linux_android="$BIN/x86_64-linux-android${API}-clang++" \
AR_x86_64_linux_android="$BIN/llvm-ar" \
CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$BIN/x86_64-linux-android${API}-clang" \
"$CARGO_BIN" build --release --target x86_64-linux-android --manifest-path "$RUST_DIR/Cargo.toml"
cp "$RUST_DIR/target/x86_64-linux-android/release/libdocusheet_rust.so" "$JNILIBS_DIR/x86_64/"

echo "=== Compilación exitosa de Rust Core para las 4 arquitecturas ==="
