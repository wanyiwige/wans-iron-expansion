$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=$PSScriptRoot
$plan=Get-Content -LiteralPath (Join-Path $taskRoot 'uv-plan.json') -Raw | ConvertFrom-Json
$bitmap=[System.Drawing.Bitmap]::new([int]$plan.width,[int]$plan.height,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$roles=@{
    hood='#1B233B';hood_crown='#1B233B';coat='#1B233B';sleeve='#1B233B';tail='#1B233B';front_tail='#1B233B';side_tail='#1B233B';trouser='#1B233B';collar='#2B3854';collar_lock='#074B55';
    visor='#08727B';jaw='#074B55';respirator='#08727B';ocular='#074B55';connector='#3A5564';rear_clasp='#074B55';chest='#08727B';core_socket='#074B55';core='#75ED70';trefoil='#11162B';back_mount='#074B55';back_panel='#08727B';canister='#074B55';cap='#708D9A';belt='#132D35';
    shoulder='#08727B';shoulder_warning='#08727B';shoulder_outer='#074B55';shoulder_shield='#074B55';forearm='#08727B';forearm_side='#074B55';cuff='#074B55';knee='#08727B';thigh='#074B55';boot='#074B55';toe='#08727B';instep='#08727B';sole='#132D35';ankle='#074B55'
}
foreach($face in $plan.faces){
    $c=[System.Drawing.ColorTranslator]::FromHtml($roles[$face.role])
    $x=[int]$face.pixels[0];$y=[int]$face.pixels[1];$w=[int]$face.pixels[2];$h=[int]$face.pixels[3]
    for($py=0;$py -lt $h;$py++){for($px=0;$px -lt $w;$px++){$bitmap.SetPixel($x+$px,$y+$py,$c)}}
}
$bitmap.Save((Join-Path $taskRoot 'uv-flat-template.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$preview=[System.Drawing.Bitmap]::new($bitmap.Width*8,$bitmap.Height*8,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g=[System.Drawing.Graphics]::FromImage($preview)
$g.Clear([System.Drawing.ColorTranslator]::FromHtml('#D7D7D7'))
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bitmap,[System.Drawing.Rectangle]::new(0,0,$preview.Width,$preview.Height),0,0,$bitmap.Width,$bitmap.Height,[System.Drawing.GraphicsUnit]::Pixel)
$uvPen=[System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#C97F87'),1)
foreach($face in $plan.faces){
    $r=$face.rect
    $g.DrawRectangle($uvPen,[single]($r[0]*8),[single]($r[1]*8),[single]($r[2]*8),[single]($r[3]*8))
}
$preview.Save((Join-Path $taskRoot 'uv-paint-template-8x.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$uvPen.Dispose();$bitmap.Dispose();$g.Dispose();$preview.Dispose()
Write-Output ('UV template '+$plan.width+'x'+$plan.height+'; 8x guide saved.')
