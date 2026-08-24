# アセット定義一覧

## 画像 (`images/`)

共有パス定数と「存在チェック→ロード→フォールバック」の処理は
`src/tetris/view/ImageAssets.java` に一元化されている。

置き場は**誰が置くか**で分けている。ツールの出力先とアプリの参照先は
必ず対で直すこと（`GenerateSkinImages.OUT_DIR` ↔ `UiSkinBank.img()` など）。

```
images/
  *.png        手で置く既定・フォールバック（hud-bg / next-bg / playfield-bg /
               character / end-credit-bg）
  skin/        tools/GenerateSkinImages.java の出力（22点）
  mock/        tools/BuildRoughMocks.java の出力（2点）
  character/   差し替え用の立ち絵（4点）。ツールは書き込まない
  new/         描いた絵の原本（白地のまま）。tools/RemoveWhiteBackground.java の入力。
               アプリからは読まない
  archive/     未参照。配布からは除外される（package.bat が消す）
```

| ファイル名 | 使用クラス | 役割 |
|---|---|---|
| `skin/bg-kowloon-base-layer.png` | `Main`, `GameView`, `ConfigPane` | 全画面共通の背景（生成物） |
| `skin/bg-kowloon-character-panel.png` | `CharacterPane` | キャラクターパネルの背景（生成物） |
| `bg-kowloon-start.png` | `Main` | スタート画面の専用背景（**未作成**。無ければ base-layer にフォールバック） |
| `bg-kowloon-game-over.png` | `Main` | ゲームオーバーの専用背景（**未作成**。同上） |
| `end-credit-bg.png` | `Main` | エンドクレジット画面の背景（唯一残した手描きアート） |
| `hud-bg.png` | `HudPane` | HUD 背景の既定（スキン画像欠落時のフォールバック） |
| `next-bg.png` | `NextPane` | NEXT/HOLD 背景の既定 |
| `playfield-bg.png` | `DeviceFramePane` | プレイフィールド背景の既定 |
| `character.png` | `CharacterPane` | キャラ立ち絵の既定（スキン画像欠落時のフォールバック） |
| `skin/skin-kowloon-<フロア>-<部位>.png` ×20 | `UiSkinBank` → 各 Pane | フロア別背景・キャラ（生成物。下記参照） |
| `mock/mock-rough-a.png` / `-b.png` | `UiLayoutBank` → `CharacterPane` | 構図ラフの OVERLAY モック（生成物） |
| `character/<フロア名>.png` ×4 | `UiSkinBank` → `CharacterPane` | **手描きの立ち絵はここへ置く**（下記参照） |

> **未参照アセットは `images/archive/` へ退避してある**（削除はしていない）。
> 旧1枚絵 4 点（`base-layer-1920x1080.png` / `start-bg.png` / `game-over-bg.png` /
> `character-closeup-bg.png`）は、独自の看板・文字・別UIを持つ1枚絵で前景の情報
> （スコア・NEXT・盤面）と競合していたため外した。戻す場合は `ImageAssets` の
> パス定数を `images/archive/` 側へ差し替えるだけでよい。
> 旧スキン 20 点（`skin-cyber-*` / `skin-ember-*` / `skin-neon-*` / `skin-violet-*`）は
> 一度も参照されておらず、現在の生成ツールの `SKINS` 表からも外れているので
> 再生成もされない。詳細は `images/archive/README.md`。

### 九龍城パレット（配色の基準）

UI に出る色は **`src/tetris/view/KowloonPalette.java` の 5 色だけ**で組む。
密度を上げても画面が濁らない条件は「色数を増やさないこと」なので、
各 View に新しい hex を直書きしない。

