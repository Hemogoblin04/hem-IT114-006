package Project.Client.Views;

import Project.Client.Client;
import java.awt.Color;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class ReadyPanel extends JPanel {
    private JButton rps3Button;
    private JButton rps5Button;
    private String currentMode = "RPS3";

    public ReadyPanel(Consumer<String> modeChangeCallback) {

    

    rps3Button.addActionListener(e -> setGameMode("RPS3", rps3Button, rps5Button, modeChangeCallback));
    rps5Button.addActionListener(e -> setGameMode("RPS5", rps5Button, rps3Button, modeChangeCallback));
}

private void setGameMode(String mode, JButton activeButton, JButton inactiveButton, Consumer<String> callback) {
    if (currentMode.equals(mode)) return;

    try {
        Client.INSTANCE.sendMessage("/mode " + mode);
        currentMode = mode;

        // Update visuals
        activeButton.setBorder(new LineBorder(Color.GREEN, 2));
        activeButton.setBackground(new Color(220, 255, 220));
        inactiveButton.setBorder(new LineBorder(Color.GRAY, 1));
        inactiveButton.setBackground(null);

        // Notify parent
        callback.accept(mode);

    } catch (IOException e) {
        e.printStackTrace();
    }
}


    private void setGameMode(String mode, JButton activeButton, JButton inactiveButton) {
        if (currentMode.equals(mode)) return;

        try {
            Client.INSTANCE.sendMessage("/mode " + mode);
            currentMode = mode;

            // Update button visuals
            activeButton.setBorder(new LineBorder(Color.GREEN, 2));
            activeButton.setBackground(new Color(220, 255, 220));
            inactiveButton.setBorder(new LineBorder(Color.GRAY, 1));
            inactiveButton.setBackground(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
