# 无名光神 / Nameless Deity：模型规划与进度

更新日期：2026-09-11。当前阶段：阶段 0——工程核对、参考分析与尺寸规划，已完成。

本文件是后续各阶段的唯一进度与设计基准。后续如需调整尺寸、部件、层级或路径，先在本文件记录理由和变更。阶段 0 结束即停止；本文件不授权自动进入建模或接入阶段。

## 1. 工程核对与证据

实例根目录：`D:/game/PCL/.minecraft/versions/All the Iron's Spellbooks/`。

目标 Mod 工程根目录：`D:/game/PCL/.minecraft/versions/All the Iron's Spellbooks/wans-iron-expansion/`。依据既有项目上下文及本次源码核对确定；工作目录本身是游戏实例，不是 Gradle 工程。下文相对路径均相对于这个 Mod 工程。

| 项目 | 本次核对结果 | 本地证据 |
| --- | --- | --- |
| Minecraft | 1.20.1 | `gradle.properties` 的 `minecraft_version` |
| 加载器 | Forge 47.4.18，不是 NeoForge | `gradle.properties`；`build.gradle` 使用 ForgeGradle 和 `net.minecraftforge:forge`；实例 JSON 中 fmlloader 为 `1.20.1-47.4.18` |
| Java 目标 | 17 | `build.gradle` 的 Java toolchain |
| modid | `alex_caves_spellsbook` | `gradle.properties` 与 `src/main/java/cn/wanyiwige/alexcavesspellsbook/AlexCavesSpellsbook.java` 的 `MOD_ID` 一致 |
| 当前源码版本 | 1.0.4 | `gradle.properties` 的 `mod_version` |
| 实例 GeckoLib | Forge 版 4.8.3 | 实例 `mods/[前置库]geckolib-forge-1.20.1-4.8.3.jar` 内 `META-INF/mods.toml` 和 manifest 均为 4.8.3 |
| 工程 GeckoLib 接入 | 尚未直接声明 | `build.gradle` 仅直接声明 Iron's Spellbooks、Alex's Caves、Citadel；`libs/` 无 GeckoLib；本 Mod `META-INF/mods.toml` 无 GeckoLib 依赖条目，Java 源码未发现 GeckoLib/GeoModel/GeoEntity 引用 |
| 现有实体模型 | Java `EntityModel` / `ModelPart` / `CubeListBuilder` Cuboid 模型 | `src/main/java/cn/wanyiwige/alexcavesspellsbook/client/model/MagneticSeekingArrowModel.java` |
| 现有 Blockbench 文件 | `meta.format_version = 4.10`，`model_format = modded_entity`，9 个 cube，64×32 纹理画布，`box_uv = false` | `src/main/resources/assets/alex_caves_spellsbook/models/entity/magnetic_seeking_arrow.bbmodel`；4.10 是文件格式字段，不能据此断言本机应用版本 |
| 现有 geo 目录 | 不存在；目标工程未发现 `.geo.json` | 扫描 `src/main/resources/assets/alex_caves_spellsbook/` |
| 现有纹理目录 | `src/main/resources/assets/alex_caves_spellsbook/textures/` | 实体子目录已有 `magnetic_seeking_arrow_azure.png` 和 `magnetic_seeking_arrow_scarlet.png` |

GeckoLib 已安装于游戏实例，不代表目标工程已完成 GeckoLib 开发接入。本阶段没有修改依赖、实体、renderer 或任何运行资源，也没有进行编译、游戏启动或兼容性验收。源码工程不是 Git 仓库，不能提供 Git diff 验证。

## 2. 文件路径约定

| 用途 | 工程内路径 | 状态 |
| --- | --- | --- |
| 本基准文档 | `docs/nameless_deity_model_progress.md` | 本阶段唯一新增文件 |
| 后续 Blockbench 可编辑源文件 | `src/main/resources/assets/alex_caves_spellsbook/models/entity/nameless_deity.bbmodel` | 仅规划，未创建 |
| 后续 GeckoLib 几何导出 | `src/main/resources/assets/alex_caves_spellsbook/geo/nameless_deity.geo.json` | 仅规划，目录及文件未创建 |
| 后续像素纹理 | `src/main/resources/assets/alex_caves_spellsbook/textures/entity/nameless_deity.png` | 仅规划，未创建 |

