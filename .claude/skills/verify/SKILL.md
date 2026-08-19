---
name: verify
description: 9pazzle(JavaFXゲーム)をビルド・起動・キー操作して変更を実機確認する手順
---

# 9pazzle 検証レシピ

## ビルド
PowerShell から(bat 末尾の pause は stdin を nul にして回避):

```powershell
& "$env:SystemRoot\System32\cmd.exe" /c "H:\9pazzle\build.bat < nul"
```

## 起動(バックグラウンド)

```powershell
$java = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot\bin\java.exe'
$fx = 'H:\9pazzle\openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib'
$p = Start-Process -FilePath $java -WorkingDirectory 'H:\9pazzle' -ArgumentList `
  '-Dglass.win.uiScale=1.0', `
  '--module-path', "`"$fx`"", '--enable-native-access=javafx.graphics,javafx.media', `
  '--add-modules', 'javafx.controls,javafx.graphics,javafx.fxml,javafx.media', `
  '-XX:ErrorFile=H:\9pazzle\log\hs_err_pid%p.log', '-XX:ReplayDataFile=H:\9pazzle\log\replay_pid%p.log', `
  '-jar', 'app.jar' -PassThru -RedirectStandardError "$env:TEMP\9pazzle-stderr.log"
```

stderr のリダイレクトで JavaFX 例外を捕捉できる(正常時は 0 行)。

## 操作と撮影

- OS スケールは **150%**。そのまま起動すると論理 1920×1080 が物理 2880×1620 になり、
  2560×1440 の画面に収まらず右側（HUD）が切れる。
  → 起動時に `-Dglass.win.uiScale=1.0` を付けて 1:1 で描かせること（上の起動コマンド参照）。
  この状態でウィンドウを (0,0) へ置くと、クライアント領域は画面座標 (10, 40) から 1920×1080
- キー送信: `(New-Object -ComObject WScript.Shell)` → `AppActivate('TETRIS')` → `SendKeys(...)`
  - ゲーム操作キー(H / 矢印など)は SendKeys だと押下→解放が速すぎて 1 フレームにも乗らず
    取りこぼす。user32 `keybd_event` で押下後 150ms ほど待ってから解放すること
  - SPACE=開始 / `{F2}`=スキン切替 / `{F3}`=配置切替 / `u`=総入れ替え / `{ESC}`=ポーズ(エンドクレジット中はスキップ)
- 撮影: `System.Drawing` の `CopyFromScreen(10, 40, 0, 0, [1920,1080])` で PNG 保存
  → Read ツールで目視確認。`GetClientRect` の値は使わない（DPI 仮想化で PowerShell 側と
  座標系がズレるため、固定オフセットの方が確実）

## スキン画像の再生成

```powershell
& $java tools\GenerateSkinImages.java   # リポジトリルートで。冪等
```

## 確認ポイント

- 起動直後 = NEXT CLUSTER 配置 + 1F ARCADE フロア
- F2×4 で 4 フロア一巡(1F ARCADE → 5F MARKET → 9F CLINIC → RF ROOFTOP。
  HUD 見出し右端のフロア名タグとパネルの色味で判別。パネル背景への焼き込みは
  プレイフィールドのみ)
- F3×6 で配置一巡(CLASSIC / SOUTHPAW / CENTER STAGE / NEXT CLUSTER / COCKPIT / THEATER)
- U×24 で配置6×フロア4 の全組合せが一巡

### ウィンドウ操作の注意

- `AppActivate('TETRIS')` は取りこぼすことがある。`MainWindowHandle` を取って
  user32 `SetForegroundWindow` を**撮影の直前に毎回**呼ぶ方が確実
- `Start-Process` は `-Wait` 無しの detached 起動で問題ない（窓は残る）。
  `-Wait` を付けるとツール側を run_in_background にしないと通話がブロックする
- ゲームオーバーするとエンドクレジットへ遷移する。撮影前に SPACE を数回送って
  スタート画面まで戻し、改めて SPACE で開始すること
