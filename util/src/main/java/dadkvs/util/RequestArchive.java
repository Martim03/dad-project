package dadkvs.util;

import io.grpc.stub.StreamObserver;

public class RequestArchive<ReqT, RespT> {

    private ReqT request;
    private StreamObserver<RespT> responseObserver;
    private int reqId;
    private int writeTS;
    private int readTS;
    private boolean commited;

    public RequestArchive() {
        this.request = null;
        this.responseObserver = null;
        this.reqId = -1;
        this.writeTS = 0;
        this.readTS = 0;
        this.commited = false;
    }

    public ReqT getRequest() {
        return request;
    }

    public StreamObserver<RespT> getResponseObserver() {
        return responseObserver;
    }

    public int getWriteTS() {
        return writeTS;
    }

    public int getReadTS() {
        return readTS;
    }

    public boolean isCommited() {
        return commited;
    }

    public RequestArchive<ReqT, RespT> setReqId(int reqId) {
        this.reqId = reqId;
        return this;
    }

    public RequestArchive<ReqT, RespT> setRequest(ReqT request) {
        this.request = request;
        return this;
    }

    public RequestArchive<ReqT, RespT> setResponseObserver(StreamObserver<RespT> responseObserver) {
        this.responseObserver = responseObserver;
        return this;
    }

    public RequestArchive<ReqT, RespT> setWriteTS(int writeTS) {
        this.writeTS = writeTS;
        return this;
    }

    public RequestArchive<ReqT, RespT> setReadTS(int readTS) {
        this.readTS = readTS;
        return this;
    }

    public RequestArchive<ReqT, RespT> setCommited(boolean commited) {
        this.commited = commited;
        return this;
    }

    public int getReqId() {
        return reqId;
    }
}
