# 辐射学派新增十法术：像素资源与铅幕模型

所有可见图标都有完全不透明的 64×64 背景，按原生 32×32 绘制后最近邻放大两倍；世界特效是独立 32×32 透明贴图。所有新增 PNG 都设置了 `blur:false`。

## 图像来源与复现

- 图标草图：`spell_icons_imagegen-source.png`。使用内置 imagegen 生成，提示词：

  > Create a 4 columns by 4 rows SPRITE SHEET for a Minecraft magic spell mod. Each cell is an independent square spell icon with a completely opaque dark charcoal background, clean separation, no text, no letters, no borders between cells. Fill cells in row-major order: (1) isotope brand: yellow radioactive nucleus with three tiny green decay sparks; (2) neutron rebound: bright lime angular ball bouncing off square wall; (3) moderation field: low green grid and sinking particles; (4) lead curtain: thick gray metallic square frame with lime translucent center; (5) chain reaction: zigzag lime arc connecting three marked dots; (6) radiation film: four ghostly green afterimage silhouettes of one target; (7) cloud chamber trail: bright point running along orthogonal pale-green path; (8) Cherenkov wake: blue-cyan pixel dash path and spreading lateral waves; (9) decay transposition: two separated broken green humanoid silhouettes exchanging positions; (10) critical matrix: legible 3 by 3 large-cell grid with one diagonal lit yellow. Cells 11-16 plain dark charcoal. Native Minecraft pixel-art aesthetic, thick uniform square pixels, extremely clear silhouette at 32x32, no fine pixels, no blur, no anti-aliasing, discrete palette. The green, yellow and gray style matches a black radiation school scroll. The atlas itself must be geometrically precise 4x4 equal cells.

- 铅幕金属贴图草图：`lead_curtain_imagegen-source.png`。使用内置 imagegen 生成，提示词：

  > A single seamless tile for a Minecraft radiation spell's thick lead-and-silver metal shield frame. Pixel-art texture source, square, straight-on orthographic view, no object silhouette, no text. Distinct chunky source pixels, medium grey cold metal plates with tiny rivets, dark seams, narrow yellow-green hazard highlights. Flat discrete colors, no anti-aliasing, no gradients, no blur. Designed to downsample to a native 32x32 texture and remain legible. Transparent background false.

资源脚本顺序：

1. `resize-lead-texture.ps1`：生成原生 64×64 金属贴图。
2. `export-pixels.ps1`：裁切图标、量化有限色盘、导出全不透明 64×64 图标和初始 32×32 特效贴图。
3. `refine-vfx.ps1`：将整幅图标拆成可重复使用的单个轮廓、亮点、蓝色路径段和阵列格；可重复运行。
4. `make-ground-sprites.ps1`：绘制稀疏地面方格和云室雾点；可重复运行。
5. `make-preview.ps1`：生成实际像素源的最近邻放大预览。

## 3D 资源

`lead_curtain.bbmodel` 为 Blockbench MCP 创建的可编辑工程，九个有体积的金属构件组成框架；`lead_curtain-export.json` 是 Blockbench 原始导出，游戏模型为 `src/main/resources/assets/alex_caves_spellsbook/models/item/toxic_lead_prop.json`。幕面是独立透明像素贴图，避免把整个防御效果做成薄片。

预览：`preview-icons.png`、`preview-effects.png`、`preview-lead-curtain-3d.png`（Blockbench 编辑器视角）。
