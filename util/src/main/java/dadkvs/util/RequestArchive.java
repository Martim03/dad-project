package dadkvs.util;

import io.grpc.stub.StreamObserver;

public class RequestArchive<ReqT, RespT> {

    private final ReqT request;
    private final StreamObserver<RespT> responseObserver;
    private final int reqId;
    private int writeTS;
    private int readTS;
    private boolean commited;

    public RequestArchive(ReqT request, StreamObserver<RespT> responseObserver, int reqId) {
        this.request = request;
        this.responseObserver = responseObserver;
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
