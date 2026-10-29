# Keep adaptive, legacy, splash, and store artwork aligned from the same vector geometry.
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$out = Join-Path $root 'build/icon-assets'
New-Item -ItemType Directory -Force $out | Out-Null
$note = New-Object System.Drawing.Drawing2D.GraphicsPath
$note.StartFigure()
$note.AddLine(12, 3, 12, 13.55)
$note.AddBezier(12, 13.55, 11.41, 13.21, 10.73, 13, 10, 13)
$note.AddBezier(10, 13, 7.79, 13, 6, 14.79, 6, 17)
$note.AddBezier(6, 17, 6, 19.21, 7.79, 21, 10, 21)
$note.AddBezier(10, 21, 12.21, 21, 14, 19.21, 14, 17)
$note.AddLine(14, 17, 14, 7)
$note.AddLine(14, 7, 18, 7)
$note.AddLine(18, 7, 18, 3)
$note.CloseFigure()
$noteTransform = New-Object System.Drawing.Drawing2D.Matrix(2.05, 0, 0, 2.05, 29.4, 18)
$note.Transform($noteTransform)
$text = New-Object System.Drawing.Drawing2D.GraphicsPath
$font = New-Object System.Drawing.FontFamily('Arial')
$text.AddString('OFFLINE', $font, [int][System.Drawing.FontStyle]::Bold, 16,
    [System.Drawing.PointF]::new(0, 0), [System.Drawing.StringFormat]::GenericTypographic)
$bounds = $text.GetBounds()
$scale = 46 / $bounds.Width
$textTransform = New-Object System.Drawing.Drawing2D.Matrix($scale, 0, 0, $scale, (31 - $bounds.X * $scale), (67.5 - $bounds.Y * $scale))
$text.Transform($textTransform)
function Number([float]$value) { $value.ToString('0.####', [System.Globalization.CultureInfo]::InvariantCulture) }
function VectorPath($path) {
    $points = $path.PathPoints
    $types = $path.PathTypes
    $parts = New-Object 'System.Collections.Generic.List[string]'
    for ($i = 0; $i -lt $points.Length; $i++) {
        $kind = $types[$i] -band 7
        $point = $points[$i]
        if ($kind -eq 0) { $parts.Add("M$(Number $point.X),$(Number $point.Y)") }
        elseif ($kind -eq 1) { $parts.Add("L$(Number $point.X),$(Number $point.Y)") }
        elseif ($kind -eq 3) {
            $b = $points[$i + 1]; $c = $points[$i + 2]
            $parts.Add("C$(Number $point.X),$(Number $point.Y) $(Number $b.X),$(Number $b.Y) $(Number $c.X),$(Number $c.Y)")
            $i += 2
        }
        if (($types[$i] -band 128) -ne 0) { $parts.Add('Z') }
    }
    return $parts -join ''
}
$paths = "    <path android:fillColor=`"#00E5FF`" android:pathData=`"$(VectorPath $note)`" />`n    <path android:fillColor=`"#FFFFFF`" android:pathData=`"$(VectorPath $text)`" />"
$header = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp"'
Set-Content -LiteralPath (Join-Path $root 'app/src/main/res/drawable/ic_launcher_foreground.xml') -Encoding UTF8 -Value "$header android:viewportWidth=`"108`" android:viewportHeight=`"108`">`n$paths`n</vector>"
Set-Content -LiteralPath (Join-Path $root 'app/src/main/res/drawable/ic_music_note.xml') -Encoding UTF8 -Value "$header android:viewportWidth=`"72`" android:viewportHeight=`"72`">`n  <group android:translateX=`"-18`" android:translateY=`"-18`">`n$paths`n  </group>`n</vector>"
function RenderLogo([int]$size, [bool]$round, [string]$destination) {
    $large = $size * 4
    $bitmap = New-Object System.Drawing.Bitmap($large, $large)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $background = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#121212'))
    if ($round) { $graphics.FillEllipse($background, 0, 0, $large, $large) }
    else { $graphics.Clear($background.Color) }
    $matrix = New-Object System.Drawing.Drawing2D.Matrix(($large / 72), 0, 0, ($large / 72), (-18 * $large / 72), (-18 * $large / 72))
    $graphics.Transform = $matrix
    $blue = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#00E5FF'))
    $graphics.FillPath($blue, $note)
    $graphics.FillPath([System.Drawing.Brushes]::White, $text)
    $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose(); $bitmap.Dispose(); $background.Dispose(); $blue.Dispose(); $matrix.Dispose()
}
foreach ($entry in @(@('mdpi',48), @('hdpi',72), @('xhdpi',96), @('xxhdpi',144), @('xxxhdpi',192))) {
    RenderLogo $entry[1] $false (Join-Path $out "$($entry[0])-square.png")
    RenderLogo $entry[1] $true (Join-Path $out "$($entry[0])-round.png")
}
RenderLogo 512 $false (Join-Path $out 'app-logo.png')
$note.Dispose(); $text.Dispose(); $font.Dispose(); $noteTransform.Dispose(); $textTransform.Dispose()
Push-Location $root
try {
    # Pillow supplies Lanczos downsampling and lossless WebP encoding.
    @'
from pathlib import Path
from PIL import Image
source = Path('build/icon-assets')
for density, size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    for shape, name in [('square','ic_launcher'),('round','ic_launcher_round')]:
        im = Image.open(source / f'{density}-{shape}.png').resize((size,size), Image.Resampling.LANCZOS)
        im.save(Path('app/src/main/res') / f'mipmap-{density}' / f'{name}.webp', lossless=True)
Image.open(source / 'app-logo.png').resize((512,512), Image.Resampling.LANCZOS).save('app_logo.png')
'@ | python -
    if ($LASTEXITCODE -ne 0) { throw 'Logo conversion failed' }
} finally { Pop-Location }
