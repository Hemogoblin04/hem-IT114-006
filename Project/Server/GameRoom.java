package Project.Server;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.PointsPayload;
import Project.Common.TimedEvent;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;
import Project.Client.Client;
import Project.Client.User;

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
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
        syncTurnStatus(sp);
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

        long remainingPlayers = clientsInRoom.values().stream()
        .filter(p -> p.isReady() && !p.getEliminated())
        .count();
        
       if (remainingPlayers == 0){
        onSessionEnd();
       } else {
        onRoundStart();
       }

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
                .filter(sp -> sp.isReady())
                .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
                // ensure to verify the isReady part since it's against the original list
                .filter(sp -> sp.isReady() && sp.didTakeTurn())
                .toList().size();
        if (numReady == numTookTurn) {
            relay(null,
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // receive data from ServerThread (GameRoom specific)

    protected void handleTurnAction(ServerThread currentUser, String exampleText) {
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
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid move. Please choose r, p, or s.");
            return;
        }

        savePlayerMove(currentUser, Move);
        

        // Mark the player as having taken their turn
        currentUser.setTookTurn(true);

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
//Helper method to validate the RPS move
private boolean isValidMove(String move) {
    if (move == null) return false;  
     move = move.trim().toLowerCase();
    return move.equals("r") || move.equals("p") || move.equals("s");
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


//revamped design after much hassle, lots of advice and genuine heartbreak HEM 
protected void round() {
    try {
        relay(null, "Round Start!");
        ArrayList<ServerThread> Clients = new ArrayList<>(
            clientsInRoom.values().stream()
                .filter(sp -> !sp.getEliminated() && sp.isReady())
                .toList()
        );

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

            if (choiceOne.equalsIgnoreCase(choiceTwo)) {
                relay(null, String.format("%s tied with %s using %s",
                    p1.getDisplayName(), p2.getDisplayName(), choiceOne));
            } else if (
                (choiceOne.equalsIgnoreCase("r") && choiceTwo.equalsIgnoreCase("s")) ||
                (choiceOne.equalsIgnoreCase("p") && choiceTwo.equalsIgnoreCase("r")) ||
                (choiceOne.equalsIgnoreCase("s") && choiceTwo.equalsIgnoreCase("p"))
            ) {
                relay(null, String.format("%s beat %s using %s",
                    p1.getDisplayName(), p2.getDisplayName(), choiceOne));
                p1.changePoints();
                p2.setEliminated(true);
                relay(null, String.format("%s has been eliminated", p2.getDisplayName()));
                syncPoints(p1);
                syncPoints(p2);
            } else {
                relay(null, String.format("%s beat %s using %s",
                    p2.getDisplayName(), p1.getDisplayName(), choiceTwo));
                p2.changePoints();
                p1.setEliminated(true);
                relay(null, String.format("%s has been eliminated", p1.getDisplayName()));
                syncPoints(p1);
                syncPoints(p2);
            }
        }

    } catch (Exception e) {
        LoggerUtil.INSTANCE.severe("Error in round()", e);
    }
}
}