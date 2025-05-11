package Project.Server;

import Project.Common.Constants;
import Project.Common.Gamemode.GameMode;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;

    private int round = 0;

    private Map<ServerThread, String> playerChoices = new HashMap<>();
    private Map<Long, Boolean> playerEliminated = new HashMap<>();

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
       if (sp.isSpectator()) {
            syncCurrentPhase(sp);
            sp.setReady(false);
        } else {
            syncCurrentPhase(sp);
            syncReadyStatus(sp);
            syncTurnStatus(sp);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    // timer handlers
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> System.out.println("Round Time: " + time));
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, () -> onTurnEnd());
        turnTimer.setTickCallback((time) -> System.out.println("Turn Time: " + time));
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        round = 0;
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        round++;
        // relay(null, String.format("Round %d has started", round));
        resetRoundTimer();
        resetTurnStatus();
        LoggerUtil.INSTANCE.info("onRoundStart() end");
        onTurnStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart() {

        playerChoices.clear();

        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring

        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer(); // reset timer if round ended without the time expiring

        round();
        playerChoices.clear();
        resetTurnStatus();

    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        leaderBoard();
        
        clientsInRoom.values().forEach(this::syncPoints);

        resetReadyStatus();
        resetTurnStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }
    // end lifecycle methods

    // send/sync data to ServerUser(s)
    private void sendResetTurnStatus() {
        clientsInRoom.values().forEach(spInRoom -> {
            boolean failedToSend = !spInRoom.sendResetTurnStatus();
            if (failedToSend) {
                removeClient(spInRoom);
            }
        });
    }

    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(client.getClientId(), client.didTakeTurn());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void syncTurnStatus(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendTurnStatus(serverUser.getClientId(),
                        serverUser.didTakeTurn(), true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    //end send data to ServerThread(s)

    //misc methods
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(sp -> {
            sp.setTookTurn(false);
        });
        sendResetTurnStatus();
    }

    private void checkAllTookTurn() {
         int numReady = clientsInRoom.values().stream()
            .filter(sp -> sp.isReady() && !sp.isSpectator() && !sp.isAway())
            .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
            .filter(sp -> sp.isReady() && sp.didTakeTurn() && !sp.isSpectator() && !sp.isAway())
            .toList().size();
        if (numReady == numTookTurn && numReady > 0) {
            relay(null,
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // receive data from ServerThread (GameRoom specific)


    protected void handleTurnAction(ServerThread currentUser, String exampleText) {
        if (exampleText.equals("/away") || exampleText.equals("/back")) {
        currentUser.setAway(exampleText.equals("/away"));
        return;
        }

        if (currentUser.isSpectator()) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot participate in the game");
            return;
        }
        // check if the client is in the room
        try {
            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.IN_PROGRESS);
            checkIsReady(currentUser);

            if (currentUser.didTakeTurn()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have already taken your turn this round");
                return;
            }
        
        String Move = exampleText.trim().toLowerCase(); 
        if (!isValidMove(Move)) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid move. Please choose");
            return;
        }

        savePlayerMove(currentUser, Move);
        

        // Mark the player as having taken their turn
        currentUser.setTookTurn(true);

        if (currentUser.getEliminated()) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have been eliminated and cannot play.");
            return;
        }        

        // Send turn status
        sendTurnStatus(currentUser, currentUser.didTakeTurn());

        // Check if all players have taken their turn
        checkAllTookTurn();
        }
        catch(NotReadyException e){
            // The check method already informs the currentUser
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } 
        catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PhaseMismatchException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only take a turn during the IN_PROGRESS phase");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        }
    }


//Methods added by me 
//sure
//redone validation method to also check for RPS5

// In Project.Server.GameRoom
public void handleReady(ServerThread player) {
    try {
        checkPlayerInRoom(player);
        checkCurrentPhase(player, Phase.READY);
        
        if (player.isSpectator()) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot ready up");
            return;
        }
        
        boolean newReadyState = !player.isReady();
        player.setReady(newReadyState);
        broadcastReadyStatus(player, newReadyState);
        checkAllReady();
    } catch (Exception e) {
        LoggerUtil.INSTANCE.severe("Ready error", e);
    }
}

private void broadcastReadyStatus(ServerThread player, boolean isReady) {
    clientsInRoom.values().removeIf(sp -> {
        boolean failed = !sp.sendReadyStatus(player.getClientId(), isReady);
        if (failed) removeClient(sp);
        return failed;
    });
}

private void checkAllReady() {
long readyPlayers = clientsInRoom.values().stream()
        .filter(sp -> !sp.isSpectator() && !sp.isAway() && sp.isReady())
        .count();
    
    long totalPlayers = clientsInRoom.values().stream()
        .filter(sp -> !sp.isSpectator() && !sp.isAway())
        .count();
        
    if (readyPlayers == totalPlayers && totalPlayers > 0) {
        onSessionStart();
    }
}

private boolean isValidMove(String move) {
        if (currentGameMode == GameMode.RPS3) {
            return move.equals("rock") || move.equals("paper") || move.equals("scissors");
        } else {
            return move.equals("rock") || move.equals("paper") || move.equals("scissors") 
                || move.equals("lizard") || move.equals("spock");
        }
    }

