package Project.Common;

public class AwayPayload extends Payload {
    private boolean away;

    public AwayPayload() {
        setPayloadType(PayloadType.AWAY); 
    }

    public boolean isAway() {
        return away;
    }

    public void setAway(boolean awayStatus) {
        this.away = awayStatus;
    }

    @Override
    public String toString() {
        return String.format("AwayPayload[ClientId: %s, Away: %s]", 
            getClientId(), 
            away ? "YES" : "NO");
    }
}