Add-Type -AssemblyName System.Drawing
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$base=Join-Path $root 'src\main\resources\assets\alex_caves_spellsbook\textures'
$names=@('isotope_brand','neutron_rebound','moderation_field','lead_curtain','chain_reaction',
         'radiation_film','cloud_chamber_trail','cherenkov_wake','decay_transposition','critical_matrix')
$fx=@('isotope','neutron','field','curtain','chain','film','cloud','wake','shell','matrix')
foreach($kind in @('icons','effects')){
    $sheet=[System.Drawing.Bitmap]::new(660,312,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g=[System.Drawing.Graphics]::FromImage($sheet)
    $g.Clear([System.Drawing.Color]::FromArgb(24,27,29))
    $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $font=[System.Drawing.Font]::new('Consolas',8)
    for($i=0;$i -lt 10;$i++){
        $col=$i%5;$row=[int][Math]::Floor($i/5)
        if($kind -eq 'icons'){$path=Join-Path $base ('gui/spell_icons/'+$names[$i]+'.png')}
        else {$path=Join-Path $base ('entity/toxic/'+$fx[$i]+'.png')}
        $sprite=[System.Drawing.Bitmap]::new($path)
        if($kind -eq 'icons'){$size=128}else{$size=128}
        $left=$col*132+2;$top=$row*156+2
        if($kind -eq 'effects'){
            $brush=[System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(55,58,62))
            $g.FillRectangle($brush,$left,$top,$size,$size);$brush.Dispose()
        }
        $g.DrawImage($sprite,[System.Drawing.Rectangle]::new($left,$top,$size,$size),0,0,$sprite.Width,$sprite.Height,[System.Drawing.GraphicsUnit]::Pixel)
        $g.DrawString(('{0:D2}' -f ($i+1)),$font,[System.Drawing.Brushes]::White,$left,$top+130)
        $sprite.Dispose()
    }
    $g.Dispose();$font.Dispose()
    $sheet.Save((Join-Path $PSScriptRoot ('preview-'+$kind+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $sheet.Dispose()
}
