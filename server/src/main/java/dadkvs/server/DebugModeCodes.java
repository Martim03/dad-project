package dadkvs.server;

public enum DebugModeCodes {
    CRASH(1),
    FREEZE(2),
    UNFREEZE(3),
    SLOW_MODE_ON(4),
    SLOW_MODE_OFF(5),
    BLOCK_PHASE_ONE(6),
    UNBLOCK_PHASE_ONE(7),
    BLOCK_PHASE_ONE_AND_TWO(8),
    UNBLOCK_PHASE_ONE_AND_TWO(9);

    private final int code;

    DebugModeCodes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
