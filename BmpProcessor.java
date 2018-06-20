import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class BmpProcessor extends JFrame implements KeyListener {
    private int height; //the number of rows of pixels
    private int width; //the number of columns of pixels
    private FileInputStream fis;
    private BufferedInputStream bis;
    private int[][] red; //red[i][j] is the value of red channel of the pixel[i][j]
    private int[][] green;
    private int[][] blue;
    private int[] redCount; //red[i] is the number of pixels whose value of red channel is i
    private int[] greenCount;
    private int[] blueCount;
    private boolean reversed; //whether the image is bottom-up
    private long emptyBytes; //the last bytes of a row may be meaningless
    private int flag; //for panel refreshment
    private DrawPanel drawPanel;

    public BmpProcessor(String fileName) throws IOException {
        fis = new FileInputStream(fileName);
        bis = new BufferedInputStream(fis);
        flag = 0;

        byte[] header = new byte[54];
        bis.read(header, 0, 54); //The first 54 bytes are header
        width = bytesToInt(header, 21); //get the width of the image
        height = bytesToInt(header, 25); // get the height of the image

        if(((int) header[25] & 0x80) == 0) {
            reversed = true; //if the first bit of the 25th byte is 0, the image is reversed (bottom-up)
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

        this.addKeyListener(this);
    }

    public int bytesToInt(byte[] bytes, int offset) {
        return ((int) bytes[offset] & 0xff) << 24 |
                ((int) bytes[offset - 1] & 0xff) << 16 |
                ((int) bytes[offset - 2] & 0xff) << 8 |
                ((int) bytes[offset - 3] & 0xff);
    }

    public void getRGB() throws IOException { //read the data area to get RGB values of each pixel
        if(reversed) { //if the image is reversed, read from bottom to top
            for(int i = height - 1; i >= 0; i--) {
                for(int j = 0; j < width; j++) {
                    blue[i][j] = bis.read();
                    green[i][j] = bis.read();
                    red[i][j] = bis.read();

                    blueCount[blue[i][j]]++;
                    greenCount[green[i][j]]++;
                    redCount[red[i][j]]++;
                }

                if(emptyBytes != 0) { //skip empty bytes
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

    public YCbCr rgbToYCbCr(int r, int g, int b) { //rgb -> ycbcr
        int y = (int)(0.299 * r + 0.587 * g + 0.114 * b);
        int cb = (int)(-0.1687 * r - 0.3313 * g + 0.5 * b) + 128;
        int cr = (int)(0.5 * r - 0.4187 * g - 0.0813 * b) + 128;

        return new YCbCr(y, cb, cr);
    }

    public RGB yCbCrToRGB(int y, int cb, int cr) { //ycbcr -> rgb
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

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(drawPanel != null) {
            flag = (flag + 1) % 5; //5 patterns, press any key to change into next pattern
            if(flag != 1) { //image patterns, so the size is the same as the image
                this.setSize(width, height);
                drawPanel.setPreferredSize(new Dimension(width, height));
            } else { //show the histogram
                this.setSize(1084, 700);
                drawPanel.setPreferredSize(new Dimension(1084, 700));
            }
            drawPanel.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public void showBMP() {
        this.setTitle("BmpProcessor");
        this.setSize(width, height);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        this.setResizable(true);
        this.setVisible(true);

        drawPanel = new DrawPanel();

        this.add(drawPanel);
    }

    public void drawOriginal(Graphics g) { //draw the original image
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                g.setColor(new Color(red[i][j], green[i][j], blue[i][j]));
                g.fillRect(j, i, 1, 1);
            }
        }
    }

    public void drayHistogram(Graphics g) { //draw the histogram of red channel, green channel and blue channel
        int width = getWidth();
        int height = getHeight();

        int part = (height - 100) / 3; //divide the panel into 3 parts to draw 3 histograms (red, green, blue)
        double intervalX = (width - 60) / 256.0; //the width of a rectangle

        int redMaxValue = 0, greenMaxValue = 0, blueMaxValue = 0;
        for(int i = 0; i < 256; i++) {
            if(redCount[i] > redMaxValue) {
                redMaxValue = redCount[i];
            }
            if(greenCount[i] > greenMaxValue) {
                greenMaxValue = greenCount[i];
            }
            if(blueCount[i] > blueMaxValue) {
                blueMaxValue = blueCount[i];
            }
        }

        int lineBetweenRedAndGreen = part; //part 1 for red
        int lineBetweenGreenAndBlue = 2 * part; //part 2 for green
        int bottom = 3 * part; //part 3 for blue

        g.setColor(Color.RED);
        double intervalY = part / 1.2 / redMaxValue;
        for(int i = 0; i < 256; i++) { //draw the histogram of the red channel
            g.fillRect((int)(30 + intervalX * i), (int)(lineBetweenRedAndGreen - intervalY * redCount[i]), (int)(intervalX), (int)(intervalY * redCount[i]));
        }

        g.setColor(Color.GREEN);
        intervalY = part / 1.2 / greenMaxValue;
        for(int i = 0; i < 256; i++) { //draw the histogram of the green channel
            g.fillRect((int)(30 + intervalX * i), (int)(lineBetweenGreenAndBlue - intervalY * greenCount[i]), (int)(intervalX), (int)(intervalY * greenCount[i]));
        }

        g.setColor(Color.BLUE);
        intervalY = part / 1.2 / blueMaxValue;
        for(int i = 0; i < 256; i++) { //draw the histogram of the blue channel
            g.fillRect((int)(30 + intervalX * i), (int)(bottom - intervalY * blueCount[i]), (int)(intervalX), (int)(intervalY * blueCount[i]));
        }
    }

    public void drawBrighter(Graphics g) { //draw the image that is 1.5 times brighter
        YCbCr yCbCr;
        RGB rgb;

        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                yCbCr = rgbToYCbCr(red[i][j], green[i][j], blue[i][j]);
                int y = yCbCr.y;
                int cb = yCbCr.cb;
                int cr = yCbCr.cr;

                y = (int)(1.5 * y); //y is the luminance, 1.5 times brighter means y * 1.5
                y = Integer.min(y, 255);

                rgb = yCbCrToRGB(y, cb, cr);

                g.setColor(new Color(rgb.r, rgb.g, rgb.b));
                g.fillRect(j, i, 1, 1);
            }
        }
    }

    public void drawGrayScale(Graphics g) {
        YCbCr yCbCr;

        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                yCbCr = rgbToYCbCr(red[i][j], green[i][j], blue[i][j]);
                int y = yCbCr.y;

                g.setColor(new Color(y, y, y)); //grayscale image is just to reserve the luminance
                g.fillRect(j, i, 1, 1);
            }
        }
    }

    public void drawOrderedDithering(Graphics g) {
        int[][] matrix = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
        //the dithering matrix is 4 by 4. Thus, 0-15 are mapped to 0, 16-31 are mapped to 1, 32-47 are mapped to 2...

        YCbCr yCbCr;

        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                yCbCr = rgbToYCbCr(red[i][j], green[i][j], blue[i][j]);

                if(yCbCr.y / 16 < matrix[i % matrix.length][j % matrix[0].length]) {
                    g.setColor(new Color(0, 0, 0));
                } else {
                    g.setColor(new Color(255, 255, 255));
                }

                g.fillRect(j, i, 1, 1);
            }
        }
    }

    public class DrawPanel extends JPanel {
        public void paint(Graphics g) {
            super.paint(g);
            if(flag == 0) { //change the pattern according to flag
                drawOriginal(g);
            } else if(flag == 1) {
                drayHistogram(g);
            } else if(flag == 2) {
                drawBrighter(g);
            } else if(flag == 3) {
                drawGrayScale(g);
            } else {
                drawOrderedDithering(g);
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