| 役割 | HEX | 用途 |
|---|---|---|
| ベース（暗い青緑） | `#214743` | パネル背景・湿ったコンクリート |
| 影（濡れた煤黒） | `#171C1B` | 最暗部・パネル背景 |
| 光（病的な蛍光灯色） | `#B7C89A` | **全フロア共通のテキスト色**・フラッシュ |
| 差し色（錆びた赤橙） | `#9A4B32` | 枠線・REN 等の補助ポップアップ |
| 看板（褪せたネオン赤） | `#C83F4D` | アクセント・警告・T-Spin |

フロア（＝ワールド回転ステップ）ごとの割り当て表は `UiTheme` の javadoc を参照。
**textColor だけは全フロア固定**にしてある（装飾がどれだけ変わっても、
スコア・NEXT・警告の見え方は変えないため）。傾きは `FxParams.TILT_A / TILT_B` の2種のみ。

### 背景画像（生成物）— 「絵」ではなく「壁」

`tools/GenerateSkinImages.java` が `images/skin/` へ 22 枚を生成する。
**生成物もコミットする**:

```
java tools\GenerateSkinImages.java
```

用意された1枚絵（サイバーパンクの端末・路地のイラスト等）を加工して背景にすると、
元絵が独自の看板・文字・別UIを持つため、どれだけぼかしても前景の情報と競合して
画面が汚れる。そこで背景は**パレット5色だけで手続き的に描く**方式にした。

九龍城の密度は乱雑さではなく「同じ増築ユニットの反復」なので、
窓・室外機・配管・汚れ筋・床スラブを一定の文法で敷き詰めるだけで、
**読めない情報を持たないまま密度が出る**。

| 生成物 | サイズ | 備考 |
|---|---|---|
| `skin-kowloon-<フロア>-hud-bg.png` | 480×400 | 壁。フロア名は**右下**（左上は見出しラベルの位置） |
| `skin-kowloon-<フロア>-next-bg.png` | 420×168 | 同上 |
| `skin-kowloon-<フロア>-playfield-bg.png` | 840×840 | 盤面優先で一番暗く・一番低コントラスト。フロア名は左上 |
| `skin-kowloon-<フロア>-character.png` / `-approach.png` | 1080×1080 | フロア色の透過シルエット（本番の絵は `images/character/` へ。下記） |
| `bg-kowloon-base-layer.png` | 1920×1080 | 全画面。モジュールを大きく取る（細かいと方眼紙に見える） |
| `bg-kowloon-character-panel.png` | 440×560 | キャラパネルの背景 |

- 調整はツール先頭の SKINS 表（壁の地色・部材色・窓明かり色の3色）と、
  `drawWall(..., module, contrast, darken)` の引数だけを触る。
  **前景が乗る面ほど contrast を下げる**のが原則
- フロアごとに固定シードを使うので再実行冪等
- JDK のみで動く（JavaFX 不要）。`tools/` は build.bat / pom.xml の対象外で
  アプリ本体には含まれない

### キャラ立ち絵の差し替え

立ち絵**だけ**は生成物と別に置き場所を持つ。`images/character/` の PNG があれば
`UiSkinBank.character()` がそちらを優先し、無ければ生成シルエットへ落ちる。

| ファイル | 出るフロア |
|---|---|
| `images/character/1F-ARCADE.png` | 1F ARCADE |
| `images/character/5F-MARKET.png` | 5F MARKET |
| `images/character/9F-CLINIC.png` | 9F CLINIC |
| `images/character/RF-ROOFTOP.png` | RF ROOFTOP |

- 差し替えは**このファイルを上書きするだけ**。ビルドは要らないが、
  判定はクラス初期化時の 1 回だけなのでアプリは起動し直す
- 消せば `images/skin/skin-kowloon-<フロア>-character.png` へ落ちる（消しても壊れない）
- `tools/GenerateSkinImages.java` は `images/character/` へ**書き込まない**ので、
  生成の再実行で手描きの絵が消えることはない。代わりに
  「そのフロアはシルエットが表示されない」と知らせる行が出る
- 透過 PNG・1080×1080 基準。縦横比は保たれ、パネルの長辺に合わせて拡大 →
  上端中央そろえでクリップされる（`CharacterPane.CHARACTER_OVERSCAN`）

