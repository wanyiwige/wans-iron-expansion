package cn.wanyiwige.alexcavesspellsbook.candy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One ledger per cast shared by every child: collision callbacks cannot multiply hits. */
public final class CandyHitLedger {
    private record Entry(long last, int count) { }
    private final Map<UUID, Entry> hits = new HashMap<>();
    public boolean claim(UUID target, long tick, int interval, int maximum) {
        if (maximum <= 0 || interval < 1) return false;
        Entry old = hits.get(target);
        if (old != null && (old.count >= maximum || tick - old.last < interval)) return false;
        // Bound area spam across a cast, including missed/cancelled damage attempts.
        if (old == null && hits.size() >= 128) return false;
        hits.put(target, new Entry(tick, old == null ? 1 : old.count + 1));
        return true;
    }
    public int count(UUID target) { Entry e = hits.get(target); return e == null ? 0 : e.count; }
}
