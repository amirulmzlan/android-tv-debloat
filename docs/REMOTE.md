# Working on a TV in another home (Tailscale)

ADB must never be exposed to the internet. To help a TV somewhere else, put both machines on a private
Tailscale network:

1. At the TV's home, install **Tailscale** on the TV from the Play Store (Android TV app), sign in, and note the
   TV's Tailscale IP (100.x.y.z) in the app.
2. Install Tailscale on your computer and sign in to the **same** tailnet (or share the TV's node with you).
3. Turn on USB/Network debugging on the TV (someone at the TV accepts the "Allow USB debugging?" prompt once).
4. Connect: `./scripts/01-connect-and-backup.sh 100.x.y.z` — use that address everywhere; **don't scan the local
   network** of either home.
5. At the end, run `scripts/close-adb.sh`, and consider removing the TV from the tailnet.

Tailscale's Android TV app can stop when the TV sleeps; if the connection drops, ask someone to open it again.
