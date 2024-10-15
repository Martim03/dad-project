package dadkvs.util;

import io.grpc.stub.StreamObserver;

public class RequestArchive<ReqT, RespT> {

    private ReqT request;
    private StreamObserver<RespT> responseObserver;
    private int reqId;
    private boolean proposable;

    public RequestArchive(ReqT request, StreamObserver<RespT> responseObserver, int reqId) {
        this.request = request;
        this.responseObserver = responseObserver;
        this.reqId = reqId;
        this.proposable = true;
    }

    public synchronized ReqT getRequest() {
        return request;
    }

    public synchronized StreamObserver<RespT> getResponseObserver() {
        return responseObserver;
    }

    public synchronized int getReqId() {
        return reqId;
    }

    public synchronized boolean isProposable() {
        return proposable;
    }

    public synchronized RequestArchive<ReqT, RespT> setReqId(int reqId) {
        this.reqId = reqId;
        return this;
    }

    public synchronized RequestArchive<ReqT, RespT> setRequest(ReqT request) {
        this.request = request;
        return this;
    }

    public synchronized RequestArchive<ReqT, RespT> setResponseObserver(StreamObserver<RespT> responseObserver) {
        this.responseObserver = responseObserver;
        return this;
    }

    public synchronized void markUnproposable() {
        this.proposable = false;
    }
}
