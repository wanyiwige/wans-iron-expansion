# 星云灾变法杖

物品 ID：`alex_caves_spellsbook:nebulous_cataclysm_wand`。
继承 Iron's Spellbooks 的 `StaffItem`，保留其施法逻辑和法杖持握动画。

- 12 个法术槽，其中一个内置Ⅰ级星云灾变，其余 11 个可配置。
- 末影、星流、星辉法强分别 +20%。
- 冷却缩减 +20%，吟唱缩减 +50%。
- 属性在主手持握时生效，采用五个不同、稳定的 UUID。
- 法术容器 `spellWheel=true`、`mustEquip=false`，可通过手持物品参与法术轮盘。
- 创造模式入口为 Wan's Iron Expansion 主栏和 ROACW 装备栏；也可用 `/give @s alex_caves_spellsbook:nebulous_cataclysm_wand`。
- 未添加合成或随机掉落。

## 单层模型

按用户最后确认的方向，保留原版 120×124 PNG 的像素配色与轮廓。
Blockbench 使用原生 Extrude Texture / Areas 生成 82 个合并区域；每个区域为同一厚度，
没有逐像素堆叠、额外手掌或螺旋几何。保留用户手动挤出的 `0.3333` 厚度（约 0.333）。
游戏模型将该薄层移至 Z=8 的物品中心，并配置左右手、GUI、地面和展示框变换。
PNG 与原版资源逐字节一致。
游戏 JSON 不继承 `item/handheld` 或 `item/generated`：这条父模型链会触发
`ItemModelGenerator`，覆盖已有实体元素；缺少 `layer0` 时生成空模型。
1.1.17 移除该父模型，直接烘焙 82 个实体区域，保留逐面 `#0` 贴图引用及全部显示变换。

Blockbench 关闭后，从 `backup_12.10.26_22.22_NebulousCataclys.bbmodel` 自动备份恢复。
自动备份当时含两个重复的 82 区域模型；只保留用户改成 0.3333 厚度的那个。
恢复后的 `.bbmodel`、对应 Java JSON 和 PNG 位于实例的
`outputs/nebulous-cataclysm-wand/single-layer/`。
游戏 JSON 从恢复的原生元素和逐面 UV 生成；尚未在重新打开的编辑器中核对。

## 星辉法强计算

现有 `kubejs:astral_spell_power` 存储以 0 为基准的加成，SchoolType 原默认逻辑却直接把
它当以 1 为基准的倍率。本次学派计算将其转换为 `1 + bonus`。
星火、耀爆、星群、光束和贯星的旧固定伤害接入这一学派倍率；星刃原本已读取
`getSpellPower`，因此不再次叠加。未修改这些法术的命中方式、数量、持续时间或获取方式。

## 验证边界

检查构建、重混淆、ROACW 合并、资源完整性、原图哈希、槽位配置和部署哈希。
这些检查不能证明实际游戏的持握、铭刻、法术切换、伤害、多人同步或帧率。
必须重启整合包并进游戏测试后，才可确认运行验收通过。
