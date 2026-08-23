# UI/UX 改善計画

## 概要

`feature/ui-ux-improvements` ブランチで実施する UI/UX 改善のタスク一覧。
難易度・優先度の低いものから順に着手する。

---

## タスク一覧

### 1. ライン消去フラッシュ（優先度: 高 / 難易度: 低）

**対象:** `src/tetris/view/Render.java`, `src/tetris/Main.java`

- ライン消去時にプレイフィールドを白く一瞬フラッシュさせる
- `FadeTransition` or Canvas の `setOpacity` で実装
- 消去ライン数に応じて強度を変える（1ライン→薄い、4ライン→強い）

---

### 2. ゲームオーバー遷移の演出強化（優先度: 高 / 難易度: 低）

**対象:** `src/tetris/Main.java` (`makeGameOverScene`, `showGameOverScene`)

- 現状: 即座にシーン切替
- 改善: ゲーム画面をスケールダウン＋フェードアウトしてからゲームオーバー画面へ
- `ScaleTransition` + `FadeTransition` を `ParallelTransition` で組み合わせる

---

### 3. HUD にレベル・落下速度を表示（優先度: 中 / 難易度: 低）

**対象:** `src/tetris/view/HudPane.java`, `src/tetris/controller/GameController.java`

- スコア・ライン数に加えて「LEVEL」表示を追加
- ライン数に応じてレベルが上がる仕組みを `GameController` に追加
- レベルアップで落下間隔を短縮（例: Lv1=1000ms → Lv10=100ms）

---

### 4. スコア加算ポップアップ（優先度: 中 / 難易度: 中）

**対象:** `src/tetris/view/HudPane.java` or `src/tetris/Main.java`

- ライン消去時に `+100`、`+300` 等が上に流れるポップアップを表示
- `Label` を `StackPane` に重ねて `TranslateTransition` + `FadeTransition` で演出

---

### 5. スタート画面の演出強化（優先度: 低 / 難易度: 中）

**対象:** `src/tetris/Main.java` (`makeStartScene`)

- 現状: タイトル文字 + 点滅ラベルのみ
- 改善案: タイトル文字のスライドイン or グロウエフェクト
- `TranslateTransition` + `DropShadow` エフェクトを活用

---

## 着手順

1. タスク 1（ラインフラッシュ）
2. タスク 2（ゲームオーバー遷移）
3. タスク 3（HUD レベル表示）
4. タスク 4（スコアポップアップ）
5. タスク 5（スタート画面演出）
