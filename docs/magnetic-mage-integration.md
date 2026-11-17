# 磁极法师接入（1.0.15）

## 本轮实现

- 导入 `磁极半机械法师NPC_v6` 的原始 64×64 贴图与 geo；不修改独立建模工程。
- 注册 `alex_caves_spellsbook:magnetic_mage` 及对应刷怪蛋，刷怪蛋放入本模组的磁极物品栏。
- 继承铁魔法 3.16.3 的 `NeutralWizard`、`AbstractSpellCastingMobModel` 和 `AbstractSpellCastingMobRenderer`。沿用原生仇恨存档、施法同步、手持物、行走/瞄准/施法动画以及喝药机制。
- 参照 Pyromancer、Cryomancer 的 `WizardAttackGoal` 和 `WizardRecoverGoal`；牧师的支持/村庄防御逻辑不适合此 NPC，本轮不复制。
- 默认中立、不主动搜寻普通玩家；受攻击后报复，平时游走/看向玩家，战斗时侧移、追击、退避，脱战后喝药恢复。
- 基础生命 60，护甲 6，移动速度 0.25，追踪距离 24，战斗施法距离 14。铁剑用于磁念御器，掉落概率为零；不穿重复覆盖模型的实体盔甲。
- 近距离优先 3 级蔚蓝排斥；随后扫描；生命低于 65% 时可用 1 级双极涡流；攻击使用 1 级特斯拉弧核、1 级坠击、2 级磁念御器；适当距离补用 2 级绯红吸引。
- 常规决策间隔 45–65 tick。特斯拉/双极冷却至少 700 tick，坠击 360 tick，御器 180 tick，推拉 120 tick，扫描 280 tick，另计施法时间。更换目标不清空冷却；冷却为运行时状态，卸载实体后会重置。
- 低血量且与目标有距离时可喝治疗药，战斗喝药间隔至少 300 tick。
- 磁极 NPC 的法术只针对当前仇恨目标，客户端同步目标用于视觉效果。NPC 特斯拉保留电弧伤害和爆炸粒子/声音，不调用会误伤旁观者的原版范围爆炸；玩家施法保持原逻辑。
- 本轮不新增交易、自然生成、结构、掉落奖励或装备配方。原设定里的交易及独立雷电法术仍待后续。

## 验证与测试入口

静态检查：`powershell -File tools/verify-magnetic-mage.ps1`。

2026-09-22 验证结果：Java 17 / Forge 47.4.18 构建通过，`reobfJar` 已执行；静态脚本通过原生骨骼名称、父级、轴心和旋转对照、动画资源存在性、贴图尺寸、语言键以及 JAR 内容检查。Wrapper 遇到已有的 loopback 错误，使用本机 Gradle 8.8 直接启动方式完成构建，未修改全局 Java 或网络设置。

产物：`build/libs/alex_caves_spellsbook-1.0.15.jar`，772086 字节；SHA-256：`7E16CBF05FBD992BB26D7FDF870B72F92D1C3029D195DC436E074ABC6EFEDC6E`。

完成 Forge 编译和 `reobfJar` 不代表完成游戏实测。部署时需替换旧版本，不能把两个版本同时放入 mods；本轮只生成构建产物，不替换实例 JAR。

进游戏后的测试命令：

```mcfunction
/summon alex_caves_spellsbook:magnetic_mage ~ ~ ~
/give @s alex_caves_spellsbook:magnetic_mage_spawn_egg
```

待实测：正反侧面与施法时模型/贴图；生存模式中立和受击还手；创造/旁观模式不被攻击；隔墙与目标死亡停止施法；武器、特斯拉命中与低血量双极；旁观实体不被范围误伤；多人同步、保存重载与专用服务器加载。
