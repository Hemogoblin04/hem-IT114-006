package Project.Common;

import java.util.List;

public class ElimPayload extends Payload {

    private List<String> eliminatedPlayers;

    public ElimPayload(List<String> eliminatedPlayers) {
        this.eliminatedPlayers = eliminatedPlayers;
    }

    public List<String> getEliminatedPlayers() {
        return eliminatedPlayers;
    }

    public String toString() {
        return super.toString() + String.format(" eliminated players = %d", eliminatedPlayers);
    }
}