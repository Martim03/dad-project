package dadkvs.server;

import dadkvs.DadkvsMain;
import dadkvs.util.PaxosOrdered;
import dadkvs.util.RequestArchive;
import dadkvs.util.RequestArchiveStore;
import io.grpc.stub.StreamObserver;

// TODO must implement debugmodes do enunciado !!!

public class CommitHandler {

    private int requestsProcessed;
    private PaxosOrdered paxosOrdered;
    private RequestArchiveStore requestArchiveStore;
    private DadkvsServerState server_state;

    public CommitHandler(DadkvsServerState state) {
        this.requestsProcessed = 0;
        this.paxosOrdered = new PaxosOrdered();
        this.requestArchiveStore = new RequestArchiveStore();
        this.server_state = state;
    }

    public synchronized void handleCommits() {
        Integer reqId;
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive;

        while ((reqId = paxosOrdered.getCommitedRequestId(requestsProcessed)) != null
                && (reqArchive = requestArchiveStore.getRequest(reqId)) != null) {
            DadkvsMain.CommitRequest request = reqArchive.getRequest();

            int key1 = request.getKey1();
            int version1 = request.getVersion1();
            int key2 = request.getKey2();
            int version2 = request.getVersion2();
            int writekey = request.getWritekey();
            int writeval = request.getWriteval();

            System.out.println(
                    ">>> EXECUTING:\n reqid " + reqId + " key1 " + key1 + " v1 " + version1 + " k2 " + key2 + " v2 "
                            + version2 + " wk " + writekey + " writeval " + writeval);

            TransactionRecord txrecord = new TransactionRecord(key1, version1, key2, version2, writekey, writeval,
                    this.requestsProcessed);

            boolean commit_success = this.server_state.store.commit(txrecord);

            requestArchiveStore.removeRequest(reqId);

            this.requestsProcessed++;

            System.out.println(
                    "Commit was " + (commit_success ? "SUCCESSFUL" : "ABORTED") + " for request with reqid "
                            + reqId);

            DadkvsMain.CommitReply response = DadkvsMain.CommitReply.newBuilder()
                    .setReqid(reqId).setAck(commit_success).build();

            StreamObserver<DadkvsMain.CommitReply> responseObserver = reqArchive.getResponseObserver();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
