# Batch lists

The agent writes one file per batch here after you approve the plan, e.g. `lists/01-telemetry.txt`:

```
# Xiaomi telemetry
com.miui.tv.analytics
com.xiaomi.statistic
```
Then: `./scripts/02-debloat.sh lists/01-telemetry.txt`. Files in this folder are personal to your TV
(they're ignored by git except this README).