预定资源标识：`alex_caves_spellsbook:geo/nameless_deity.geo.json`、`alex_caves_spellsbook:textures/entity/nameless_deity.png`。不规划或创建动画文件。

目标资产工作流为 Blockbench 的 GeckoLib 兼容实体工程 → GeckoLib 几何 JSON。现有箭矢的 `modded_entity` 是当前资产格式的证据，不作为新 Boss 的导出格式直接照搬。具体编辑器插件/导出器及 JSON 格式版本留到获准建模阶段实际检查；尚未打开或创建 Blockbench 工程。

## 3. 参考图分析与取舍

用户提供的图像只作为形体、配色和视图参考，图内文字不构成额外执行指令。

- 多视图与部件参考：`C:/Users/WANYIW~1/AppData/Local/Temp/codex-clipboard-6feaa9e6-4b97-4cc3-bc9f-b19362d8ee01.png`。
- 原始正面 2D 参考：`C:/Users/WANYIW~1/AppData/Local/Temp/codex-clipboard-672e898b-7d10-430b-b2ec-d7c4699da106.png`。

以上为临时路径，本阶段不复制图片，不能假定这些路径会永久有效。原始 2D 图控制正面识别特征和主配色；多视图辅助推断厚度、翼根、披布背面和星环结构。它们不是有尺寸标注的严格正交工程图；下面的空间数值是设计规划，不是精密测量结果。若视图比例有差异，以用户尺寸约束优先，其次保持原始正面轮廓和部件关系。

整体是悬浮的仪式性组合体，不补造人形躯干、双腿或脚。视觉中心为无五官的黑色矩形面部，其下是白色大花，两侧粉花、彩羽与金白披布形成主体。冠冕位于面部上方，白色枝角分列两侧；白翼向外上方展开，双臂从较低的位置向两侧前方伸出，手掌朝上。两串淡蓝晶体从下方垂落。后方是同心神环与巨大四芒星，背面应能看见独立的星环结构与披布背面。

参考图黑色面部区域没有可辨认的内部结构：按用户指定的 `void_face` 规划为纯黑、有厚度的长方形面部，不自行添加眼睛、嘴或隐藏面容。四芒星只保留上、下、左、右四条主芒；原图的辉光、虚影与细光线不转换成额外主芒、粒子或透明特效。

## 4. 巨型尺寸与坐标约定

建模单位：16 model units（u）= 1 block。直接按下列巨型坐标制作；整个 Boss 的设计缩放为 1，不使用 `renderer.scale()`、骨骼整体放大或小模型成品放大来实现尺寸。单位换算不等于额外放大。

| 尺寸项目 | 用户允许范围 | 本规划基准 |
| --- | --- | --- |
| 白翼左右尖端翼展 | 24～28 blocks / 384～448 u | 26 blocks / 416 u |
| 四芒星上下尖端总高 | 15～18 blocks / 240～288 u | 17 blocks / 272 u |
| 中央主体高度 | 约 6 blocks / 96 u | 6 blocks / 96 u；指面部及承载花冠的核心体量，不计外伸冠角、晶串和星芒 |
| 整体前后深度 | 5～7 blocks / 80～112 u | 6 blocks / 96 u，计入最前手指与最后星体 |
| 星体横向跨度 | 用户未硬性指定 | 暂定 24 blocks / 384 u，左右尖端接近但略收于翼展 |
| 神环外径 | 用户未硬性指定 | 暂定 11 blocks / 176 u |

模型设计原点放在四芒星及神环公共中心。Y 向上；Z 负方向为正面、Z 正方向为背面；正面观察时 X 正方向在画面右侧。`left_*` / `right_*` 按 Boss 自身左右命名，因此 Boss 左侧位于正面画面右侧。后续导出坐标如有变换必须统一应用，不能逐部件混用左右。

整体初始包围范围：X = [-208, 208] u，Y = [-136, 136] u，Z = [-48, 48] u。星尖达到 Y 上下界，翼尖达到 X 左右界；冠角、翼尖、晶串均应位于整体包围范围内。中央主体参考 Y = [-48, 48] u。枢轴仅用于静态分组和摆放，不建立动画。

