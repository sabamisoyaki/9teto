package tetris.view;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class EndCreditPane {

    private static final double WIDTH = 1920;
    private static final double HEIGHT = 1080;

    // JSON文字列値にマッチするパターン（エスケープ文字を含む）
    private static final String JSON_STRING = "\"((?:[^\"\\\\]|\\\\.)*)\"";

    private final StackPane root;
    private final VBox creditsBox;

    public EndCreditPane(String creditJson, Path backgroundImagePath) {
        root = new StackPane();
        root.setPrefSize(WIDTH, HEIGHT);
        ImageAssets.addBackdropView(root, backgroundImagePath, WIDTH, HEIGHT, Backdrop.CREDIT);

        Rectangle overlay = new Rectangle(WIDTH, HEIGHT);
        overlay.setFill(KowloonPalette.alpha(KowloonPalette.SHADOW, 0.6));
        root.getChildren().add(overlay);

        creditsBox = new VBox(18);
        creditsBox.setAlignment(Pos.TOP_CENTER);

        List<String> lines = parseCreditLines(creditJson);
        for (String line : lines) {
            Label label = new Label(line);
            label.setStyle("-fx-font-size: 34px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.0, 2, 2);");
            if (line.startsWith("# ")) {
                label.setText(line.substring(2));
                label.setStyle("-fx-font-size: 42px; -fx-text-fill: #aaddff; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.0, 2, 2);");
            }
            creditsBox.getChildren().add(label);
        }

        Pane viewport = new Pane(creditsBox);
        viewport.setPrefSize(WIDTH, HEIGHT);

        Rectangle clip = new Rectangle(WIDTH, HEIGHT);
        viewport.setClip(clip);

        root.getChildren().add(viewport);
    }

    public StackPane getRoot() {
        return root;
    }

    public Timeline buildScrollAnimation() {
        // レイアウト計算を強制してから高さを取得する
        creditsBox.applyCss();
        creditsBox.layout();

        double startY = HEIGHT;
        double endY = -(creditsBox.prefHeight(-1) + 250);
        double totalDistance = startY - endY;
        double speedPixelPerSecond = 85;
        Duration duration = Duration.seconds(totalDistance / speedPixelPerSecond);

        creditsBox.setTranslateY(startY);
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(creditsBox.translateYProperty(), startY, Interpolator.LINEAR)),
                new KeyFrame(duration,
                        new KeyValue(creditsBox.translateYProperty(), endY, Interpolator.LINEAR)));
    }

    private List<String> parseCreditLines(String json) {
        List<String> lines = new ArrayList<>();

        if (json == null || json.isBlank()) {
            lines.add("# END CREDITS");
            lines.add("No credit data.");
            return lines;
        }

        Matcher titleMatcher = Pattern.compile("\"title\"\\s*:\\s*" + JSON_STRING).matcher(json);
        if (titleMatcher.find()) {
            lines.add("# " + unescapeJson(titleMatcher.group(1)));
            lines.add("");
        }

        Matcher sectionMatcher = Pattern.compile(
                "\"heading\"\\s*:\\s*" + JSON_STRING + "\\s*,\\s*\"lines\"\\s*:\\s*\\[([^\\[\\]]*?)\\]",
                Pattern.DOTALL).matcher(json);

        while (sectionMatcher.find()) {
            lines.add("# " + unescapeJson(sectionMatcher.group(1)));
            Matcher lineMatcher = Pattern.compile(JSON_STRING).matcher(sectionMatcher.group(2));
            while (lineMatcher.find()) {
                lines.add(unescapeJson(lineMatcher.group(1)));
            }
            lines.add("");
        }

        if (lines.isEmpty()) {
            lines.add("# END CREDITS");
            lines.add("No credit data.");
        }

        return lines;
    }

    private String unescapeJson(String text) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                switch (next) {
                    case 'n'  -> { sb.append('\n'); i += 2; }
                    case 't'  -> { sb.append('\t'); i += 2; }
                    case 'r'  -> { sb.append('\r'); i += 2; }
                    case '"'  -> { sb.append('"');  i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    default   -> { sb.append(c);    i++;    }
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
