package tetris.view;

import java.nio.file.Path;

import javafx.geometry.Point2D;

/**
 * UI パネルの配置（座標とサイズ）を定義する。
 * 1920×1080 のルート上に絶対座標で置くため、ここに数値を書くだけで構図を組み替えられる。
 * 配置一覧は {@link UiLayoutBank} が一元管理する。
 *
 * プレイフィールドだけは座標のみを持つ。一辺は {@link UiMetrics#FIELD} 固定で、
 * これを変えると {@link Render} のセルサイズ（生成時に確定）とズレるため配置では触らない。
 *
 * その他のパネルは {@link Box} でサイズまで指定する。配置ごとに
 * 「キャラを大きく／HUD を縦長に」といった構図を作れるようにするため。
 */
public final class UiLayout {

    /**
     * 画面の様式。配置ごとに「計器を並べる画面」と「1 枚絵に窓を開ける画面」を切り替える。
     *
     * ラフ（docs/improvements/improvements/12-main-screen-composition.md）の構図は
     * 立ち絵の上に盤面を窓状に重ねるもので、枠付きパネルを並べる様式とは相容れない。
     * どちらが良いかは触って決める前提なので、両方を配置定義から選べるようにしている。
     */
    public enum Style {
        /** 枠・見出し帯・背景テクスチャを持つパネルを並べる */
        PANEL,
        /** キャラを全画面レイヤーに置き、盤面と情報をその上へ重ねる（枠は最小限） */
        OVERLAY
    }

    /** 左上座標とサイズの組 */
    public record Box(double x, double y, double w, double h) {
    }

    /** 配置名（デバッグ表示用） */
    public final String name;
    /** 画面の様式 */
    public final Style style;
    /** プレイフィールド左上（サイズは UiMetrics.FIELD 固定） */
    public final Point2D playfield;
    public final Box hold;
    public final Box next;
    /** キャラのレイヤー矩形。OVERLAY では画面全面を指定し、絵の側が構図を決める */
    public final Box character;
    public final Box hud;
    /** 操作ヒント帯。null ならその配置では表示しない */
    public final Box hint;
    /**
     * この配置専用の立ち絵（null ならスキンの絵を使う）。
     * OVERLAY は「絵が構図を決める」様式なので、どの絵を敷くかは階層（スキン）ではなく
     * 配置に属する。構図ラフから起こしたモックをここへ差す。
     */
    public final Path characterArt;

    public UiLayout(
            String name,
            Style style,
            Point2D playfield,
            Box hold,
            Box next,
            Box character,
            Box hud,
            Box hint,
            Path characterArt) {
        this.name = name;
        this.style = style;
        this.playfield = playfield;
        this.hold = hold;
        this.next = next;
        this.character = character;
        this.hud = hud;
        this.hint = hint;
        this.characterArt = characterArt;
    }
}
