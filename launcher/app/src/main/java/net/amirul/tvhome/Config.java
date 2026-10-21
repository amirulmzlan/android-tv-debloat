package net.amirul.tvhome;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User settings that don't need a rebuild. Read from a plain text file on the TV:
 *   /sdcard/Android/data/net.amirul.tvhome/files/home.conf
 * Lines are key=value; '#' starts a comment. Unknown keys are ignored. Example:
 *   columns=6
 *   dock=com.google.android.youtube.tv,com.netflix.ninja,com.amazon.amazonvideo.livingroom
 *   hidden=com.example.someapp
 *   team_id=364
 *   team_name=LIVERPOOL FC
 * Push it with adb, then: adb shell am force-stop net.amirul.tvhome && adb shell input keyevent KEYCODE_HOME
 */
final class Config {
    int columns = 6;                 // grid columns = number of dock apps (4-6 look best)
    List<String> dock = new ArrayList<>();
    Set<String> hidden = new HashSet<>();
    String teamId = "", teamName = "";

    static Config load(Context c) {
        Config k = new Config();
        k.teamId = c.getString(R.string.fixture_team_id).trim();
        k.teamName = c.getString(R.string.fixture_team_name).trim();
        File f = new File(c.getExternalFilesDir(null), "home.conf");
        if (!f.exists()) return k;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                int hash = line.indexOf('#');
                if (hash >= 0) line = line.substring(0, hash);
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim().toLowerCase(), v = line.substring(eq + 1).trim();
                switch (key) {
                    case "columns":
                        try { k.columns = Math.max(3, Math.min(7, Integer.parseInt(v))); } catch (NumberFormatException ignored) {}
                        break;
                    case "dock": k.dock = split(v); break;
                    case "hidden": k.hidden = new HashSet<>(split(v)); break;
                    case "team_id": k.teamId = v; break;
                    case "team_name": k.teamName = v; break;
                }
            }
        } catch (Exception ignored) {}
        return k;
    }

    static List<String> split(String v) {
        List<String> out = new ArrayList<>();
        for (String s : Arrays.asList(v.split("[,\\s]+"))) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }
}
