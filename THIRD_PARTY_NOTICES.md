# Third-party notices

<!-- HAZEN_RECREATION_NOTICE_BEGIN -->
Hazen equipment recreation: the original geometry, pixel textures, display transforms,
animation files, sound recordings and school artwork are retained from the user's
Hazen N Stuff 1.4.0.14, HazentouveLib 1.0.9 and Ace's Spell Utils 1.2.7.2 archives.
These assets remain the work of their original authors. Production gameplay code
in cn.wanyiwige.alexcavesspellsbook.hazen is a Forge 1.20.1 implementation written
for this local integration; it does not bundle the previous backported libraries.
Per-file source paths and SHA-256 receipts are recorded in
development-records/hazen-recreation. Source reference commit:
5fcaf39cf399609f6c1c87d14f8d4807098c9cce. This notice does not relicense the original
assets or grant redistribution rights.
<!-- HAZEN_RECREATION_NOTICE_END -->

Toxic Proton Lance's local pixel-light pass takes its 1/16-block, voxel-center
sampling convention from JNNGL's VanillaDI-reference resource pack
(assets/minecraft/shaders/program/pixelate.fsh, MIT License, Copyright 2024 JNNGL).
The upstream license is included at licenses/VanillaDI-MIT.txt. The Java pass
is a small surface overlay with quantized falloff; it does not bundle VanillaDI's
core shaders or reproduce its complete dynamic-light/ray-traced-shadow pipeline.
The toxic school scroll, icons, effect sprites and industrial texture plates were
drawn using image generation and exported to pixel grids; the two volumetric
prop models were made and UV-mapped through Blockbench MCP.

Nebulous Cataclysm's cloud, four-frame core, fourteen-frame nova and spell icon
are unmodified Calamity Mod artwork from CalamityTeam/CalamityModPublic,
commit 1a8cebd27ec5615316b78f71973446b5528d2b78. Original source paths:
Projectiles/Magic/NebulaCloud.png, NebulaCloudCore.png, NebulaNova.png and
Items/Weapons/Magic/NebulousCataclysm.png. Original files and the projectile
reference code are retained in artwork/nebulous-cataclysm/original. These are
third-party assets used for this user-authorized local integration, not artwork
created by Wan's Iron Expansion. A solid dark-purple backing is rendered behind
the original sprites; the upstream image bytes remain unchanged.

Land of the Lustrous artwork is from the Coralite development team:
https://github.com/CoraIite/Coralite-Mod/tree/21001e126002f5808b90a31cc7e89bd51536f362
The unmodified item icon is Assets/Items/LandOfTheLustrousSeries/LandOfTheLustrous.png.
The floating caster uses frame 0 of LandOfTheLustrousProj.png with local Blockbench
geometry and UV mapping. Upstream declares GPL-3.0; the upstream license is
preserved at licenses/Coralite-GPL-3.0.txt. These visuals are third-party artwork,
not original artwork by Wan's Iron Expansion. This integration is for local use.

The luminous-beam geometry and texture resources are adapted from Brutality
0.8.5 for Minecraft 1.20.1 by goo. Brutality declares the MIT License in its
`META-INF/mods.toml`.

The runtime implementation remains an Iron's Spells spell. It does not access
Brutality's mana capability or spell bar; only the visual, particle, and sound
resources are referenced.

Remnants Of A Cruel World (ROACW) is a separate required mod by Kamaarion and
contributors. Its JAR declares "All Rights Reserved". Wan's Iron Expansion
compiles against ROACW 1.2.2 but does not bundle its classes, assets, data, or
mod metadata. Obtain ROACW from its authors separately.

Piercing Light circle: `assets/astral_spell_compat/textures/entity/piercing_light_circle.png`
is the unmodified `circle_3_neon` texture from Magic Circles 1.2.2 (Minecraft
1.20.1), author FlamesLight. The supplied mod declares AGPL-3.0-only in
META-INF/mods.toml. This is third-party artwork, not original artwork. Preserve
the upstream license and attribution when distributing; this notice does not
relicense the asset or replace AGPL compliance. The supplied reference JAR is
`magiccircles-1.2.2-1.20.1.jar`; the new entity/gameplay code is implemented here.

Asterism Arcanum content recreation (Forge 1.20.1): behavior was inspected from
asterismarcanum-1.21.1-0.1.0.jar. Scroll, spell icon and selected entity textures
are retained upstream artwork. The supplied JAR declares Polyform Shield;
these resources are not relicensed. Gameplay in cn.wanyiwige.astralcompat.recreation
is implemented against Forge 1.20.1 / Iron's Spellbooks 3.16.3 for local integration.


## The Illusionist Grimoire 1.0.2

Forge 1.20.1 recreation integrated into Wan's Iron Expansion from the user-provided NeoForge 1.21.1 reference JAR, authored by Alshanex. The source JAR declares All Rights Reserved. Original textures, models, animations, sound and language resources retain their original ownership and license; this integration does not grant redistribution rights. Runtime logic was adapted/reimplemented for the local instance. Reference bytecode and audit tools are excluded from the release JAR.
