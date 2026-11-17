package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Numbers and paths extracted from original definitions; no guessed model aliases. */
public final class HazenWeaponSpec {
    public String id, kind, model, texture, animation, animation_name, rarity, vanilla_staff_tier;
    public int durability, enchantment;
    public double damage, speed;
    public boolean glowing, animated, fire_resistant;
    public List<ExtraAttribute> attributes;
    public List<Preset> presets;
    public List<Affinity> affinity;
    public List<String> descriptions;
    public record ExtraAttribute(String source_attribute, double amount, String operation) {}
    public record Preset(String source_field, int level) {}
    public record Affinity(String source_field, int bonus) {}
    public static List<HazenWeaponSpec> load() {
        var stream = HazenWeaponSpec.class.getResourceAsStream("/hazen-recreated-weapons.json");
        if (stream == null) throw new IllegalStateException("Missing Hazen weapon definitions");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return List.of(new Gson().fromJson(reader, HazenWeaponSpec[].class));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
