# アセット定義一覧

## 画像 (`images/`)

| ファイル名 | 使用クラス | 役割 |
|---|---|---|
| `base-layer-1920x1080.png` | `Main`, `GameView`, `ConfigPane` | 全画面共通の背景（スタート・ゲーム・ゲームオーバー・コンフィグ） |
| `end-credit-bg.png` | `Main` | エンドクレジット画面の背景 |
| `playfield-bg.png` | `DeviceFramePane` | プレイフィールドの背景 |
| `hud-bg.png` | `HudPane` | スコア・ライン表示エリアの背景 |
| `next-bg.png` | `NextPane` | NEXTミノ表示エリアの背景 |
| `character.png` | `CharacterPane` | キャラクター（ワールド回転なし / デフォルト） |
| `character-rotate-1.png` | `CharacterPane` | キャラクター（ワールド回転ステップ 1） |
| `character-rotate-2.png` | `CharacterPane` | キャラクター（ワールド回転ステップ 2） |
| `character-rotate-3.png` | `CharacterPane` | キャラクター（ワールド回転ステップ 3） |
| `start-bg.png` | ― | 未使用（スタート画面専用背景として用意） |
| `game-over-bg.png` | ― | 未使用（ゲームオーバー画面専用背景として用意） |
| `character-bg.png` | ― | 未使用（旧キャラクター背景。`character-closeup-bg.png` に置き換え済み） |
| `character-closeup-bg.png` | `CharacterPane` | キャラクターアップの背景画像。キャラ非表示中に CharacterPane の背景として使用 |

### 推奨サイズ

| 用途 | サイズ |
|---|---|
| 全画面背景 | 1920 × 1080 px |
| プレイフィールド背景 | 840 × 840 px |
| HUD 背景 | 480 × 280 px |
| NEXT 背景 | 840 × 168 px |
| キャラクター | 1080 × 1080 px（縦長可、`preserveRatio: true`） |
| キャラアップ背景 | 480 × 1080 px（CharacterPane のサイズに合わせる） |

---

## 音声 (`audio/`)

### BGM

| ファイル名 | 使用クラス | 役割 |
|---|---|---|
| `bgm.wav` | `Main` | ゲーム中ループ再生。最大出力音量は `0.35`（コンフィグの 100% = 実音量 0.35） |

> 現在使用中の BGM：https://kyattoworks.com/fizzy/

### SE（効果音）

SE は `SePlayer` が起動時にロードし、ファイルが存在しない場合はそのイベントをスキップします。

| ファイル名 | トリガー |
|---|---|
| `se_move.wav` | ミノの左右移動（成功時） |
| `se_rotate.wav` | ミノの回転（成功時） |
| `se_harddrop.wav` | ハードドロップ |
| `se_lock.wav` | ミノ接地・固定 |
| `se_clear.wav` | ライン消去 |
| `se_world_rotate.wav` | ワールド回転（盤面 90° 回転） |
| `se_hold.wav` | ホールド操作 |
| `se_tspin.wav` | T-Spin（フルスピン + ライン消去） |
| `se_tspin_mini.wav` | T-Spin Mini（+ ライン消去） |
| `se_ren.wav` | Ren コンボ（2 連続以上のライン消去） |
| `se_pinch.wav` | 仮ゲームオーバー（スポーン詰まり） |

### 音量の仕組み

```
実際の出力音量 = コンフィグの割合（0.0〜1.0） × 上限値
BGM 上限: BGM_MAX_VOLUME = 0.35  (Main.java)
SE  上限: AudioClip.play(volume) に seVolume をそのまま渡す（上限 1.0）
```

---

## ファイルが存在しない場合のフォールバック

| アセット種別 | フォールバック動作 |
|---|---|
| 全画面背景 | 黒背景（`-fx-background-color: black`）で代替 |
| パネル背景（HUD 等） | 背景なし（透過）で継続 |
| キャラクター画像 | 画像なし（空欄）で継続 |
| BGM | BGM なしで起動、ログに `[BGM] Not found` を出力 |
| SE | そのイベントの SE を無音でスキップ |
