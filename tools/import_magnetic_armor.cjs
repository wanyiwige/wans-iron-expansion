const fs = require('fs');
const path = require('path');
const assert = require('assert/strict');
const project = path.resolve(__dirname, '..');
const source = path.resolve(project, '../魔改与mod制作/模型/磁极法师套装_v2');
const model = JSON.parse(fs.readFileSync(path.join(source, 'magnetic_mage_armor_v2.bbmodel'), 'utf8'));
const groups = new Map(model.groups.map(g => [g.uuid, g]));
const elements = new Map(model.elements.map(e => [e.uuid, e]));
const names = {helmet_head:'armorHead',chest_body:'armorBody',chest_left_arm:'armorLeftArm',
  chest_right_arm:'armorRightArm',leggings_waist:'armorWaist',leggings_left_leg:'armorLeftLeg',
  leggings_right_leg:'armorRightLeg',boots_left_leg:'armorLeftBoot',boots_right_leg:'armorRightBoot'};
const bones = [];
let count = 0;
function visit(node) {
  const group = groups.get(node.uuid);
  assert(group && !(group.rotation || []).some(v => v !== 0), 'Unsupported group rotation');
  if (names[group.name]) {
    const cubes = node.children.map(id => {
      const e = elements.get(id);
      assert(e && e.type === 'cube' && !(e.rotation || []).some(v => v !== 0), 'Unsupported cube');
      const uv = {};
      for (const [side, face] of Object.entries(e.faces)) {
        if (face.texture === null) continue;
        assert(!face.rotation, 'Rotated face requires explicit conversion');
        const [u,v,endU,endV] = face.uv;
        assert(u >= 0 && v >= 0 && endU <= 128 && endV <= 128, 'UV outside atlas');
        // Bedrock reverses both texture axes on the top and bottom faces.
        uv[side] = side === 'up' || side === 'down'
          ? {uv:[endU,endV],uv_size:[u-endU,v-endV]}
          : {uv:[u,v],uv_size:[endU-u,endV-v]};
      }
      count++;
      return {origin:e.from,size:e.to.map((v,i)=>+(v-e.from[i]).toFixed(6)),uv};
    });
    // Waist follows the torso; the body rotation pivot is y=24.
    bones.push({name:names[group.name],pivot:group.name==='leggings_waist'?[0,24,0]:group.origin,cubes});
  } else { for (const child of node.children) visit(child); }
}
model.outliner.forEach(visit);
assert.equal(count,109); assert.equal(bones.length,9);
const assets = path.join(project,'src/main/resources/assets/alex_caves_spellsbook');
fs.mkdirSync(path.join(assets,'geo'),{recursive:true});
fs.writeFileSync(path.join(assets,'geo/magnetic_mage_armor.geo.json'),JSON.stringify({
  format_version:'1.12.0','minecraft:geometry':[{description:{identifier:'geometry.magnetic_mage_armor',
  texture_width:128,texture_height:128,visible_bounds_width:3,visible_bounds_height:3,
  visible_bounds_offset:[0,1.5,0]},bones}]
},null,2)+'\n');
fs.copyFileSync(path.join(source,'magnetic_mage_armor_v2.png'),path.join(assets,'textures/models/armor/magnetic_mage_v2.png'));
console.log(`Imported ${count} cubes, ${bones.length} bones, original 128x128 texture.`);
