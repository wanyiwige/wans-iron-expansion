"""Verify shipped Xaero integration targets and the new release without launching Minecraft."""
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
instance = root.parent
audit = root / 'outputs/map-travel-audit-20261007'
release = root / 'build/libs/wans_iron_expansion-1.1.3.jar'
old = instance / 'mods/wans_iron_expansion-1.1.2.jar'
if not old.exists():
    deployment = json.loads((audit / 'deployment-1.1.3.json').read_text(encoding='utf-8-sig'))
    old = Path(deployment['backup'])
javap = Path(r'C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot\bin\javap.exe')
worldmap = next((instance / 'mods').glob('*xaeroworldmap*.jar'))
minimap = next((instance / 'mods').glob('*xaerominimap*.jar'))


def disassemble(archive, class_name):
    return subprocess.check_output([str(javap), '-classpath', str(archive), '-p', '-s', '-c', '-l', class_name],
                                   text=True, encoding='utf-8', errors='replace')


targets = {}
for archive, class_name, descriptor in [
    (worldmap, 'xaero.map.teleport.MapTeleporter', '(Lnet/minecraft/client/gui/screens/Screen;Lxaero/map/world/MapWorld;IIILnet/minecraft/resources/ResourceKey;)V'),
    (minimap, 'xaero.hud.minimap.waypoint.WaypointTeleport', '(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V'),
    (minimap, 'xaero.common.gui.GuiWaypoints', 'canTeleport();'),
]:
    output = disassemble(archive, class_name)
    assert descriptor in output, class_name
    targets[class_name] = descriptor

options = disassemble(worldmap, 'xaero.map.gui.GuiMap').split(' getRightClickOptions();', 1)[1]
options = options.split('public void onRightClick', 1)[0]
assert re.search(r'\b6\s+teleportAllowed\s+Z\b', options)
assert re.search(r'\b3\s+coordinatesConfig\s+Z\b', options)
assert re.search(r'\b4\s+waypointsConfig\s+Z\b', options)
assert 'MAP_TELEPORT_ALLOWED:' in options
targets['xaero.map.gui.GuiMap'] = 'teleportAllowed: boolean ordinal 2, slot 6'

# These methods/fields are accessed reflectively; verify them in the installed archives.
for archive, class_name, members in [
    (worldmap, 'xaero.map.WorldMapSession', ['getCurrentSession()', 'getMapProcessor()']),
    (worldmap, 'xaero.map.MapProcessor', ['getMapWorld()']),
    (worldmap, 'xaero.map.gui.GuiMap', ['GuiMap(net.minecraft.client.gui.screens.Screen, net.minecraft.client.gui.screens.Screen, xaero.map.MapProcessor, net.minecraft.world.entity.Entity)']),
    (worldmap, 'xaero.map.world.MapWorld', ['getDimension(', 'getPlayerMapKey()', 'getMapConnections()']),
    (worldmap, 'xaero.map.world.MapDimension', ['getSelectedMapKeyUnsynced()']),
    (worldmap, 'xaero.map.world.MapConnectionManager', ['isConnected(']),
    (minimap, 'xaero.common.gui.GuiWaypoints', ['selectedListSet;', 'displayedWorld;', 'session;']),
    (minimap, 'xaero.hud.minimap.module.MinimapSession', ['getWorldManager()', 'getWorldStateUpdater()', 'getDimensionHelper()', 'getWaypointSession()']),
    (minimap, 'xaero.hud.minimap.waypoint.WaypointSession', ['getTeleport()']),
    (minimap, 'xaero.hud.minimap.world.MinimapWorld', ['getContainer()']),
    (minimap, 'xaero.hud.minimap.world.MinimapWorldManager', ['getAutoWorld()']),
    (minimap, 'xaero.hud.minimap.world.MinimapDimensionHelper', ['getDimensionDivision(']),
    (minimap, 'xaero.hud.minimap.waypoint.WaypointTeleport', ['minimapSession;', 'isWorldTeleportable(', 'isTeleportationSafe(']),
    (minimap, 'xaero.common.minimap.waypoints.Waypoint', ['getX()', 'getY()', 'getZ()', 'isYIncluded()', 'isRotation()', 'getYaw()']),
]:
    output = disassemble(archive, class_name)
    for member in members:
        assert member in output, (class_name, member)

prefix = 'cn/wanyiwige/alexcavesspellsbook/'
required = [prefix + path for path in [
    'traveller/MapTravelSpell.class', 'traveller/MapTravelServer.class', 'traveller/MapTravelNetwork.class',
    'traveller/MapTravelNetwork$Grant.class', 'traveller/MapTravelNetwork$Request.class',
    'traveller/MapTravelNetwork$Result.class', 'traveller/client/MapTravelClient.class',
    'traveller/client/XaeroReflection.class',
    'mixin/xaero/MapOptionsMixin.class', 'mixin/xaero/MapTeleportMixin.class',
    'mixin/xaero/WaypointTeleportMixin.class', 'mixin/xaero/WaypointMenuMixin.class',
]] + ['assets/kubejs/textures/gui/spell_icons/map_travel.png']

with zipfile.ZipFile(release) as archive, zipfile.ZipFile(old) as previous:
    assert archive.testzip() is None
    names = set(archive.namelist())
    assert all(entry in names for entry in required)
    missing = sorted(set(previous.namelist()) - names)
    assert not missing, missing
    config = json.loads(archive.read('mixins.alex_caves_spellsbook.json'))
    assert all('xaero.' + name in config['client'] for name in
               ['MapOptionsMixin', 'MapTeleportMixin', 'WaypointTeleportMixin', 'WaypointMenuMixin'])
    assert not any(name.startswith('xaero.') for name in config['mixins'])
    metadata = archive.read('META-INF/mods.toml').decode('utf-8')
    assert 'version="1.1.3"' in metadata
    classes = list((root / 'vendor/roacw-1.1.1-unpacked/net/kamaarion/roacw').rglob('*.class'))
    assert len(classes) == 98
    for path in classes:
        entry = path.relative_to(root / 'vendor/roacw-1.1.1-unpacked').as_posix()
        assert archive.read(entry) == path.read_bytes(), entry
    for language in ['zh_cn', 'en_us']:
        data = json.loads(archive.read(f'assets/kubejs/lang/{language}.json'))
        assert 'spell.kubejs.map_travel' in data and 'map_travel.unloaded' in data

result = dict(checks='passed', release=str(release), size_bytes=release.stat().st_size,
              sha256=hashlib.sha256(release.read_bytes()).hexdigest(),
              xaero_worldmap=str(worldmap), xaero_minimap=str(minimap), injection_targets=targets,
              required_entries=required, old_entries_missing=missing, roacw_vendor_classes_identical=98,
              gameplay_verified=False)
(audit / 'release-validation-1.1.3.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(result, ensure_ascii=False, indent=2))