//actual use of points payload to allow a client to know there points at the end of the round
protected void syncPoints(ServerThread p){
    p.sendPoints(p.getPoints());
}
//saves player moves
private void savePlayerMove(ServerThread currentUser, String move) {
    playerChoices.put(currentUser, move);
}

//Scoreboard method to braodcast results at the end of a session
private void leaderBoard() {
    StringBuilder sb = new StringBuilder("Current Scores:\n");
    for (ServerThread user : clientsInRoom.values()) {
        sb.append(String.format("%s: %d points\n", user.getDisplayName(), user.getPoints()));
    }
    relay(null, sb.toString());
}

    private GameMode currentGameMode = GameMode.RPS3; // default to RPS3
    
    // Add getter and setter
    public GameMode getGameMode() {
        return currentGameMode;
    }
    
    public void setGameMode(GameMode mode) {
        this.currentGameMode = mode;
    }
    
    // Modify your handleMessage or add new method to process mode selection
    public void handleGameModeChange(ServerThread user, String mode) {
        try {
            GameMode selectedMode = GameMode.valueOf(mode.toUpperCase());
            this.currentGameMode = selectedMode;
            user.sendMessage(Constants.DEFAULT_CLIENT_ID, 
                String.format("Game mode changed to %s", selectedMode));
        } catch (IllegalArgumentException e) {
            user.sendMessage(Constants.DEFAULT_CLIENT_ID, 
                "Invalid game mode. Use RPS3 or RPS5");
        }
    }


//Seperated round logic to make it easeier to change and test
private boolean determineWinner(String move1, String move2) {
        if (currentGameMode == GameMode.RPS3) {
            return (move1.equals("rock") && move2.equals("scissors")) ||
                   (move1.equals("paper") && move2.equals("rock")) ||
                   (move1.equals("scissors") && move2.equals("paper"));
        } else {
            return (move1.equals("rock") && (move2.equals("scissors") || move2.equals("lizard"))) ||
                   (move1.equals("paper") && (move2.equals("rock") || move2.equals("spock"))) ||
                   (move1.equals("scissors") && (move2.equals("paper") || move2.equals("lizard"))) ||
                   (move1.equals("lizard") && (move2.equals("paper") || move2.equals("spock"))) ||
                   (move1.equals("spock") && (move2.equals("rock") || move2.equals("scissors")));
        }
    }

    //helper method to assign points
    private void awardWin(ServerThread winner, ServerThread loser, String winningMove, String losingMove) {
        winner.changePoints();
        loser.setEliminated(true);
        relay(null, String.format("%s's %s beats %s's %s", 
            winner.getDisplayName(), winningMove,
            loser.getDisplayName(), losingMove));
        syncPoints(winner);
        syncPoints(loser);
    }

    public void setSpectator(ServerThread user, boolean isSpectator) {
        user.setSpectator(isSpectator);
        if (isSpectator) {
            // Reset any game-related states
            user.setReady(false);
            user.setTookTurn(false);
            user.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are now a spectator");
        } else {
            user.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are now a player");
        }
        // Notify other clients if needed
    }

//revamped design after much hassle, lots of advice and genuine heartbreak HEM 
protected void round() {
    try {
        relay(null, "Round Start!");
        ArrayList<ServerThread> Clients = new ArrayList<>(
            clientsInRoom.values().stream()
                .filter(sp -> !sp.getEliminated() && sp.isReady() && !sp.isAway() && !sp.isSpectator())
                .toList()
        );



        // Process all player pairs first
        for (int i = 0; i < Clients.size(); i += 2) {
            if (i + 1 >= Clients.size()) {
                relay(null, String.format("%s had no opponent this round", Clients.get(i).getDisplayName()));
                continue;
            }

            ServerThread p1 = Clients.get(i);
            ServerThread p2 = Clients.get(i + 1);

            String choiceOne = playerChoices.get(p1);
            String choiceTwo = playerChoices.get(p2);

            if (choiceOne == null || choiceTwo == null) {
                relay(null, String.format("One or both players did not submit a move: %s (%s), %s (%s)",
                    p1.getDisplayName(), choiceOne,
                    p2.getDisplayName(), choiceTwo
                ));
                continue;
            }

        if (determineWinner(choiceOne, choiceTwo)) {
                awardWin(p1, p2, choiceOne, choiceTwo);
            } else {
            awardWin(p2, p1, choiceTwo, choiceOne);
            }
    }
        // Moved this check outside the loop to process all pairs first
        long activePlayers = clientsInRoom.values().stream()
            .filter(sp -> sp.isReady() && !sp.getEliminated())
            .count();

        if (activePlayers <= 1) {
            relay(null, "Game over!");
            
            // show the winner
            clientsInRoom.values().stream()
                .filter(sp -> sp.isReady() && !sp.getEliminated())
                .findFirst()
                .ifPresent(winner -> relay(null, winner.getDisplayName() + " is the winner!"));

            onSessionEnd();
        } else {
            onRoundStart(); // continue the game
        }

    } catch (Exception e) {
        LoggerUtil.INSTANCE.severe("Error in round()", e);
    }
}
}