翼展大于总高会令造型比原始 2D 图更横向舒展，这是按用户巨型尺寸作出的设计取舍。最终高度还需包含冠角和翼尖检查，不能只测量星体后忽略其他部件越界。

## 5. 模型部件树

以下是未来分组/骨骼命名规划，不是已经创建的模型。

```text
nameless_deity_root
├── central_core
│   ├── core_front
│   ├── core_sides
│   ├── core_back
│   ├── void_face
│   ├── crown
│   │   ├── crown_band
│   │   ├── crown_arches
│   │   └── crown_finial
│   └── antlers
│       ├── left_antler
│       └── right_antler
├── left_arm
│   ├── left_upper_arm
│   ├── left_forearm
│   ├── left_cuffs
│   └── left_hand
│       ├── left_palm
│       └── left_fingers
├── right_arm
│   ├── right_upper_arm
│   ├── right_forearm
│   ├── right_cuffs
│   └── right_hand
│       ├── right_palm
│       └── right_fingers
├── left_wing
│   ├── left_wing_root
│   ├── left_wing_coverts
│   ├── left_primary_feathers
│   └── left_secondary_feathers
├── right_wing
│   ├── right_wing_root
│   ├── right_wing_coverts
│   ├── right_primary_feathers
│   └── right_secondary_feathers
├── white_flower
│   ├── white_flower_center
│   └── white_petal_layers
├── pink_flowers
│   ├── left_pink_flower
│   ├── right_pink_flower
│   └── blossom_sprays
├── colored_feathers
│   ├── left_colored_fan
│   └── right_colored_fan
├── cloth
│   ├── left_cloth
│   ├── right_cloth
│   └── rear_cloth_folds
├── hanging_crystals
│   ├── left_crystal_strand
│   └── right_crystal_strand
├── halo_rings
│   ├── inner_ring
│   ├── outer_ring
│   └── ring_ornaments
└── four_point_star
    ├── star_hub
    ├── star_top
    ├── star_bottom
    ├── star_left
    └── star_right
```

## 6. 主要 3D 结构拆分

| 部件 | 形体与像素风实现规划 |
| --- | --- |
| `central_core` | 隐藏于面部、花卉和披布后的实体承载核心；用少量分层 Box 做正面、侧壁、背板，侧面不可为空。避免做成写实人体胸腔。 |
| `void_face` | 深黑矩形浅盒，正面保持平整无五官，侧壁与核心有真实连接；不是朝相机转动的平面贴片。 |
| `crown` | 金色底带、弯拱和顶部饰尖；以旋转短 Box 拼接有棱角的开放拱形，保留中间负空间。 |
| `antlers` | 成对的白色树枝角；由逐级变细的方柱分叉，分枝在深度方向错开，不用圆滑曲线。 |
| `left_arm` / `right_arm` | 明确上臂、肘、前臂；象牙白方柱分段连接，金色袖口以短 Box 围合。前臂朝两侧前方展开。 |
| `left_hand` / `right_hand` | 方块手掌、独立拇指与四根简化手指；用少量有厚度的指节表达掌心朝上和轻微弯曲的静态姿势。 |
| `left_wing` / `right_wing` | 实体翼根与覆羽层承接长羽；单根羽毛以长条薄 Box 和阶梯尖端组成，上下错层、前后叠压，保留主羽之间的缝隙。背面也有完整羽体。 |
| `white_flower` | 面部下方的白色主花；中心小方块与多圈旋转薄 Box 花瓣，内层前凸、外层向后展开，保留白玫瑰式旋涡轮廓。 |
| `pink_flowers` | 左右大型粉花加沿翼根上伸的粉紫花枝；大花有分层实体花瓣，小花用稀疏方块花簇和少量绿叶，不照搬照片微细花瓣。 |
| `colored_feathers` | 粉花外下方成对扇形彩羽；紫、蓝、青、洋红、红色短羽条逐段排列，与白翼是独立结构。 |
| `cloth` | 核心两侧向下外展的白底金橙纹披布，背面有垂褶；用有厚度、分段转折的薄 Box，花纹以后用像素纹理表达。 |
| `hanging_crystals` | 两条下垂的淡蓝晶串；短连接段加大小交替的旋转小 Box，保留串间空隙和不完全一致的晶体节奏。 |
| `halo_rings` | 后方同心金白环；用分段旋转 Box 形成低边数闭合环，局部饰块和前后厚度明确，不使用一整张带透明背景的环图。 |
| `four_point_star` | 中心厚实、四向收尖的巨大金白星体；优先用层叠阶梯 Box 缩窄成尖，正背面都做结构，不能用十字广告牌代替。 |

