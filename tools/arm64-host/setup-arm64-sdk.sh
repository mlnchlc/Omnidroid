#!/bin/sh
# Make an Android SDK usable on an arm64 Linux host (e.g. an Ampere VM).
# Google ships aapt2/zipalign/the NDK toolchain for linux-x86_64 only.
#
# Prerequisites (run once):
#   sudo apt install -y clang-19 lld-19 llvm-19 cmake ninja-build
#   ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
#   ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0" "ndk;28.2.13676358"
#
# What it does (safe to re-run; originals are kept as *.x86_64):
# 1. build-tools 36.0.0: swaps aapt2/aidl/split-select/zipalign for the Commit451
#    arm64 (glibc) builds, verified against the SHA256s pinned below.
# 2. NDK 28.2.13676358: points its x86_64 LLVM tools at Ubuntu's LLVM 19. clang
#    becomes a wrapper that uses Ubuntu clang's own builtin headers (the NDK's are
#    generated for its prerelease 19.0.1 and break 19.1.7) plus the NDK's sysroot
#    and compiler-rt/libunwind runtime libraries.
# 3. ~/.gradle/gradle.properties: tells AGP 9 to use the arm64 aapt2 instead of
#    downloading its x86_64 one from Maven.
# The project's local.properties also needs: sdk.dir=$HOME/Android/Sdk and
# cmake.dir=/usr (system CMake + Ninja).
set -eu

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/36.0.0"
TC="$SDK/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64"
LLVM=/usr/lib/llvm-19/bin
URL=https://github.com/Commit451/android-arm-build-tools/releases/download/platform-tools-36.0.0

[ "$(uname -m)" = aarch64 ] || { echo "not an arm64 host" >&2; exit 1; }
[ -x "$LLVM/clang" ] || { echo "install clang-19 lld-19 llvm-19 first" >&2; exit 1; }

# 1. build-tools
DL="$(mktemp -d)"
trap 'rm -rf "$DL"' EXIT
cat > "$DL/SHA256SUMS" <<'EOF'
7512ff7e381bea6fd310b6f6e347422c8fda21e07c6e3f0162742e95eb9d7f98  aapt2
8c97356b8bba8f7aad44cfd408e3ee24c66a1258244b5d08af1c9249f50dd659  aidl
e8856fb24b10095eb6e940c577ce96d89ddbfc45aa0f7eeaef1597ef68f11a12  zipalign
fb7f0c3c87dbd4243d7e1277ba64ded2399389244058b6a4c969cc615360766c  split-select
EOF
for f in aapt2 aidl split-select zipalign; do
  curl -fsSL -o "$DL/$f" "$URL/$f"
done
(cd "$DL" && sha256sum -c SHA256SUMS)
for f in aapt2 aidl split-select zipalign; do
  if [ -e "$BT/$f" ] && [ ! -e "$BT/$f.x86_64" ]; then mv "$BT/$f" "$BT/$f.x86_64"; fi
  install -m 755 "$DL/$f" "$BT/$f"
done

# 2. NDK toolchain
RD="$TC/lib/clang/19-system"
mkdir -p "$RD"
ln -sfn /usr/lib/llvm-19/lib/clang/19/include "$RD/include"
ln -sfn ../19/lib "$RD/lib"
ln -sfn ../19/share "$RD/share"
cd "$TC/bin"
for f in *; do
  [ -L "$f" ] && continue
  case "$f" in *.x86_64) continue ;; esac
  file -b "$f" | grep -q 'ELF.*x86-64' || continue
  [ -e "$LLVM/$f" ] || continue
  mv "$f" "$f.x86_64"
  if [ "$f" = clang-19 ]; then
    cat > clang-19 <<'EOF'
#!/bin/sh
# Wrapper: run Ubuntu's clang 19 with this NDK's sysroot and runtime libraries.
TC="$(cd "$(dirname "$0")/.." && pwd)"
case "$(basename "$0")" in
  *++*) C=/usr/lib/llvm-19/bin/clang++ ;;
  *)    C=/usr/lib/llvm-19/bin/clang ;;
esac
exec "$C" -resource-dir="$TC/lib/clang/19-system" --sysroot="$TC/sysroot" \
  -fuse-ld=lld --rtlib=compiler-rt --unwindlib=libunwind \
  -Wno-unused-command-line-argument "$@"
EOF
    chmod 755 clang-19
  else
    ln -s "$LLVM/$f" "$f"
  fi
done

# 3. AGP aapt2 override
GP="$HOME/.gradle/gradle.properties"
mkdir -p "$HOME/.gradle"
touch "$GP"
grep -q '^android.aapt2FromMavenOverride=' "$GP" || \
  echo "android.aapt2FromMavenOverride=$BT/aapt2" >> "$GP"

echo "done: $("$BT/aapt2" version 2>&1 | head -1) / $("$TC/bin/clang" --version | head -1)"
