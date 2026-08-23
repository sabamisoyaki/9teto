# 未参照アセットの退避先

ここにあるファイルは**どこからも読まれていない**。消してはいないが、現役では
ないので `images/` 直下から退けてある。`package.bat` は配布物へコピーする際に
このフォルダを除外するので、容量は製品に乗らない。

## 旧スキン（20点・約 28 MB）

`skin-cyber-*` / `skin-ember-*` / `skin-neon-*` / `skin-violet-*`

`UiSkinBank` の `SKINS` は九龍城 4 フロアだけを持っており、この 4 系統は
一度も参照されていない。`tools/GenerateSkinImages.java` の `SKINS` 表からも
外れているので**再生成もされない**（生成ツールを回してもここは更新されない）。

コミット `09ce3ff`（九龍城 4 フロアの生成ツールを入れた回）で新規追加された
まま残っていたもの。

## 旧1枚絵（4点・約 4 MB）

`base-layer-1920x1080.png` / `start-bg.png` / `game-over-bg.png` /
`character-closeup-bg.png`

いずれも独自の看板・文字・別 UI を持つ 1 枚絵で、前景の情報（スコア・NEXT・
盤面）と競合するため外した。戻す場合は `ImageAssets` のパス定数を
`images/archive/` 側へ差し替えるだけでよい。

詳細は [ASSETS.md](../../ASSETS.md) を参照。
