package dadkvs.util;

import io.grpc.stub.StreamObserver;

public class RequestArchive<ReqT, RespT> {

    private ReqT request;
    private StreamObserver<RespT> responseObserver;
    private final int reqId;
    private int writeTS;
    private int readTS;
    private boolean commited;

    public RequestArchive(int reqId) {
        this.request = null;
        this.responseObserver = null;
        this.reqId = reqId;
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

    public void setRequest(ReqT request) {
        this.request = request;
    }

    public void setResponseObserver(StreamObserver<RespT> responseObserver) {
        this.responseObserver = responseObserver;
    }

    public void setWriteTS(int writeTS) {
        this.writeTS = writeTS;
    }

    public void setReadTS(int readTS) {
        this.readTS = readTS;
    }

    public void setCommited(boolean commited) {
        this.commited = commited;
    }

    public int getReqId() {
        return reqId;
    }
}
