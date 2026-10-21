package net.amirul.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Next matches for one club, from ESPN's public team-schedule feed (all competitions). */
final class Fixtures {
    private Fixtures() {}

    static final class Match {
        long kickoff;          // epoch ms
        String opponent = "", competition = "", venue = "";
        boolean home;
    }

    static String url(String teamId) {
        return "https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/" + teamId + "/schedule?fixture=true";
    }

    static String fetch(String teamId) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url(teamId)).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "TvHome/1.0");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
            return sb.toString();
        } finally { c.disconnect(); }
    }

    static List<Match> parse(String json, String teamId) {
        List<Match> out = new ArrayList<>();
        try {
            JSONArray ev = new JSONObject(json).optJSONArray("events");
            if (ev == null) return out;
            for (int i = 0; i < ev.length(); i++) {
                JSONObject e = ev.getJSONObject(i);
                long t = parseDate(e.optString("date"));
                if (t <= 0) continue;
                Match m = new Match();
                m.kickoff = t;
                JSONObject league = e.optJSONObject("league");
                m.competition = shortComp(league != null ? league.optString("name") : "");
                JSONArray comps = e.optJSONArray("competitions");
                JSONObject comp = comps != null && comps.length() > 0 ? comps.getJSONObject(0) : null;
                if (comp != null) {
                    JSONObject venue = comp.optJSONObject("venue");
                    if (venue != null) m.venue = venue.optString("fullName");
                    JSONArray cs = comp.optJSONArray("competitors");
                    if (cs != null) for (int j = 0; j < cs.length(); j++) {
                        JSONObject c = cs.getJSONObject(j);
                        JSONObject team = c.optJSONObject("team");
                        String id = team != null ? team.optString("id", c.optString("id")) : c.optString("id");
                        String name = team != null ? team.optString("shortDisplayName", team.optString("displayName")) : "";
                        if (teamId.equals(id)) m.home = "home".equals(c.optString("homeAway"));
                        else m.opponent = name;
                    }
                }
                if (m.competition.isEmpty() && comp != null) {
                    JSONObject season = e.optJSONObject("season");
                    if (season != null) m.competition = shortComp(season.optString("displayName").replaceAll("^\\d{4}-\\d{2} ", ""));
                }
                out.add(m);
            }
        } catch (Exception ignored) {}
        Collections.sort(out, (a, b) -> Long.compare(a.kickoff, b.kickoff));
        return out;
    }

    static String shortComp(String s) {
        return s == null ? "" : s.replace("UEFA ", "").replace("English ", "").replace("FA Community Shield", "Community Shield").trim();
    }

    static long parseDate(String s) {
        for (String p : new String[]{"yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"}) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = f.parse(s);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return 0;
    }
}
