package dadkvs.server;

import dadkvs.DadkvsPaxos;
import dadkvs.DadkvsPaxosServiceGrpc;
import io.grpc.stub.StreamObserver;

public class DadkvsPaxosServiceImpl extends DadkvsPaxosServiceGrpc.DadkvsPaxosServiceImplBase {

    DadkvsServerState server_state;

    public DadkvsPaxosServiceImpl(DadkvsServerState state) {
        this.server_state = state;

    }

    @Override
    public void phaseone(DadkvsPaxos.PhaseOneRequest request,
            StreamObserver<DadkvsPaxos.PhaseOneReply> responseObserver) {
        // for debug purposes
        System.out.println("Receive phase1 request: " + request);
        /*
         * TODO
         * 
         * this is the request received from the leader to preppare the transaction
         * so the server will respond with the value of the highest accepted proposal
         * for the leader to understand
         * if it can propose a new value or was there already an ongoing transaction
         * that must be now completed
         * OR
         * just reject the request if the server has already accepted a higher proposal
         * 
         */

    }

    @Override
    public void phasetwo(DadkvsPaxos.PhaseTwoRequest request,
            StreamObserver<DadkvsPaxos.PhaseTwoReply> responseObserver) {
        // for debug purposes
        System.out.println("Receive phase two request: " + request);

        /*
         * TODO
         * 
         * this is the request received from the leader to anounce the chosen value
         * so the server will respond with an OK if it hanst already accepted a higher
         * proposal
         * OR
         * just reject the request if the server has already accepted a higher proposal
         * 
         */

    }

    @Override
    public void learn(DadkvsPaxos.LearnRequest request, StreamObserver<DadkvsPaxos.LearnReply> responseObserver) {
        // for debug purposes
        System.out.println("Receive learn request: " + request);

        /*
         * TODO
         * 
         * this is the request received from the leader to anounce the value to commit
         * so the server will take the chosen value and commit it to the store, it is
         * decided
         * 
         */

    }

}