`-approach.png`（寄り演出の差分）は**現在どこからも読まれていない**ので、
差し替えの対象からは外してある（`UiSkin.approachImage` のコメント参照）。

### 背景の引っ込め処理（ランタイム）

`src/tetris/view/Backdrop.java` が、背景 ImageView へ
ぼかし・脱色・減光・不透明度をまとめて掛ける。**背景として敷く画像は必ずここを通す**。

生成画像は既に静かなのでぼかしはほぼ 0 で、不透明度を下げて
パネルのパレット色と馴染ませるのが主目的。手描きアートが残る `CREDIT` だけ
脱色・減光を強めに掛けている。強度を変えたいときは `Backdrop` の表だけを触る。

> **パレット外のまま残っているもの**
> - ミノ7色（`ShapeType`） … 7 個を瞬時に識別する必要があるため意図的に据え置き
> - スタート／コンフィグ／ゲームオーバー／エンドクレジットの**文字色** …
>   背景と暗幕はパレット化済みだが、ラベルの hex はまだ旧配色

> ワールド回転ステップごとの UI（配色・フォント・枠形状・背景画像・キャラ絵）は
> `src/tetris/view/UiSkinBank.java` の `SKINS` で一元管理している。
> スキンを増やす／画像を差し替える場合はそこに 1 エントリ追加・編集するだけでよい。
>
> パネルの**配置**（位置）は `src/tetris/view/UiLayoutBank.java` の `LAYOUTS` で
> 一元管理している（CLASSIC / SOUTHPAW / CENTER STAGE / SIDEBAR / NEXT_CLUSTER の5種。
> 起動時の配置は `UiLayoutBank.DEFAULT_LAYOUT_INDEX` = NEXT_CLUSTER）。
> 各パネルの座標を 1920×1080 上の絶対座標で指定するだけで配置を組み替えられる。
> スキン（見た目）と配置（位置）は独立しており、自由に組み合わせられる。
>
> **デバッグ用キー（ゲーム中）:**
> - `U` … UI を総入れ替え（配置＋スキンを同時に次へ）。一発でガラッと変わる
> - `F2` … スキン（見た目）だけを次へ
> - `F3` … 配置（位置）だけを次へ
>
> いずれもプレイフィールド上に切り替え後の名前をポップアップ表示する。

### 推奨サイズ

| 用途 | サイズ |
|---|---|
| 全画面背景 | 1920 × 1080 px |
| プレイフィールド背景 | 840 × 840 px |
| HUD 背景 | 480 × 280 px |
| NEXT 背景 | 840 × 168 px |
| キャラクター | 1080 × 1080 px（縦長可、`preserveRatio: true`） |
| キャラアップ背景 | 480 × 1080 px（CharacterPane のサイズに合わせる） |

### 演出パラメータ

演出のタイミング・強度の定数は `src/tetris/view/FxParams.java` に一元化されている
（ワールド回転・UIフリップ・フリーズ時間・各種フラッシュ・シェイク・ポップアップ・セリフ間隔）。
フリーズ時間は「最長の演出時間＋マージン」から導出され、演出を伸ばしても操作再開が
先行しない。ポップアップ演出の共通部品は `src/tetris/view/PopupFx.java`。

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

`ImageAssets` が以下の方針で一律にフォールバックする。

| アセット種別 | フォールバック動作 |
|---|---|
| 全画面背景 | 黒背景（`-fx-background-color: black`）で代替 |
| パネル背景（HUD 等） | スキン画像 → 既定画像 → 背景なし（透過）の順 |
| キャラクター画像 | `images/character/<フロア名>.png` → 生成シルエット → `character.png` → 画像なし（空欄）の順 |
| BGM | BGM なしで起動、ログに `[BGM] Not found` を出力 |
| SE | そのイベントの SE を無音でスキップ |
