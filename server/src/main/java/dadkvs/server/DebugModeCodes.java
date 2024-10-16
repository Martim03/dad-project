package dadkvs.server;

public enum DebugModeCodes {
    CRASH(1),
    FREEZE(2),
    UNFREEZE(3),
    SLOW_MODE_ON(4),
    SLOW_MODE_OFF(5);

    private final int code;

    DebugModeCodes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}