import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class BmpProcessor extends JFrame {
    private int height;
    private int width;
    private FileInputStream fis;
    private BufferedInputStream bis;
    private int[][] red;
    private int[][] green;
    private int[][] blue;
    private int[] redCount;
    private int[] greenCount;
    private int[] blueCount;
    private boolean reversed;
    private long emptyBytes;

    public BmpProcessor(String fileName) throws IOException {
        fis = new FileInputStream(fileName);
        bis = new BufferedInputStream(fis);

        byte[] header = new byte[54];
        bis.read(header, 0, 54);
        width = bytesToInt(header, 21);
        height = bytesToInt(header, 25);

        if(((int) header[25] & 0x80) == 0) {
            reversed = true;
        } else {
            reversed = false;
        }

        red = new int[height][width];
        green = new int[height][width];
        blue = new int[height][width];
        redCount = new int[256];
        greenCount = new int[256];
        blueCount = new int[256];

        if(width * 3 % 4 != 0) {
            emptyBytes = 4 - (width * 3 % 4);
        } else {
            emptyBytes = 0;
        }
    }

    public int bytesToInt(byte[] bytes, int offset) {
        if(((int) bytes[offset] & 0x80) == 0) {
            return ((int) bytes[offset] & 0xff) << 24 |
                    ((int) bytes[offset - 1] & 0xff) << 16 |
                    ((int) bytes[offset - 2] & 0xff) << 8 |
                    ((int) bytes[offset - 3] & 0xff);
        } else {
            int value1 = ((bytes[offset] ^ 0xff) & 0xff) << 24;
            int value2 = ((bytes[offset - 1] ^ 0xff) & 0xff) << 16;
            int value3 = ((bytes[offset - 2] ^ 0xff) & 0xff) << 8;
            int value4 = ((bytes[offset - 3] ^ 0xff) & 0xff);
            return (value1 | value2 | value3 | value4) + 1;
        }
    }

    public void getRGB() throws IOException {
        if(reversed) {
            for(int i = height - 1; i >= 0; i--) {
                for(int j = 0; j < width; j++) {
                    blue[i][j] = bis.read();
                    green[i][j] = bis.read();
                    red[i][j] = bis.read();

                    blueCount[blue[i][j]]++;
                    greenCount[green[i][j]]++;
                    redCount[red[i][j]]++;
                }

                if(emptyBytes != 0) {
                    bis.skip(emptyBytes);
                }
            }
        } else {
            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    blue[i][j] = bis.read();
                    green[i][j] = bis.read();
                    red[i][j] = bis.read();

                    blueCount[blue[i][j]]++;
                    greenCount[green[i][j]]++;
                    redCount[red[i][j]]++;
                }

                if(emptyBytes != 0) {
                    bis.skip(emptyBytes);
                }
            }
        }

        fis.close();
        bis.close();
    }

    public YCbCr rgbToYCbCr(int r, int g, int b) {
        int y = (int)(0.299 * r + 0.587 * g + 0.114 * b);
        int cb = (int)(-0.1687 * r - 0.3313 * g + 0.5 * b) + 128;
        int cr = (int)(0.5 * r - 0.4187 * g - 0.0813 * b) + 128;

        return new YCbCr(y, cb, cr);
    }

    public RGB yCbCrToRGB(int y, int cb, int cr) {
        int r = (int)(y + 1.402 * (cr - 128));
        int g = (int)(y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128));
        int b = (int)(y + 1.772 * (cb - 128));

        r = Integer.min(r, 255);
        g = Integer.min(g, 255);
        b = Integer.min(b, 255);

        r = Integer.max(r, 0);
        g = Integer.max(g, 0);
        b = Integer.max(b, 0);

        return new RGB(r, g, b);
    }

    public void showBMP() {
        this.setTitle("Test");
        this.setSize(1024, 600);
        this.setDefaultCloseOperation(3);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

//        Original original = new Original();
//        Dimension dimension = new Dimension(width, height);
//        original.setPreferredSize(dimension);
//        this.add(original);
//        this.setVisible(true);

//        Brighter brighter = new Brighter();
//        Dimension dimension = new Dimension(width, height);
//        brighter.setPreferredSize(dimension);
//        this.add(brighter);
//        this.setVisible(true);

//        GrayScale grayScale = new GrayScale();
//        Dimension dimension = new Dimension(width, height);
//        grayScale.setPreferredSize(dimension);
//        this.add(grayScale);
//        this.setVisible(true);

//        OrderedDithering orderedDithering = new OrderedDithering();
//        Dimension dimension = new Dimension(width, height);
//        orderedDithering.setPreferredSize(dimension);
//        this.add(orderedDithering);
//        this.setVisible(true);

        Histogram histogram = new Histogram();
        Dimension dimension = new Dimension(1024, 600);
        histogram.setPreferredSize(dimension);
        this.add(histogram);
        this.setVisible(true);
    }

    public class Original extends JPanel {
        public void paint(Graphics g) {
            super.paint(g);
            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    g.setColor(new Color(red[i][j], green[i][j], blue[i][j]));
                    g.fillRect(j, i, 1, 1);
                }
            }
        }
    }

    public class Histogram extends JPanel {
        public void paint(Graphics g) {
            super.paint(g);
            int width = getWidth();
            int height = getHeight();
            int part = height / 3;
            double intervalX = width / 256.0;

            int redMaxValue = 0, greenMaxValue = 0, blueMaxValue = 0;
            int redIndex = 0, blueIndex = 0, greenIndex = 0;
            for(int i = 0; i < 256; i++) {
                if(redCount[i] > redMaxValue) {
                    redIndex = i;
                    redMaxValue = redCount[i];
                }
                if(greenCount[i] > greenMaxValue) {
                    greenIndex = i;
                    greenMaxValue = greenCount[i];
                }
                if(blueCount[i] > blueMaxValue) {
                    blueIndex = i;
                    blueMaxValue = blueCount[i];
                }
            }

            int lineBetweenRedAndGreen = part;
            int lineBetweenGreenAndBlue = 2 * part;
            int bottom = 3 * part;

            System.out.println(blueCount[0] + " " + blueCount[1] + " " + blueCount[2]);
            System.out.println(redMaxValue + " " + greenMaxValue + " " + blueMaxValue);
            System.out.println(redIndex + " " + greenIndex + " " + blueIndex);

            g.setColor(Color.RED);
            double intervalY = part / 1.2 / redMaxValue;
            for(int i = 0; i < 256; i++) {
                g.fillRect((int)(intervalX * i), (int)(lineBetweenRedAndGreen - intervalY * redCount[i]), (int)(intervalX), (int)(intervalY * redCount[i]));
            }

            g.setColor(Color.GREEN);
            intervalY = part / 1.2 / greenMaxValue;
            for(int i = 0; i < 256; i++) {
                g.fillRect((int)(intervalX * i), (int)(lineBetweenGreenAndBlue - intervalY * greenCount[i]), (int)(intervalX), (int)(intervalY * greenCount[i]));
            }

            g.setColor(Color.BLUE);
            intervalY = part / 1.2 / blueMaxValue;
            for(int i = 0; i < 256; i++) {
                g.fillRect((int)(intervalX * i), (int)(bottom - intervalY * blueCount[i]), (int)(intervalX), (int)(intervalY * blueCount[i]));
            }
        }
    }

    public class Brighter extends JPanel {
        int[][] y = new int[height][width];
        int[][] cr = new int[height][width];
        int[][] cb = new int[height][width];

        public void paint(Graphics g) {
            super.paint(g);
            YCbCr yCbCr;
            RGB rgb;
            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    yCbCr = rgbToYCbCr(red[i][j], green[i][j], blue[i][j]);
                    int y = yCbCr.y;
                    int cb = yCbCr.cb;
                    int cr = yCbCr.cr;

                    y = (int)(1.5 * y);
                    y = Integer.min(y, 255);

                    rgb = yCbCrToRGB(y, cb, cr);

                    g.setColor(new Color(rgb.r, rgb.g, rgb.b));
                    g.fillRect(j, i, 1, 1);
                }
            }
        }
    }

    public class GrayScale extends JPanel {
        int[][] y = new int[height][width];

        public void paint(Graphics g) {
            super.paint(g);
            YCbCr yCbCr;

            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    yCbCr = rgbToYCbCr(red[i][j], green[i][j], blue[i][j]);
                    int y = yCbCr.y;

                    g.setColor(new Color(y, y, y));
                    g.fillRect(j, i, 1, 1);
                }
            }
        }
    }

    public class OrderedDithering extends JPanel {
        int[][] matrix =
                {
                        {
                            0, 192, 48, 240, 12, 204, 60, 252, 3, 195, 51, 243, 15, 207, 63, 255
                        },
                        {
                            128, 64, 176, 112, 140, 76, 188, 124, 131, 67, 179, 115, 143, 79, 191, 127
                        },
                        {
                            32, 224, 16, 208, 44, 236, 28, 220, 35, 227, 19, 211, 47, 239, 31, 223
                        },
                        {
                            160, 96, 144, 80, 172, 108, 156, 92, 163, 99, 147, 83, 175, 111, 159, 95
                        },
                        {
                            8, 200, 56, 248, 4, 196, 52, 244, 11, 203, 59, 251, 7, 199, 55, 247
                        },
                        {
                            136, 72, 184, 120, 132, 68, 180, 116, 139, 75, 187, 123, 135, 71, 183, 119
                        },
                        {
                            40, 232, 24, 216, 36, 228, 20, 212, 43, 235, 27, 219, 39, 231, 23, 215
                        },
                        {
                            168, 104, 152, 88, 164, 100, 148, 84, 171, 107, 155, 91, 167, 103, 151, 87
                        },
                        {
                            2, 194, 50, 242, 14, 206, 62, 254, 1, 193, 49, 241, 13, 205, 61, 253
                        },
                        {
                            130, 66, 178, 114, 142, 78, 190, 126, 129, 65, 177, 113, 141, 77, 189, 125
                        },
                        {
                            34, 226, 18, 210, 46, 238, 30, 222, 33, 225, 17, 209, 45, 237, 29, 221
                        },
                        {
                            162, 98, 146, 82, 174, 110, 158, 94, 161, 97, 145, 81, 173, 109, 157, 93
                        },
                        {
                            10, 202, 58, 250, 6, 198, 54, 246, 9, 201, 57, 249, 5, 197, 53, 245
                        },
                        {
                            138, 74, 186, 122, 134, 70, 182, 118, 137, 73, 185, 121, 133, 69, 181, 117
                        },
                        {
                            42, 234, 26, 218, 38, 230, 22, 214, 41, 233, 25, 217, 37, 229, 21, 213
                        },
                        {
                            170, 106, 154, 90, 166, 102, 150, 86, 169, 105, 153, 89, 165, 101, 149, 85
                        }
                };

        public void paint(Graphics g) {
            super.paint(g);
            YCbCr yCbCr;

            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    yCbCr = rgbToYCbCr(red[i][j], green[i][j], blue[i][j]);

                    if(yCbCr.y < matrix[i % matrix.length][j % matrix[0].length]) {
                        g.setColor(new Color(0, 0, 0));
                    } else {
                        g.setColor(new Color(255, 255, 255));
                    }

                    g.fillRect(j, i, 1, 1);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            FileSelector fs = new FileSelector();
            if(fs.getPath() != null && fs.getPath().endsWith(".bmp")) {
                BmpProcessor bp = new BmpProcessor(fs.getPath());
                bp.getRGB();
                bp.showBMP();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class YCbCr {
    public int y;
    public int cb;
    public int cr;

    public YCbCr(int y, int cb, int cr) {
        this.y = y;
        this.cb = cb;
        this.cr = cr;
    }
}

class RGB {
    public int r;
    public int g;
    public int b;

    public RGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}

