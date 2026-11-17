Add-Type -AssemblyName System.Drawing
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$dir=Join-Path $root 'src\main\resources\assets\alex_caves_spellsbook\textures\entity\toxic'
function Draw([string]$name,[bool]$mist){
    $b=[System.Drawing.Bitmap]::new(32,32,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    if($mist){
        $marks=@(@(3,6,5,2),@(12,4,3,2),@(23,7,6,2),@(6,16,4,2),@(17,15,7,2),
                 @(3,25,7,2),@(14,24,4,2),@(25,23,4,2),@(10,10,2,2),@(22,29,2,2))
        $colors=@([System.Drawing.Color]::FromArgb(160,80,133,85),[System.Drawing.Color]::FromArgb(190,150,215,110))
        for($i=0;$i -lt $marks.Count;$i++){
            $m=$marks[$i];for($y=$m[1];$y -lt $m[1]+$m[3];$y++){for($x=$m[0];$x -lt $m[0]+$m[2];$x++){$b.SetPixel($x,$y,$colors[$i%2])}}
        }
    }else{
        $dim=[System.Drawing.Color]::FromArgb(180,64,130,45)
        $bright=[System.Drawing.Color]::FromArgb(235,179,255,84)
        foreach($line in @(4,12,20,28)){
            for($x=2;$x -lt 31;$x++){$b.SetPixel($x,$line,$dim)}
            for($y=2;$y -lt 31;$y++){$b.SetPixel($line,$y,$dim)}
        }
        foreach($pair in @(@(4,4),@(12,20),@(20,12),@(28,28),@(12,4),@(28,12))){
            $b.SetPixel($pair[0],$pair[1],$bright)
        }
    }
    $b.Save((Join-Path $dir ($name+'.png')),[System.Drawing.Imaging.ImageFormat]::Png);$b.Dispose()
}
Draw 'field' $false
Draw 'cloud' $true
