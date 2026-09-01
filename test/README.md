# テスト

```
test.bat
```

`src` と `test` をまとめてコンパイルし、`tetris.AllTests` を走らせる。
全部通れば終了コード 0、1 つでも落ちれば 1。

出力先は `out-test/`（`build.bat` の `out/` とは別。アプリのビルド成果物を
テストで壊さないため）。`build.bat` / `package.bat` は `src\*.java` しか拾わないので、
**テストコードが app.jar や配布物へ混ざることはない**。

## 外部ライブラリを使わない

JUnit を入れると配布 jar とビルド手順に波及する（`package.bat` が `javafx-libs` を
並べている構成へライブラリを持ち込むことになる）。検証したいのは素の Java で書ける
ロジックだけなので、`main` と `AssertionError` で足りる。

共通の判定は `tetris.TestSupport` にある。

| ヘルパ | 用途 |
| --- | --- |
| `check(条件, 説明)` | 満たさなければ失敗 |
| `checkEquals(期待, 実際, 説明)` | 値が違えば両方をメッセージに出す |
| `expectInvalid(処理, 語, 説明)` | `IllegalArgumentException` になり、メッセージに手がかりが含まれること |

## いま見ているもの

| テスト | 何を守っているか |
| --- | --- |
| `JsonTest` | 入れ子を隣まで食わない・型違いを既定値で握り潰さない・壊れた JSON は行番号付きで落ちる |
| `ScenarioTest` | `minScore` の境界・立ち絵の引き継ぎ（省略＝継続 / 空文字＝消す）・未知のパート名で落ちない |
| `ScenarioValidationTest` | シナリオのスキーマ（必須項目・id と minScore の重複） |
| `KeyBindingsTest` | 割り当ては置き換え・予約キーを取らせない・保存の往復 |
| `GameConfigTest` | 設定と進行状況の保存／復元・項目を増やしても古いファイルで壊れない |

いずれも**画面を起動しない**。JavaFX の型（`KeyCode` / `Color`）には触れるので
モジュールパスは要るが、ウィンドウは開かないので CI でも回せる。

## 足すとき

1. `test/` 以下に `public static void main(String[])` を持つクラスを作る
2. 失敗したら `AssertionError` を投げる（`TestSupport` を使えば自動で投がる）
3. **`AllTests` の `TESTS` に 1 行足す**。ここに登録し忘れると走らない

## 実データを壊さないこと

`GameConfig` の保存先はコンストラクタで差し替えられる。
テストは必ず一時ディレクトリを渡すこと。既定のままだと
`~/.9pazzle/settings.properties` を書き換えてしまい、
テストのたびにプレイ記録（ハイスコア・既読エンディング・キー割り当て）が消える。

```java
Path dir = Files.createTempDirectory("9pazzle-test");
GameConfig config = new GameConfig(dir.resolve("settings.properties"));
```
