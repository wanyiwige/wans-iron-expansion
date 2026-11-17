# 辐射卷轴：统一像素网格版本

本版保留黑灰主体、黄色辐射标识和荧绿色外边。ImageGen 重绘后，正式以 `scroll_radiation-black-16.png` 的原生 16×16 网格为准；生成大图仅作为绘制源，不直接作为游戏贴图或最终像素检查图。

黄色标识、灰色主体和绿色边缘都只能占完整的原生像素。透明度为 0 或 255，不保留半格图形或平滑边缘。`native-16-grid-proof.png` 是同一张原生贴图的整数倍放大，右侧网格的每格对应一个源像素；`preview-native-grid.ps1` 可复现该预览。

素材由内置 imagegen 绘制；最后一次提示词为 `prompt.txt`。`export-black-scroll.ps1` 负责最近邻取样和限色导出，不给任何局部元素单独使用更高分辨率。32px 文件仅作对照，本次选择16px版本更新工程资源。

原工程64px版本备份为 `previous-project-scroll-64.png`。本次未构建或替换活动游戏 JAR，游戏内实际显示尚未复查。
