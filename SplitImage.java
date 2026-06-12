import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SplitImage {
    public static void main(String[] args) {
        try {
            File inputFile = new File("images/nine-bunkatu.png");
            if (!inputFile.exists()) {
                System.out.println("エラー: 画像が見つかりません - " + inputFile.getAbsolutePath());
                return;
            }

            BufferedImage img = ImageIO.read(inputFile);
            int width = img.getWidth();
            int height = img.getHeight();

            System.out.println("元の画像サイズ: " + width + "x" + height);

            // 3等分できるようにサイズを調整（余りを切り捨て）
            int newWidth = width - (width % 3);
            int newHeight = height - (height % 3);

            if (newWidth != width || newHeight != height) {
                System.out.println("3等分するため、" + newWidth + "x" + newHeight + " にリサイズ（クロップ）します");
                img = img.getSubimage(0, 0, newWidth, newHeight);
            }

            int pieceWidth = newWidth / 3;
            int pieceHeight = newHeight / 3;

            System.out.println("1ピースのサイズ: " + pieceWidth + "x" + pieceHeight);

            int index = 1;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    BufferedImage piece = img.getSubimage(col * pieceWidth, row * pieceHeight, pieceWidth, pieceHeight);
                    File outputFile = new File("images/piece_" + index + ".png");
                    ImageIO.write(piece, "png", outputFile);
                    System.out.println("保存しました: " + outputFile.getPath());
                    index++;
                }
            }
            
            System.out.println("画像の9分割処理が正常に完了しました！");

        } catch (IOException e) {
            System.out.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
