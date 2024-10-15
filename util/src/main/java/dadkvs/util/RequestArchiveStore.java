package dadkvs.util;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import dadkvs.DadkvsMain;
import dadkvs.DadkvsMain.CommitReply;
import dadkvs.DadkvsMain.CommitRequest;

public class RequestArchiveStore {
    private ConcurrentHashMap<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> archiveStore;

    public RequestArchiveStore() {
        this.archiveStore = new ConcurrentHashMap<>();
    }

    /** (Leaner POV) Called by Learner: Get Request to be executed */
    public RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> getRequest(int reqId) {
        return archiveStore.get(reqId);
    }

    /** (Learner POV) Called by Client Transaction: Store Request Archive */
    public void addRequest(RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive) {
        archiveStore.put(reqArchive.getReqId(), reqArchive);
    }

    /** (Proposer POV) Called by Proposer: Get next Value to propose (it can be null) */
    public RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> getNext() {
        if (archiveStore.isEmpty()) {
            return null;
        }

        Iterator<RequestArchive<CommitRequest, CommitReply>> iter = archiveStore.values().iterator();
        while (iter.hasNext()) {
            RequestArchive<CommitRequest, CommitReply> reqArchive = iter.next();
            if (reqArchive.isProposable()) {
                return reqArchive; // Return the first proposable Request
            }
        }

        return null;
    }

    /** (Leaner POV) Called by Learner: Remove Request after it gets executed (memory saving purposes) */
    public void removeRequest(int reqId) {
        archiveStore.remove(reqId);
    }
}
