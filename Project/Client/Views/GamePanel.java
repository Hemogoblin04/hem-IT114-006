package Project.Client.Views;

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Common.Constants;
import Project.Common.Phase;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class GamePanel extends JPanel implements IRoomEvents, IPhaseEvent {

    private JPanel playPanel;
    private CardLayout cardLayout;
    private static final String READY_PANEL = "READY";
    private static final String PLAY_PANEL = "PLAY";// example panel for this lesson
    JPanel buttonPanel = new JPanel();

    @SuppressWarnings("unused")
    public GamePanel(ICardControls controls) {
        super(new BorderLayout());

        buildChoiceButtons();
        
        JPanel gameContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) gameContainer.getLayout();
        this.setName(CardView.GAME_SCREEN.name());
        Client.INSTANCE.addCallback(this);

        ReadyPanel readyPanel = new ReadyPanel(this::onModeChanged);
        readyPanel.setName(READY_PANEL);
        gameContainer.add(READY_PANEL, readyPanel);

        playPanel = new JPanel();
        playPanel.setName(PLAY_PANEL);
        playPanel.add(buttonPanel);
        gameContainer.add(PLAY_PANEL, playPanel);

        GameEventsPanel gameEventsPanel = new GameEventsPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameContainer, gameEventsPanel);
        splitPane.setResizeWeight(0.7);

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.7);
            }
        });

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playPanel.revalidate();
                playPanel.repaint();
            }
        });

        this.add(splitPane, BorderLayout.CENTER);
        controls.addPanel(CardView.CHAT_GAME_SCREEN.name(), this);
        setVisible(false);
    }

private boolean extendedMode = false;

private void onModeChanged(String mode) {
    extendedMode = mode.equals("RPS5");
    buildChoiceButtons();
}


private void buildChoiceButtons() {
    buttonPanel.removeAll();

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

    String[] choices = extendedMode
        ? new String[] { "Rock", "Paper", "Scissors", "Lizard", "Spock" }
        : new String[] { "Rock", "Paper", "Scissors" };

    for (String choice : choices) {
        JButton rpsButton = new JButton(choice);
        rpsButton.addActionListener(event -> {
            try {
                Client.INSTANCE.sendDoTurn(choice.toLowerCase());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        buttonPanel.add(rpsButton);
    }

    JButton awayButton = new JButton("Go Away");
    awayButton.addActionListener(event -> {
        boolean goingAway = awayButton.getText().equals("Go Away");
        awayButton.setText(goingAway ? "Back" : "Go Away");

        try {
            Client.INSTANCE.sendDoTurn(goingAway ? "/away" : "/back");
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
    buttonPanel.add(awayButton);

    buttonPanel.revalidate();
    buttonPanel.repaint();
}


    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (Constants.LOBBY.equals(roomName) && isJoin) {
            setVisible(false);
            revalidate();
            repaint();
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {
        System.out.println("Received phase: " + phase.name());
        if (!isVisible()) {
            setVisible(true);
            getParent().revalidate();
            getParent().repaint();
            System.out.println("GamePanel visible");
        }
        if (phase == Phase.READY) {
            cardLayout.show(playPanel.getParent(), READY_PANEL);
            buttonPanel.setVisible(false);
        } else if (phase == Phase.IN_PROGRESS) {
            cardLayout.show(playPanel.getParent(), PLAY_PANEL);
            buttonPanel.setVisible(true);
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // Not used here, but needs to be defined due to interface
    }
}