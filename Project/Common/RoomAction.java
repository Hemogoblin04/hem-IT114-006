package Project.Common;

public enum RoomAction {
    CREATE, JOIN, LEAVE, LIST, JOIN_SPECTATOR;

    public boolean isSpectator(){
        return this == JOIN_SPECTATOR;
    }

}
