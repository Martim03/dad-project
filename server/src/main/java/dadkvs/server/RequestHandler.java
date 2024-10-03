package dadkvs.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dadkvs.DadkvsMain;
import dadkvs.util.RequestArchive;
import io.grpc.stub.StreamObserver;

public class RequestHandler {
    // TODO check for request concureency!!
    // TODO add lookup map by reqId for optimization

    int requestsProcessed;
    Map<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> request_order_map;
    DadkvsServerState server_state;
    private int order = 0;
    private final Lock requestsProcessedLock;
    private final Lock handleCommitLock;
    private final Lock orderLock;
    private final int undefinedReqId = -1;

    public RequestHandler(DadkvsServerState state) {
        this.requestsProcessed = 0;
        this.request_order_map = new HashMap<>();
        this.request_order_map = Collections.synchronizedMap(this.request_order_map);
        this.server_state = state;
        this.requestsProcessedLock = new ReentrantLock();
        this.handleCommitLock = new ReentrantLock();
        this.orderLock = new ReentrantLock();
    }

    public void SwapRequestOrder(int order, int reqid) {
        Integer reqidOrder = null;

        for (Map.Entry<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> entry : request_order_map
                .entrySet()) {
            if (entry.getValue().getReqId() == reqid) {
                reqidOrder = entry.getKey();
                break;
            }
        }

        if (reqidOrder == null) {
            // if not found it is assumed as the last request to arrive (order)
            reqidOrder = order;
            addEmptyRequest().setReqId(reqid); // TODO can it be empty and have  a reqID?????
        }

        if (order == reqidOrder) {
            // skip swap if its already on the right place
            return;
        }

        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> temp = request_order_map.get(order);
        request_order_map.put(order, getRequestById(reqid));
        request_order_map.put(reqidOrder, temp);
    }

    //TODO FOR SURE DA PARA DAR REFACTOR NISTO
    public int GetIncrementOrder() {
        int nextOrder = -1;

        orderLock.lock();
        try {
            nextOrder = this.order++;
        } finally {
            orderLock.unlock();
        }

        return nextOrder;
    }

    public int getOrder() {
        int nextOrder = -1;

        orderLock.lock();
        try {
            nextOrder = this.order;
        } finally {
            orderLock.unlock();
        }

        return nextOrder;
    }

    public RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> addRequest() {
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive = new RequestArchive<>();

        request_order_map.put(GetIncrementOrder(), reqArchive);
        return reqArchive;
    }

    public RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> addEmptyRequest() {
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive = new RequestArchive<>();

        request_order_map.put(getOrder(), reqArchive);
        return reqArchive;
    }

    public void registerClientRequest(DadkvsMain.CommitRequest request,
            StreamObserver<DadkvsMain.CommitReply> responseObserver) {
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive = getRequestById(
                request.getReqid());

        if (reqArchive == null) {
            // Didnt find request with reqId

            reqArchive = request_order_map.get(requestsProcessed);
            if (reqArchive == null) {
                // request in the current order does not exist

                addRequest().setReqId(request.getReqid()).setRequest(request).setResponseObserver(responseObserver);
            } else {
                // exists but its empty, so we'll fill the values

                reqArchive.setReqId(request.getReqid()).setRequest(request).setResponseObserver(responseObserver);
                GetIncrementOrder(); // It was empty before, now it has a reqId so it counts as a new
            }
        } else {
            // Found request with redId, so we'll fill the values
            reqArchive.setRequest(request).setResponseObserver(responseObserver);
        }

        handleCommits();
    }

    public void handleCommits() {

        boolean commit_success = false;

        handleCommitLock.lock(); // TODO check if the lock will work right after the first unlock
        try {

            RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive = request_order_map
                    .get(this.requestsProcessed);
            if (!((reqArchive != null) && (reqArchive.getReqId() != undefinedReqId) && reqArchive.isCommited()
                    && (reqArchive.getRequest() != null))) {
                // skip if request is not ready to execute
                return;
            }

            DadkvsMain.CommitRequest request = reqArchive.getRequest();

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

            StreamObserver<DadkvsMain.CommitReply> responseObserver = reqArchive.getResponseObserver();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } finally {
            handleCommitLock.unlock();
        }

        if (commit_success) {
            handleCommits();
        }
    }

    public RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> getRequestByOrder(int order) {
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive = request_order_map.get(order);

        if (reqArchive == null) {
            addEmptyRequest();
        }

        return reqArchive;
    }

    public RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> getRequestById(int reqId) {
        for (Map.Entry<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> entry : request_order_map
                .entrySet()) {
            if (entry.getValue().getReqId() == reqId) {
                return entry.getValue();
            }
        }
        return null;
    }

    public int getRequestsProcessed() {
        return requestsProcessed;
    }
}
