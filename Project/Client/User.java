package Project.Client;

import Project.Common.Constants;

public class User {
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;
    private boolean isReady = false;
    private boolean tookTurn = false;
    private int points = 0;
    private boolean isEliminated = false;
    private boolean away = false;
    private String choice; 

    public int getPoints() {
        return points;
    }

    /**
     * Changes points by specified amount (positive or negative)
     * @param points the points to add/subtract
     */
    public void changePoints(int points) {
        if (!isEliminated) {  // Only change points if not eliminated
            this.points += points;
            if (this.points < 0) {
                this.points = 0;
            }
        }
    }

    /**
     * Sets points to exact value
     * @param points the points to set
     */
    public void setPoints(int points) {
        if (!isEliminated) {  // Only set points if not eliminated
            this.points = points;
            if (this.points < 0) {
                this.points = 0;
            }
        }
    }

    /**
     * Increments points by 1 (for wins)
     */
    public void incrementPoints() {
        if (!isEliminated) {
            this.points++;
        }
    }

    // Remove the duplicate changePoints() method

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String username) {
        this.clientName = username;
    }

    public String getDisplayName() {
        return String.format("%s#%s", this.clientName, this.clientId);
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public void reset() {
        this.clientId = Constants.DEFAULT_CLIENT_ID;
        this.clientName = null;
        this.isReady = false;
        this.tookTurn = false;
    }

    public boolean didTakeTurn() {
        return tookTurn;
    }

    public void setTookTurn(boolean tookTurn) {
        this.tookTurn = tookTurn;
    }

    public boolean isEliminated() {
        return isEliminated;
    }

    public void setEliminated(boolean isEliminated) {
        this.isEliminated = isEliminated;
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public boolean isAway() {
        return away;
    }

    public void setAway(boolean awayStatus) {
        this.away = awayStatus;
    }
    // Remove the typo method gettEliminated()
}