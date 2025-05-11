package Project.Client.Views;

import Project.Client.Client;
import java.awt.Color;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class ReadyPanel extends JPanel {
    private JButton readyButton;
    private JButton rps3Button;
    private JButton rps5Button;
    private String currentMode = "RPS3";
    private JCheckBox cooldownToggle; 

    public ReadyPanel(Consumer<String> modeChangeCallback) {

    readyButton = new JButton("Ready");  // Initialize before using

    readyButton.setText("Ready");
    readyButton.addActionListener(event -> {
        try {
            Client.INSTANCE.sendReady();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    });
    this.add(readyButton);
    
    rps3Button = new JButton("RPS");
    rps5Button = new JButton("RPS-5");

    this.add(rps3Button);
    this.add(rps5Button);

    rps3Button.setBorder(new LineBorder(Color.GREEN, 2));
    rps3Button.setBackground(new Color(220, 255, 220));

    rps3Button.addActionListener(e -> setGameMode("RPS3", rps3Button, rps5Button, modeChangeCallback));
    rps5Button.addActionListener(e -> setGameMode("RPS5", rps5Button, rps3Button, modeChangeCallback));

    cooldownToggle = new JCheckBox("Enable 10-second cooldown", true);
        this.add(cooldownToggle);
        cooldownToggle.addActionListener(e -> {
            try {
                Client.INSTANCE.sendCooldownToggle(cooldownToggle.isSelected());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
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
}