# When the AI agent can't reach your TV directly

> This is the **fallback**. First try letting Claude do everything itself (README, Option 1).

Many AI agents run in a sandbox (a cloud container, or a VM on your computer) whose network can reach websites but
**not devices on your home network**. ADB needs a direct connection to the TV on port 5555, so the agent can't run
`adb` itself. That's fine — this is how the original run of this project was done:

1. The agent writes each step as a script in this folder (`scripts/...`) — never more than one phase at a time.
2. You run it in **Terminal** (Mac/Linux) or **Git Bash** (Windows) on your own computer, which is on the same Wi-Fi as the TV:
   `cd android-tv-debloat && ./scripts/<step>.sh`
3. The script saves its log to `state/` and screenshots from the TV to `shots/`.
4. You tell the agent "done". It reads the log and **looks at the screenshots** before writing the next step.

If the agent can read files on your computer (e.g. Claude desktop with this folder connected), step 4 needs no
copy-paste. Otherwise paste the script's output into the chat.

**For agents:** check reachability once (`nc -vz <tv-ip> 5555` from your own shell). If it fails with
"Network is unreachable" or a proxy refusal, don't keep retrying other tools — switch to this mode, and say so.
