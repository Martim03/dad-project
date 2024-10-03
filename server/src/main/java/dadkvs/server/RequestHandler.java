package dadkvs.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dadkvs.DadkvsMain;
import dadkvs.util.RequestArchive;
import io.grpc.stub.StreamObserver;

public class RequestHandler {

    int requestsProcessed;
    Map<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> request_map;
    Map<Integer, Integer> request_order_map;
    DadkvsServerState server_state;
    private int order = 0;
    private final Lock requestsProcessedLock;
    private final Lock handleCommitLock;
    private final Lock orderLock;

    public RequestHandler(DadkvsServerState state) {
        this.requestsProcessed = 0;
        this.request_map = new HashMap<>();
        this.request_map = Collections.synchronizedMap(this.request_map);
        this.request_order_map = new HashMap<>();
        this.request_order_map = Collections.synchronizedMap(this.request_order_map);
        this.server_state = state;
        this.requestsProcessedLock = new ReentrantLock();
        // lock?
        this.handleCommitLock = new ReentrantLock();
        // lock?
        this.orderLock = new ReentrantLock();
    }

    public void SwapRequestOrder(int order, int reqid) {
        Integer reqidOrder = null;

        for (Map.Entry<Integer, Integer> entry : request_order_map.entrySet()) {
            if (entry.getValue() == reqid) {
                reqidOrder = entry.getKey();
                break;
            }
        }

        if (reqidOrder == null) {
            System.out.println("Request ID not found in the map.");
            return;
        }

        if (order == reqidOrder) {
            // skip swap if its already on the right place
            return;
        }

        int temp = request_order_map.get(order);
        request_order_map.put(order, reqid);
        request_order_map.put(reqidOrder, temp);
    }

    public void addRequest(DadkvsMain.CommitRequest request, StreamObserver<DadkvsMain.CommitReply> responseObserver) {
        int nextOrder = -1;

        orderLock.lock();
        try {
            nextOrder = this.order++;
        } finally {
            orderLock.unlock();
        }

        request_order_map.put(nextOrder, request.getReqid());
        request_map.put(request.getReqid(), new RequestArchive(request, responseObserver, request.getReqid()));
        handleCommits();
    }

    public RequestArchive getRequestByOrder(int order) {
        return request_map.get(order);
    }

    public void handleCommits() {

        boolean commit_success = false;

        handleCommitLock.lock(); // TODO check if the lock will work right after the first unlock
        try {

            Integer requestid = request_order_map.get(this.requestsProcessed);
            if (!(requestid != null && request_map.containsKey(requestid)) && request_map.get(requestid).isCommited()) {
                // skip if request is not ready to execute
                return;
            }

            RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> request_archive = request_map
                    .get(requestid);
            DadkvsMain.CommitRequest request = request_archive.getRequest();

            int reqid = request.getReqid();
            int key1 = request.getKey1();
            int version1 = request.getVersion1();
            int key2 = request.getKey2();
            int version2 = request.getVersion2();
            int writekey = request.getWritekey();
            int writeval = request.getWriteval();

            System.out.println(
                    "executing:\n reqid " + reqid + " key1 " + key1 + " v1 " + version1 + " k2 " + key2 + " v2 "
                            + version2 + " wk " + writekey + " writeval " + writeval);

            TransactionRecord txrecord = new TransactionRecord(key1, version1, key2, version2, writekey, writeval,
                    this.requestsProcessed);

            commit_success = this.server_state.store.commit(txrecord);

            // clean the request from the maps
            request_map.remove(reqid);
            request_order_map.remove(this.requestsProcessed);

            requestsProcessedLock.lock(); // TODO maybe this can be removed if the whole function is
                                                      // synchronized
            try {
                this.requestsProcessed++;

            } finally {
                requestsProcessedLock.unlock();
            }

            // for debug purposes
            System.out.println(
                    "Commit was " + (commit_success ? "SUCCESSFUL" : "ABORTED") + " for request with reqid " + reqid);

            DadkvsMain.CommitReply response = DadkvsMain.CommitReply.newBuilder()
                    .setReqid(reqid).setAck(commit_success).build();

            StreamObserver<DadkvsMain.CommitReply> responseObserver = request_archive.getResponseObserver();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } finally {
            handleCommitLock.unlock();
        }

        if (commit_success) {
            handleCommits();
        }
    }

    public int getRequestsProcessed() {
        return requestsProcessed;
    }

    public int getCurrentOder() {
        return order;
    }
}
