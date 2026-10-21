#!/bin/bash
# Last step: switch off USB/network debugging so port 5555 is closed.
# Port 5555 is plaintext ADB: left open, devices on your network may get a shell on the TV.
source "$(dirname "$0")/lib.sh"
IP_ONLY="${D%:*}"
echo "== Final state: home=$(top | grep -o '[a-z0-9_.]*/[A-Za-z0-9_.]*')  disabled=$(S pm list packages -d | wc -l | tr -d ' ')"
S settings put global adb_enabled 0 >/dev/null 2>&1
sleep 2; "$ADB" disconnect "$D" >/dev/null 2>&1
cat <<MSG

>>> On the TV:
    1. Settings > Device Preferences > Developer options: make sure USB debugging (and Network/ADB
       debugging, Wireless debugging) are OFF
    2. Choose 'Revoke USB debugging authorisations'
    3. Restart the TV

MSG
read -r -p "Press Enter once the TV has restarted... " _
if nc -vz -w 3 "$IP_ONLY" 5555 2>/dev/null; then echo "!! Port 5555 is STILL OPEN - check Developer options again."
else echo "   Port 5555 is closed. All done."; fi
