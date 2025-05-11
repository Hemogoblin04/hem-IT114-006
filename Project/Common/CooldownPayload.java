package Project.Common;

public class CooldownPayload extends ReadyPayload {
    private boolean cooldownEnabled;
    
    // Getters/setters
 public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    public void setCooldownEnabled(boolean cooldownEnabled) {
        this.cooldownEnabled = cooldownEnabled;
    }
}