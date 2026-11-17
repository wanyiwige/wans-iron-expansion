# 毒化卷轴的小尺寸版本

原稿 `scroll_toxic-v1.png` 已保留。它是 1254×1254 的生成设计图，实测含 49,871 种可见 RGBA 颜色、529,326 个半透明像素，不能因为外观像像素画就视为原生低分辨率贴图。

ImageGen 针对原稿做了像素简化，生成 `scroll_toxic-v2-pixel-source.png`，维持卷轴轮廓、暗绿纸面、金属轴头、黄色箍带和荧绿标志。生成结果仍需导出到精确的纹理尺寸。

`export-crisp.ps1` 使用中心最近邻取样导出 32×32、64×64，限制到 18 色预设调色板，将透明度设为 0 或 255。没有双线性、双三次或其他平滑缩放；原始生成图不被覆盖。调用方式：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\export-crisp.ps1
```

选择 `scroll_toxic-64.png` 为正式素材。32×32 版本的标志与轴头损失明显，保留用于对照，不作为正式纹理。

`inventory-scale-check.png` 比较原稿、32px、64px 三个来源在 16、32、48 个屏幕像素下的最近邻显示；底部是整数倍放大。它是离线栅格预览，不包含游戏图集、物品模型变换、光照、GUI 缩放和第三方渲染修改，不能代替实际游戏截图。

工程资源：

- `src/main/resources/assets/alex_caves_spellsbook/textures/item/scroll_toxic.png`：正式 64×64 RGBA。
- 相邻 `.png.mcmeta`：声明 `blur=false`，没有动画；此声明不能强制覆盖其他模组对整个图集的采样修改。
- `src/main/resources/assets/alex_caves_spellsbook/models/item/scroll_toxic.json`：引用上述纹理的 generated 物品模型，供后续学派注册使用。

本次只准备贴图和模型资源。毒化学派尚未注册，没有构建或部署，没有更改当前画质选项或其他卷轴。

后续接入时检查实际物品栏、快捷栏和法术合成界面，重点确认辐射标志、两端卷轴和透明边缘；同时检查资源包是否覆盖路径。通过实际显示后才能记为游戏内验收。
