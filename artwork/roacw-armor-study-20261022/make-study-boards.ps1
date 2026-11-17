$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$studyRoot=$PSScriptRoot
$bounds=Get-Content -LiteralPath (Join-Path $studyRoot 'screenshot-bounds.json') -Raw | ConvertFrom-Json
$sets=@('plaguebringer_armor','mars_armor','fearmonger_armor','auric_tesla_armor','arsenal_t1_armor','earthen_paladin_armor')
$titleFont=[System.Drawing.Font]::new('Microsoft YaHei',24,[System.Drawing.FontStyle]::Bold)
$labelFont=[System.Drawing.Font]::new('Microsoft YaHei',15)
$smallFont=[System.Drawing.Font]::new('Microsoft YaHei',12)
$white=[System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#E4EDF0'))
$muted=[System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#A6BBC0'))
foreach($set in $sets){
    $board=[System.Drawing.Bitmap]::new(1280,850,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g=[System.Drawing.Graphics]::FromImage($board)
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#1C242C'))
    $g.TextRenderingHint=[System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.DrawString(('ROACW 1.2.2 / '+$set),$titleFont,$white,24,16)
    $g.DrawString('原始裸模',$labelFont,$muted,90,80)
    $g.DrawString('原始贴图 · 正面',$labelFont,$muted,365,80)
    $g.DrawString('原始贴图 · 背面',$labelFont,$muted,650,80)
    $g.DrawString('原始像素贴图',$labelFont,$muted,1005,80)
    $views=@('bare-front-three-quarter','textured-front-three-quarter','textured-back-three-quarter')
    for($column=0;$column -lt 3;$column++){
        $name=$views[$column]
        $imagePath=Join-Path $studyRoot ($set+'/'+$name+'.png')
        $source=[System.Drawing.Bitmap]::new($imagePath)
        $box=$bounds.($set+'/'+$name+'.png')
        $sw=[int]$box[2]-[int]$box[0];$sh=[int]$box[3]-[int]$box[1]
        $scale=[math]::Min(280.0/$sw,660.0/$sh)
        $dw=[int]($sw*$scale);$dh=[int]($sh*$scale)
        $x=20+$column*305+[int]((280-$dw)/2)
        $y=126+[int]((660-$dh)/2)
        $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $g.DrawImage($source,[System.Drawing.Rectangle]::new($x,$y,$dw,$dh),[int]$box[0],[int]$box[1],$sw,$sh,[System.Drawing.GraphicsUnit]::Pixel)
        $source.Dispose()
    }
    foreach($kind in @('base','glow')){
        $suffix=if($kind -eq 'base'){'.png'}else{'_glowmask.png'}
        $tex=[System.Drawing.Bitmap]::new((Join-Path $studyRoot ($set+'/'+$set+$suffix)))
        $factor=if($tex.Width -eq 128){2}else{1}
        $tw=$tex.Width*$factor;$th=$tex.Height*$factor
        $tx=935+[int]((320-$tw)/2)
        $ty=if($kind -eq 'base'){140}else{490}
        if($kind -eq 'glow'){$g.DrawString('独立发光遮罩',$labelFont,$muted,1005,445)}
        $g.DrawImage($tex,[System.Drawing.Rectangle]::new($tx,$ty,$tw,$th),0,0,$tex.Width,$tex.Height,[System.Drawing.GraphicsUnit]::Pixel)
        $g.DrawString(($tex.Width.ToString()+' × '+$tex.Height.ToString()+'  / '+$factor.ToString()+'x nearest'),$smallFont,$muted,990,($ty+$th+12))
        $tex.Dispose()
    }
    $g.DrawString('JAR 原资源 / Blockbench 实际预览；无玩家皮肤，未模拟游戏发光渲染。',$smallFont,$muted,24,812)
    $board.Save((Join-Path $studyRoot ($set+'/study-board.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose();$board.Dispose()
}
$titleFont.Dispose();$labelFont.Dispose();$smallFont.Dispose();$white.Dispose();$muted.Dispose()
Write-Output 'Saved six native geometry / texture study boards.'
