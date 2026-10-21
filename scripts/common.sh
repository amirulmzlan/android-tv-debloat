# Portable helpers for macOS, Linux and Windows (Git Bash, as used by Claude Code on Windows).
# Sourced by the other scripts; no ADB connection needed.

find_adb() {  # sets ADB, or prints install help and returns 1
  ADB=$(command -v adb 2>/dev/null || command -v adb.exe 2>/dev/null || true)
  for p in "$HOME/Library/Android/sdk/platform-tools/adb" "$HOME/Android/Sdk/platform-tools/adb" \
           /opt/homebrew/bin/adb /usr/local/bin/adb \
           "${LOCALAPPDATA:-/nonexistent}/Android/Sdk/platform-tools/adb.exe" /c/platform-tools/adb.exe \
           "$HOME/AppData/Local/Android/Sdk/platform-tools/adb.exe"; do
    [ -z "$ADB" ] && [ -x "$p" ] && ADB="$p"
  done
  if [ -z "$ADB" ] && [ -d "${LOCALAPPDATA:-/nonexistent}/Microsoft/WinGet/Packages" ]; then   # winget install location
    ADB=$(ls "$LOCALAPPDATA"/Microsoft/WinGet/Packages/Google.PlatformTools*/platform-tools/adb.exe 2>/dev/null | head -1)
  fi
  if [ -z "$ADB" ]; then
    echo "!! adb not found. Install it, then re-run:"
    echo "   macOS:   brew install --cask android-platform-tools"
    echo "   Linux:   sudo apt install adb      (or your distro's android-tools package)"
    echo "   Windows: winget install Google.PlatformTools   (then open a new terminal)"
    return 1
  fi
}

port_open() {  # port_open <host> <port>  -> 0 if a TCP connection succeeds within ~3 s
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 3 "$1" "$2" >/dev/null 2>&1 && return 0
    command -v timeout >/dev/null 2>&1 || return 1
  fi
  if command -v timeout >/dev/null 2>&1; then timeout 3 bash -c "exec 3<>/dev/tcp/$1/$2" 2>/dev/null
  else (exec 3<>"/dev/tcp/$1/$2") 2>/dev/null; fi
}

lan_hosts() {  # IPv4 addresses this computer has recently seen on the LAN (ARP table)
  arp -a 2>/dev/null | grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' | grep -vE '^(224|239|255)\.|\.255$' | sort -u
}
