package tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tetris.model.ScenarioRoute;

/**
 * 回想モードの一覧。踏んだエンディングを選んで読み返す。
 *
 * <p>並びは JSON に書いた順（minScore 順ではない）。話の順番は作者が決めるもの。
 * 未到達は伏せ字にして選べなくする。
 */
public class RecollectionPane {

    private static final double WIDTH = 1920;
    private static final double HEIGHT = 1080;

    /** 未到達の表示。title をそのまま出すとネタバレになる */
    private static final String LOCKED = "？？？";

    private final StackPane root;
    private final VBox listBox;
    private final List<ScenarioRoute> routes;
    private final List<Boolean> unlocked;
    private final List<Label> rows = new ArrayList<>();

    private int cursor = -1;

    /**
     * @param routes   一覧に出すルート（JSON の並び順のまま）
     * @param unlocked 各ルートが既読か。routes と同じ長さ・同じ並び
     */
    public RecollectionPane(List<ScenarioRoute> routes, List<Boolean> unlocked) {
        this.routes = List.copyOf(routes);
        this.unlocked = List.copyOf(unlocked);

        root = new StackPane();
        root.setPrefSize(WIDTH, HEIGHT);
        ImageAssets.addBackdropView(root, ImageAssets.BASE_LAYER, WIDTH, HEIGHT, Backdrop.FAR);

        Rectangle veil = new Rectangle(WIDTH, HEIGHT);
        veil.setFill(KowloonPalette.alpha(KowloonPalette.SHADOW, 0.72));

        Label title = new Label("RECOLLECTION");
        title.setStyle(MenuStyle.title(64, KowloonPalette.LIGHT_HEX));

        listBox = new VBox(18);
        listBox.setAlignment(Pos.CENTER_LEFT);
        listBox.setStyle(MenuStyle.box());
        listBox.setMaxWidth(760);
        listBox.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        if (routes.isEmpty()) {
            listBox.getChildren().add(emptyRow("シナリオが読み込まれていません"));
        } else if (this.unlocked.stream().noneMatch(Boolean::booleanValue)) {
            listBox.getChildren().add(emptyRow("まだ何も見ていません"));
        } else {
            buildRows();
        }

        Label hint = new Label(rows.isEmpty()
                ? "ESC  ·  戻る"
                : "↑↓  ·  選択        SPACE  ·  読む        ESC  ·  戻る");
        hint.setStyle(MenuStyle.hint());

        VBox content = new VBox(40, title, listBox, hint);
        content.setAlignment(Pos.CENTER);

        root.getChildren().addAll(veil, content);

        moveCursor(0); // 最初の既読へ寄せる
    }

    private void buildRows() {
        for (int i = 0; i < routes.size(); i++) {
            boolean open = unlocked.get(i);
            Label row = new Label(rowText(i, open, false));
            row.setStyle(MenuStyle.value(30, open
                    ? KowloonPalette.LIGHT_HEX
                    : KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.30)));
            rows.add(row);
            listBox.getChildren().add(row);
        }
    }

    private String rowText(int i, boolean open, boolean selected) {
        String mark = selected ? "▶  " : "     ";
        return mark + (open ? routes.get(i).displayTitle() : LOCKED);
    }

    private Label emptyRow(String text) {
        Label label = new Label(text);
        label.setStyle(MenuStyle.value(28, KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.45)));
        return label;
    }

    public StackPane getRoot() {
        return root;
    }

    /** 既読が 1 つも無ければ選ぶものが無い。呼び出し側は ESC だけ受ければいい */
    public boolean hasSelectable() {
        return unlocked.contains(Boolean.TRUE);
    }

    /** いま選んでいるルート。選べるものが無ければ null */
    public ScenarioRoute selected() {
        return (cursor < 0 || cursor >= routes.size()) ? null : routes.get(cursor);
    }

    /** 未到達を飛ばしながらカーソルを動かす。step は +1 / -1 */
    public void moveCursor(int step) {
        if (!hasSelectable()) return;

        int size = routes.size();
        int next = (cursor < 0) ? 0 : cursor + step;
        // 既読に当たるまで進める。step=0 の初期化時は前へ進む向きで探す
        int dir = (step == 0) ? 1 : step;
        for (int tried = 0; tried < size; tried++) {
            int idx = ((next % size) + size) % size;
            if (unlocked.get(idx)) {
                setCursor(idx);
                return;
            }
            next = idx + dir;
        }
    }

    private void setCursor(int idx) {
        if (cursor >= 0 && cursor < rows.size()) {
            rows.get(cursor).setText(rowText(cursor, unlocked.get(cursor), false));
        }
        cursor = idx;
        if (cursor < rows.size()) {
            rows.get(cursor).setText(rowText(cursor, true, true));
        }
    }
}
