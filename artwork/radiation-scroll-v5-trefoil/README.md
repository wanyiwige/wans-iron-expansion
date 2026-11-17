# 辐射卷轴：中央辐射标识修正

本次只修正中央黄色标识，保留此前的16×16原生网格、黑灰卷轴、荧绿色边缘和透明轮廓。

标识由内置 imagegen 单独绘制，再按7×7网格导出，插入原卷轴中心。黄色由上方叶瓣、左下叶瓣、右下叶瓣及独立中心点组成，叶瓣与中心点之间保留完整像素间隔。各部分与卷轴、绿边共用相同的源像素尺寸。

- `trefoil-imagegen-source.png`：生成原图。
- `prompt.txt`：实际绘制提示词。
- `trefoil-7.png`：单色黄色标识，原生7×7。
- `scroll_radiation-black-16.png`：最终卷轴贴图，原生16×16。
- `apply-trefoil.ps1`：标识网格取样、校验及局部合成。
- `native-16-grid-proof.png`：最终贴图的整数倍放大预览，右侧每格对应一个源像素。

工程 `textures/item/scroll_toxic.png` 已替换为此版本；修改前版本保留在 `../radiation-scroll-v4-uniform/`。未构建或部署游戏JAR，游戏画面未复查。
