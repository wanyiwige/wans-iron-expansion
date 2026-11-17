# Hazen 装备重做试验版

2026-10-16 生存批次：`1.2.1-hazen-preview.8` 补四种矿石、智木材料链、14 条掉落接入及 83 条配方解锁；原生物等依赖尚有缺口，详见 [第四步说明](hazen-survival-batch1.md)。本批试做包尚未替换正式 preview.7。

2026-10-16 启动修复：`1.2.1-hazen-preview.7` 修复 preview.6 在客户端注册法术状态时，父类构造期间提前调用 `initializeClient` 导致 `kind` 为空的崩溃。灵魂火状态仍隐藏物品栏图标，其他第二批法术内容保持不变。

2026-10-16 法术批次：`1.2.1-hazen-preview.6` 新增其余 31 个原版已注册法术，累计覆盖原版 38 个；同时接入相关状态与装备机制。开关、独立回退、行为范围和验收限制见 [第二批法术说明](hazen-spells-batch2.md)。原模组全部内容仍未完成，以下记录保留各历史批次边界。

2026-10-16 新增护甲批次：`1.2.1-hazen-preview.5` 在 preview.4 基础上重做五套、20 件护甲及 21 份配方，保留此前辐射学派和图集修复。细节、开关、独立回退及验收范围见 [第一批护甲说明](hazen-armor-batch1.md)。用户已反馈此前武器／法术验证没有太大问题；本批新护甲仍需独立客户端验收。以下原始批次描述作为历史记录保留。

2026-10-16 修复状态：当前修复版为 `1.2.1-hazen-preview.4`，包含辐射学派。preview.2 修复正式环境的 StaffTier 方法映射；preview.3 将 50 个模型和 11 个动画移至 GeckoLib 实际扫描的 `geo/hazen/**` 与 `animations/hazen/**`，原始资源字节不变，已用安装的 GeckoLib 4.8.4 完成全部资源解析与烘焙。preview.4 为辐射三维道具与 Hazen 物品的 96 张烘焙模型贴图补充 blocks 图集 single 来源；将 8 个材料模型的 NeoForge 图层加载器和 face-data 键转换成 Forge 对应格式。Minecraft 1.20.1 实际图集加载器对比：旧包 0/96，新包 96/96，原版图集来源保留；Forge 47.4.18 图层加载器解析 8 个模型并保留 7 组发光层数据。贴图、几何、动画与编译类字节均未改变。尚未进行本版本重启后的游戏内渲染复测。以下最初“尚未替换”的描述为构建阶段历史记录，当前部署和备份以 `outputs/radiation-atlas-fix-20261016/deployment.json` 为准。

这批内容直接加入 Wan’s Iron Expansion。基准是用户提供的 Hazen N Stuff 1.4.0.14 JAR；参考提交为 `5fcaf39cf399609f6c1c87d14f8d4807098c9cce`，其 2511 个客户端资源与原 JAR 逐文件一致。没有沿用旧 `hazennstuff_full` 的兼容占位物品，也没有打包移植的 HazentouveLib 或 Ace’s Spell Utils 类。

本批包含当前精简版缺少的 47 件武器和法杖，以及这些武器需要的 7 个专属法术：荆棘环、流星、幽灵斧、永夜刃斩击、泰拉棱镜弹幕、召唤泰拉棱镜、棱光位移。继续遵守此前的重锤排除范围，不加入 Skyscorcher。护甲和其余独立法术尚不属于本批完成范围。

新增物品、法术和学派均使用 `alex_caves_spellsbook:hazen_…` ID。模型、像素贴图、动画和音效取自原档；二维模型和 GeckoLib 使用各自正确的资源路径。武器会出现在主创造页和「Hazen 装备重制」页中。附带原有 Better Combat 攻击配置；没有强制增加 Better Combat 代码依赖。

原学派保留宇宙、辉光、暗影、水系的内容和配色。符文、升级珠与配方依赖的新增材料共 30 项；59 份原配方数据按 1.20.1 格式重新生成。原档中有 16 件新增武器没有合成配方，这里没有自行补造配方。部分原配方仍使用现有精简版或铁魔法提供的材料；缺少依赖的配方会跳过。矿物生成、全部 Boss 掉落和完整生存获取流程尚未复现，不能把材料注册等同于生存流程完成。

有意修正的原定义问题包括：零基数附加属性使用乘法导致加成失效；锻造复制基础物品 NBT 覆盖成品内置法术；召唤剑一级数值覆盖为零；永夜后续斩击引用不存在的动画；泰拉棱镜弹幕伤害错误地引用唤魔法术。吸血飞刀仅在命中伤害实际被接受时回血。传送保留混沌惩罚并检查目的地碰撞。召唤剑使用原五种招式和命中时间重写 AI，未声称与原复合连招决策逐帧一致。

## 开关

整合包配置文件：`config/wans-hazen-recreation-common.toml`。修改后重启游戏；客户端与服务器应使用一致配置。

```toml
[hazen]
enabled = true
weapons = true
spells = true
recipes = true
disabledItems = []
disabledSpells = []
```

`enabled=false` 关闭整个模块；`weapons=false` 关闭武器入口、属性和使用；`spells=false` 关闭专属法术；`recipes=false` 关闭模块配方。单项列表使用原短名称，例如 `disabledItems=["vampire_knives"]`、`disabledSpells=["prismatic_shift"]`。禁用保留注册 ID 和已有物品数据；不要靠删除注册来清理存档。

## 备份与移除

修改前的集成文件和五份原始／当前 JAR 已备份到 `development-backups/hazen-recreation-20261015-205400/`，并核对 SHA-256。当前 `mods` 里的正式 JAR 未被替换。

源码移除工具默认只预览：

```powershell
python tools/remove-hazen-recreation.py
```

显式加 `--apply` 才执行源码移除。它先校验清单，发现手动改动就停止；执行前另做哈希校验备份。仅移除模块生产源码、资源和两处集成入口，保留存档、配置、已有 JAR、审计记录及备份。移除后需重新构建。

## 验证边界

构建、原生启动检查和客户端体验分别记录。最终生产 JAR 已通过 Java 17 编译和 reobfJar。437 条原模型、贴图、动画、声音等资源复制记录通过 SHA-256 校验。原生检查在真实 Forge 服务端世界中通过六组同步断言：注册及内置法术、学派及材料、双持加成及禁用开关、锻造 NBT、投射物原生命中、7 个法术回调及召唤数量／血量／归属／移除；它们不等同于完整 GameTest 或玩家操作验收。标准 GameTest 启动被本机 Java Unix-domain loopback 故障阻断，调用来自 Iron’s Lib 的 Patreon HTTP 初始化。测试没有改动这些依赖或全局 Java／防火墙。

命中检查使用独立空气通道并手动推进实体 tick，目标生命从 20 降至 10.16；这只验证该次荆棘环碰撞路径，不能代表全部投射物、全部战斗交互或实际施法均已验收。完整日志为 `development-records/hazen-recreation/native-final-checks.log`。

最终校验数据见 `development-records/hazen-recreation/verification.json`。第一／第三人称、GUI、掉落物、粒子透明度、联机同步、实际施法操作和 TPS 仍需在客户端与目标整合包中验收。本包为试验产物，未声称整个 HazennStuff 已经重做完毕。