## 7. 前后层级与空间关系

Z 区间为设计包络，不要求每个部件填满；主层级从正面往后如下。连接部位允许跨层，但必须有可理解的实体连接。

| 前后顺序 | 部件 | Z 范围（u） | 说明 |
| --- | --- | --- | --- |
| 最前 | 手掌、指尖、前臂 | -48～-24 | 指尖达到最前界；手位于身体两侧，不能遮住中心面部 |
| 中前 | `void_face`、`central_core`、冠角前缘 | -24～0 | 黑色面部正面靠近 -24；上臂向核心接合 |
| 中层 | 白花、粉花、彩羽、披布、晶串 | -8～+12 | 白花可前凸到 -20，但位于面部下方，不能盖住黑色矩形；粉花挡住部分翼根，彩羽露在外下侧 |
| 中后 | 翼根、白翼 | +8～+28 | 翼根与核心背部连接，翼面沿展翼方向有折面和厚度，不能把全部羽毛压在同一 Z 平面 |
| 后层 | 神环 | +30～+38 | 环孔保持通透；从背面仍可读出完整圆环 |
| 最后 | 四芒星 | +40～+48 | 星体位于环之后，中央星毂可经连接块延伸到 +38，与环形成有意连接 |

侧面应明确看出“前伸手臂 → 面部核心 → 花羽 → 白翼 → 神环 → 星体”的层次。背面不复制黑色面部或正面白花，应主要呈现星毂、神环、翼背、核心背板和披布背褶。俯视/仰视需能看出前伸手臂、翼面折角及星环厚度。

相邻可见表面避免共面与闪烁，建议预留至少 1 u 的错位；有意覆盖的花瓣、羽片可以相交，但不要留下穿出正面的接头。图中环绕面部的细亮弧，先并入神环的结构分析，不把每条高光都解释成一根实体杆。

## 8. Pixel Art 与建模规则

1. 纯 3D Minecraft 像素风，以 Cuboid / Box 为主体。硬边、平面着色；禁止 Smooth Shading、细分曲面、写实高模和照片投射纹理。
2. 主要轮廓靠真实几何完成，包括翼尖、羽缘、花瓣、星尖、指节及环孔。禁止用完整角色图片或大面积透明平面伪装立体结构。
3. 低面数 Mesh 是用户允许的少量例外，不是必用项。当前先以全 Cuboid 可导出方案规划；未来必须实际验证所用 GeckoLib 导出器及运行端的 Mesh 支持，未验证前将局部尖端、花瓣等也设计为阶梯 Box。薄片优先用有厚度的薄 Box，避免零厚度面片背面消失。
4. 主体结构采用整数 u 网格，大体量块件优先按 2～4 u 步进；局部连接确有需要才用 0.5 u。可见薄羽、布片通常保留 1～2 u 厚度，大型结构按受力视觉增加厚度。各部件按巨型比例直接布置。
5. 纹理以后使用手工像素簇、硬边色块及有限调色板：象牙白/暖灰羽毛，白花，金黄/赭色金属，粉红/洋红花朵，蓝青紫红彩羽，淡蓝晶体和纯黑面部。各材质以少量明暗阶表达，禁用摄影纹理、连续喷枪渐变、法线/PBR 写实细节。
6. 暂定基础纹素密度为 16 px/block，即 1 px/u；未来先做 UV 面积预算，画布可从 1024×1024 候选开始评估，容量不足可调整并记录。不可强行沿用现有箭矢 64×32 画布，也不可把普通小模型纹理整张放大来冒充细节。
7. 重复羽片、花瓣、环段允许共享或镜像 UV，以保持纹素密度；正背面及侧壁都必须覆盖。重要的不对称花枝、布纹和金饰保留独立 UV。纹理使用最近邻显示与清晰像素边界，不用抗锯齿模糊像素。
8. 大花采用可辨认的少数瓣层，白翼采用清楚的主羽/覆羽层级；不为每条羽丝、每根花蕊建几何。构件数量与图集预算待建模阶段记录真实统计，本阶段不以未经验证的面数宣称性能合格。
9. 神环与四芒星先用白金实体和像素色阶表达光感。此阶段不设计或创建发光 renderer、辉光贴图、shader、粒子、AI、技能、实体碰撞或战斗逻辑。
10. 所有姿势静态固定。部件分组可含枢轴，但不创建关键帧、动画控制器或动画 JSON。不要照搬现有箭矢 renderer 的旋转和 scale 行为。

