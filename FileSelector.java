import javax.swing.*;
import java.io.File;

public class FileSelector extends JFrame {
    private String path;

    public String getPath() {
        return path;
    }

    public FileSelector() {
        path = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.showDialog(new JLabel(), "select");
        File file = fileChooser.getSelectedFile();
        if(file != null && file.isFile()){
            path = file.getAbsolutePath();
        }
    }

    public static void main(String[] args) {
        FileSelector fs = new FileSelector();
        if(fs.getPath() != null) {
            System.out.println(fs.getPath());
        }
    }
}
