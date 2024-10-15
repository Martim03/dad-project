package dadkvs.server;

import dadkvs.util.PaxosLog;
import dadkvs.util.RequestArchiveStore;

public class PaxosParticipant {
    private DadkvsServerState state;
    private RequestArchiveStore requestArchiveStore;
    private PaxosLog paxosLog;

    public PaxosParticipant(DadkvsServerState state, RequestArchiveStore requestArchiveStore, PaxosLog paxosLog) {
        this.state = state;
        this.requestArchiveStore = requestArchiveStore;
        this.paxosLog = paxosLog;
    }

    public DadkvsServerState getServerState() {
        return state;
    }

    public RequestArchiveStore getRequestArchiveStore() {
        return requestArchiveStore;
    }

    public PaxosLog getPaxosLog() {
        return paxosLog;
    }
}
