# Phase 0 audit

- Target: Minecraft 1.20.1, Forge 47.4.18, Java 17.
- Integration versions: Iron's Spellbooks 3.16.3, Alex's Caves 2.0.2, Travel Optics 6.3.0.
- Addon version before phases 0-3: 1.0.2.
- Magnetic roster before retirement: 12 addon spells plus 3 overridden Travel Optics spell instances.
- Travel Optics spell IDs remain under `traveloptics`; the addon changes their school/behaviour through Mixins.
- `ferric_recall` remains registered as deprecated for old-world `SpellData`, but is disabled and hidden from acquisition.
- Levels 8, 9 and 10 of ten-level addon spells are explicitly classified as Legendary.
- The magnetic rune participates in Iron's `inscribed_rune` tag and crafts the magnetic upgrade orb.
- The magnetic upgrade orb uses Iron's data-driven `upgrade_orb_type` registry and grants 5% Magnetic Spell Power per upgrade.

Static/source checks and Gradle builds do not replace an in-game spell, resource and performance test.
