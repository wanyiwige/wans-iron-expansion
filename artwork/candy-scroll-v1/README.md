# 糖果学派卷轴贴图

日期：2026-10-06。版本阶段：1.1。

- 参考：本地 Iron's Spellbooks 1.20.1-3.16.3 JAR 的 10 张卷轴贴图，均为 16×16。
- 主轮廓参考：`references/scroll_evocation.png`。
- 设计：奶油粉纸张、莓红符文、薄荷青卷轴端部。
- 图像生成：使用内置 imagegen；第一稿保留用于比较，最终采用第二稿。
- 游戏贴图导出：按 16×16 网格中心采样、映射至 9 色固定调色板、将 alpha 导出为 0/255。此步骤用于低分辨率游戏资源格式整理。
- 最终贴图：`scroll_candy_16x16_v2.png`。
- 放大对比：`scroll_candy_comparison_v2.png`，左为本体唤魔卷轴，右为糖果卷轴。
- 源码资源：`../../src/main/resources/assets/alex_caves_spellsbook/textures/item/scroll_candy.png`。
- 验证：最终为 16×16 RGBA PNG，无半透明像素；与本体参考的透明轮廓逐像素比较，差异为 0；非透明像素共 137 个。
- 当前仅添加贴图资源，尚未注册糖果学派/卷轴模型；未构建、未替换游戏 JAR、未做游戏内展示。

## 导出调色板

`#FFF1E3` `#FFD0D8` `#E998AC` `#AC5E7D` `#9B255C` `#DB397D` `#6EDBC0` `#258F89` `#D8FFED`