## 9. Blockbench 插件工作约定

以下为本机 Blockbench 已安装插件，按本资产的 GeckoLib 静态巨型 Cuboid 工作流筛选。插件只辅助编辑与核查；不改变本文件约定的资源路径、尺寸、部件树或“先 Blockbench 源文件、后 GeckoLib 几何导出”的流程。

| 插件 | 当前任务适配度 | 允许的用途 | 当前阶段边界 |
| --- | --- | --- | --- |
| `MCP Server` | 高 | 在用户明确授权的后续建模阶段，供外部助手读取模型结构、创建/调整 Cuboid、整理 group，并核查 Front / Side / Back / Perspective。 | 仅在 Blockbench 内确认已启用并连接成功后使用；不以文件存在替代连接或导出验证。本阶段不通过它创建模型。 |
| `Creative Mode` | 中 | 以 WASD 镜头巡航核查 24～28 block 翼展、15～18 block 星高、5～7 block 深度的巨型空间感，以及从侧后方观察翼、环、星的分层。 | 不用其无约束堆放方块；大型结构仍按明确的 group、坐标与 Cuboid 方案创建。 |
| `Bone View` | 中 | 核查 `nameless_deity_root` 下的核心、双臂、双翼、神环、四芒星 group/pivot 是否清晰，辅助静态层级检查。 | 不创建关键帧或动画；pivot 只用于静态组织和后续可能的实体挂接。 |
| `Animated Java` | 不适用 | 无。它面向原版数据包/资源包的展示实体与动画导出。 | 不选择其项目格式、导出器或动画工作流，避免与 Forge + GeckoLib 实体资产混用。 |
| `Animation Sliders` | 不适用 | 无。 | 不创建或修改关键帧。 |
| `Minecraft Entity Wizard` | 不适用 | 无。它面向 Minecraft Bedrock Add-On。 | 不用于 Forge 1.20.1 项目。 |
| `Minecraft Title Generator` | 不适用 | 无。它用于 Minecraft 风格文字标题模型。 | 不用于 Boss 实体模型。 |

后续启动 Blockbench 建模前，应优先确认目标 GeckoLib 格式/导出器可用；`MCP Server`、`Creative Mode` 与 `Bone View` 都不是 GeckoLib 导出器的替代品。任何插件产生的几何仍须遵守第 4、5、7、8 节的原生巨型尺寸、部件命名、前后层级、Cuboid 优先和无贴图/无动画阶段限制。

## 10. 当前完成状态与停止点

- [x] 确认目标源码工程、Minecraft、Forge、modid 与源码版本。
- [x] 读取 GeckoLib JAR 元数据，确认实例为 4.8.3，并记录开发接入尚未完成。
- [x] 核对现有 Java/Blockbench 模型格式、纹理路径及 geo 目录缺失状态。
- [x] 分析两张参考图，规划全部 17 个指定主要部件及静态部件树。
- [x] 记录巨型原生坐标尺寸、前后层级、正背面关系及 Pixel Art 规则。
- [x] 创建本进度文档。
- [x] 记录本机 Blockbench 插件的任务适配度与使用边界；未借此创建任何模型内容。
- [ ] 创建 Blockbench 模型：未开始，等待用户后续明确指令。
- [ ] 创建 geo 几何：未开始。
- [ ] 创建纹理：未开始。
- [ ] 几何、UV、导出和游戏内尺寸验收：未开始，不能声称已验证。

**阶段 0 已完成，到此停止。未创建模型、纹理、动画；未修改代码、依赖或游戏资源。后续先读取本文件，只有用户明确要求进入下一阶段时才继续。**
