package dadkvs.util;

import io.grpc.stub.StreamObserver;

public class RequestArchive<ReqT, RespT> {

    private final ReqT request;
    private final StreamObserver<RespT> responseObserver;

    public RequestArchive(ReqT request, StreamObserver<RespT> responseObserver) {
        this.request = request;
        this.responseObserver = responseObserver;
    }

    public ReqT getRequest() {
        return request;
    }

    public StreamObserver<RespT> getResponseObserver() {
        return responseObserver;
    }
}
