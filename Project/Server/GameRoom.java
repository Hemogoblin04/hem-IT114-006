package Project.Server;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;
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

    private Map<Long, String> playerChoices = new HashMap<>();
    private Map<Long, Integer> playerPoints = new HashMap<>();
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

        LoggerUtil.INSTANCE.info("onRoundEnd() end");
        if (round >= 3) {
            onSessionEnd();
        }
        else{
            onRoundStart();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
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

    // end send data to ServerThread(s)

    // misc methods
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

    /**
     * Example turn action
     * 
     * @param currentUser
     */
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
private Map<ServerThread, String> playerMoves = new HashMap<>();

//Helper method to validate the RPS move
private boolean isValidMove(String move) {
    return move.equals("r") || move.equals("p") || move.equals("s");
}
//save moves taken to hasmhmap
private void savePlayerMove(ServerThread user, String move) {
    playerMoves.put(user, move);
}

//revamped design after much hassle, lots of advice and genuine heartbreak HEM 
protected void round() {

    try {
        relay( null,  "Round Start!");
        ArrayList<ServerThread> Clients = new ArrayList<>(
            clientsInRoom.values().stream()
                .filter(sp -> !sp.getEliminated() && sp.isReady())
                .toList()
        );

        for (int i = 0; i < Clients.size(); i++) {
            String choiceOne = playerMoves.get(Clients.get(i));
            String choiceTwo = playerMoves.get(Clients.get((i + 1) % Clients.size()));


            if (choiceOne.equals(choiceTwo)) {
                relay(null, String.format("%s has tied with %s using %s",
                    Clients.get(i).getDisplayName(),
                    Clients.get((i + 1) % Clients.size()).getDisplayName(),
                    choiceOne));
            } else if (
                (choiceOne.equalsIgnoreCase("r") && choiceTwo.equals("s")) ||
                (choiceOne.equalsIgnoreCase("p") && choiceTwo.equals("r")) ||
                (choiceOne.equalsIgnoreCase("s") && choiceTwo.equals("p"))
            ) {
                relay(null, String.format("%s has beaten %s using %s",
                    Clients.get(i).getDisplayName(),
                    Clients.get((i + 1) % Clients.size()).getDisplayName(),
                    choiceOne));
                relay(null, String.format("%s has been eliminated",
                    Clients.get((i + 1) % Clients.size()).getDisplayName()));
                Clients.get(i).changePoints();
                Clients.get((i + 1) % Clients.size()).setEliminated(true);
            } else {
                relay(null, String.format("%s has beaten %s using %s",
                    Clients.get((i + 1) % Clients.size()).getDisplayName(),
                    Clients.get(i).getDisplayName(),
                    choiceTwo));
                Clients.get(i).setEliminated(true);
                relay(null, String.format("%s has been eliminated",
                    Clients.get(i).getDisplayName()));
            }
        }
    } catch (Exception e) {
        LoggerUtil.INSTANCE.severe("Error", e);
    }
}
// end receive data from ServerThread (GameRoom specific)
}
