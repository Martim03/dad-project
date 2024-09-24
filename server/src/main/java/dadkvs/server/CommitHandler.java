package dadkvs.server;

import java.util.Map;

import dadkvs.DadkvsMain;
import dadkvs.util.RequestArchive;
import io.grpc.stub.StreamObserver;

public class CommitHandler {

    int requestsProcessed;
    Map<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> request_map;
    Map<Integer, Integer> request_order_map;
    DadkvsServerState server_state;

    public CommitHandler(Map<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> request_map, Map<Integer, Integer> request_order_map, DadkvsServerState state) {
        this.requestsProcessed = 0;
        this.request_map = request_map;
        this.request_order_map = request_order_map;
        this.server_state = state;
    }

    public void addOrderedRequest(int order, int reqid) {
        request_order_map.put(order, reqid);
        handleCommits();
    }

    public void addRequest(DadkvsMain.CommitRequest request, StreamObserver<DadkvsMain.CommitReply> responseObserver) {

        request_map.put(request.getReqid(), new RequestArchive(request, responseObserver));
        handleCommits();

    }

    public void handleCommits() {

        Integer requestid = request_order_map.get(this.requestsProcessed);
        if (!(requestid != null && request_map.containsKey(requestid))) {
            // skip if request is not ready to execute
            return;
        }

        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> request_archive = request_map.get(requestid);
        DadkvsMain.CommitRequest request = request_archive.getRequest();

        int reqid = request.getReqid();
        int key1 = request.getKey1();
        int version1 = request.getVersion1();
        int key2 = request.getKey2();
        int version2 = request.getVersion2();
        int writekey = request.getWritekey();
        int writeval = request.getWriteval();

        System.out.println("executing:\n reqid " + reqid + " key1 " + key1 + " v1 " + version1 + " k2 " + key2 + " v2 " + version2 + " wk " + writekey + " writeval " + writeval);

        TransactionRecord txrecord = new TransactionRecord(key1, version1, key2, version2, writekey, writeval, this.requestsProcessed);

        // TODO ainda não sei se o server state devia estar aqui não
        boolean commit_success = this.server_state.store.commit(txrecord);

        if (commit_success == true) {
            this.requestsProcessed++;
            // TODO removes from the maps
        }

        // for debug purposes
        System.out.println("Commit was " + (commit_success ? "SUCCESSFUL" : "ABORTED") + " for request with reqid " + reqid);

        DadkvsMain.CommitReply response = DadkvsMain.CommitReply.newBuilder()
                .setReqid(reqid).setAck(commit_success).build();

        StreamObserver<DadkvsMain.CommitReply> responseObserver = request_archive.getResponseObserver();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
