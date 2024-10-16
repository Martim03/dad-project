package dadkvs.server;

/* these imported classes are generated by the contract */
import dadkvs.DadkvsConsole;
import dadkvs.DadkvsConsoleServiceGrpc;
import io.grpc.stub.StreamObserver;

public class DadkvsConsoleServiceImpl extends DadkvsConsoleServiceGrpc.DadkvsConsoleServiceImplBase {
    DadkvsServerState state;
    PaxosProposer proposer;
    DebugModes debug;

    public DadkvsConsoleServiceImpl(DadkvsServerState state, PaxosProposer proposer, DebugModes debug) {
        this.state = state;
        this.proposer = proposer;
        this.debug = debug;
    }

    @Override
    public void setleader(DadkvsConsole.SetLeaderRequest request,
            StreamObserver<DadkvsConsole.SetLeaderReply> responseObserver) {

        // for debug purposes
        System.out.println(request);

        // reply to the request with ACK
        boolean response_value = true;
        DadkvsConsole.SetLeaderReply response = DadkvsConsole.SetLeaderReply.newBuilder()
                .setIsleaderack(response_value).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        boolean isLeader = request.getIsleader();

        // change the leader status
        state.setLeader(isLeader);

        // for debug purposes
        System.out.println("I am the leader = " + state.isLeader());

        this.state.wakeMainLoop(); // TODO are main loops necessary?

        if (isLeader) {
            // if the leader status was changed to ON then wake up the proposer
            proposer.wakeUp();
        }
    }

    @Override
    public void setdebug(DadkvsConsole.SetDebugRequest request,
            StreamObserver<DadkvsConsole.SetDebugReply> responseObserver) {
        // for debug purposes
        System.out.println(request);

        boolean response_value = true;

        state.setDebugMode(request.getMode());
        state.wakeMainLoop();

        // for debug purposes
        System.out.println("Setting debug mode to = " + state.getDebugMode());

        // reply to the request with ACK
        DadkvsConsole.SetDebugReply response = DadkvsConsole.SetDebugReply.newBuilder()
                .setAck(response_value).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // handle debug mode changes

        if (state.getDebugMode() == DebugModeCodes.CRASH.getCode()) {
            // Terminate everything with shutdownHook
            System.exit(0);
        } else if (state.getDebugMode() == DebugModeCodes.UNFREEZE.getCode()) {
            debug.unfreeze();
        }

    }
}
