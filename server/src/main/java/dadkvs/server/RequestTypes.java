package dadkvs.server;

public enum RequestTypes {
    PHASE_ONE_REQUEST(1),
    PHASE_TWO_REQUEST(2),
    LEARN_REQUEST(3),
    CLIENT_REQUEST(4);

    private final int code;

    RequestTypes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
