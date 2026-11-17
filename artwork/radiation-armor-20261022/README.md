# 临界监护者：辐射学派护甲模型

日期：2026-10-22。

## 当前阶段

已按外观设计完成四件护甲的立体几何，使用实际运行的 Blockbench MCP 1.7.0 创建、修整、预览和导出。先完成模型，再进行精细像素贴图；目前仅使用纯色色块区分布料、金属、密封带与能量件。

此目录是建模工作成果，未改动生产资源或装备注册，未构建、部署或进行游戏内测试。

## 主要文件

- `critical_warden_armor.bbmodel`：当前可编辑主文件，GeckoLib Armor 格式，内嵌基础配色贴图，可独立打开。
- `critical_warden_armor.geo.json`：从 Blockbench 导出的几何文件，标识为 `geometry.armor.critical_warden`。
- `critical_warden_block_colors.png`：16×16 临时色板，仅用于分区辨认；不是正式护甲像素贴图。
- `critical_warden_geometry_v1.bbmodel`：初次建好几何后的保留版本；当前主文件已修整其重叠面、目镜环与衣摆展开方向。
- `preview-front-three-quarter.png`、`preview-front.png`、`preview-back-three-quarter.png`、`preview-back.png`、`preview-left.png`：当前模型的实际 Blockbench 预览。
- `preview-articulation.png`：编辑器内临时转头、抬手和迈步姿势预览，已恢复中立姿势。
- `model-manifest.json`：导出数量、检查项、文件哈希与验证边界。

## 已完成的结构

四件装备合计 169 个有厚度的立方体，13 个骨骼分组，1014 个有材质引用的面。

| 装备 | 结构 |
| --- | --- |
| 临界观测兜盔 | 包覆头壳、短护脊、分隔观察窗、三槽呼吸面罩、左侧嵌入式机械目镜、后脑状态窗 |
| 裂变容纳法衣 | 高领、不对称肩甲、核心隔离框与夹具、胸前辐射徽记、前臂护板、贴背反应堆匣、四片独立分组衣摆 |
| 铅封护腿 | 腰带与三格仪表扣、密封罐、腿侧护板、台阶护膝、后侧裤料折面 |
| 闭环踏靴 | 方形鞋头、厚底、密封靴口、胫甲能量窗、踝部连接盒、后跟扣和三段鞋底纹 |

九个根分组沿用工程已有的人形护甲约定：`armorHead`、`armorBody`、`armorLeftArm`、`armorRightArm`、`armorLeggingTorsoLayer`、`armorLeftLeg`、`armorRightLeg`、`armorLeftBoot`、`armorRightBoot`。坐标与现有磁极护甲一致，左侧为正 X。

后衣摆 `coatTailLeft/Right` 与侧衣摆 `coatSideLeft/Right` 分别保留独立枢轴，当前归属 `armorBody`。后续接入游戏时，需要处理衣摆随腿部运动以及靴子随腿骨旋转的同步；本次临时姿势由编辑器手动设置，不代表运行时动画已实现。

## 模型检查与后续贴图

已检查正、侧、背与三分之四视图，修整肩部、袖口、目镜与衣摆包边的共面重叠。主文件的全部元素均归入骨骼、UUID 无重复、各立方体三轴厚度均大于零，原生几何导出保留全部 169 个立方体和 13 个骨骼。

当前 UV 映射到纯色色板，不作为正式像素密度依据。模型定型后，应重新排布完整 UV，在各面使用相同单位面积像素密度，随后绘制布料折面、金属磨痕、紧固件、刻度、核心与观察窗；正背面、内侧、鞋底均需覆盖。贴图沿用深靛蓝、青绿金属、翡翠绿与黄绿色，保持粗像素格与清楚的符号。

生成与修整脚本均通过本地 MCP 操作 Blockbench；重复运行 `build_model.py` 会向当前项目加入几何，因此不要在已有完整模型上重复执行。
