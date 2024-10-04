package dadkvs.server;

import dadkvs.DadkvsPaxos;
import dadkvs.DadkvsPaxosServiceGrpc;
import io.grpc.stub.StreamObserver;

public class DadkvsPaxosServiceImpl extends DadkvsPaxosServiceGrpc.DadkvsPaxosServiceImplBase {

        DadkvsServerState server_state;
        RequestHandler requestHandler;

        public DadkvsPaxosServiceImpl(DadkvsServerState state, RequestHandler requestHandler) {
                this.server_state = state;
                this.requestHandler = requestHandler;
        }

        private boolean assertNotLearner(String method) {
                if (server_state.isOnlyLearner()) {
                        System.out.println("PANIC!!!!!!! LEARNER RECEIVING " + method);
                        return false;
                }
                return true;
        }

        private boolean assertNotCommited(int order) {
                if (requestHandler.getRequestByOrder(order).isCommited()) {
                        // Already commited, skiping paxos
                        System.err.println("Request already commited, skipping paxos");
                        return false;
                }
                return true;
        }

        @Override
        public void phaseone(DadkvsPaxos.PhaseOneRequest request,
                        StreamObserver<DadkvsPaxos.PhaseOneReply> responseObserver) {

                if (!assertNotLearner("phaseone") || !assertNotCommited(request.getPhase1Index())) {
                        return;
                }

                // for debug purposes
                System.out.println("Receive phase1 request: " + request);

                /*
                 * this is the request received from the leader to preppare the transaction
                 * so the server will respond with the value of the highest accepted proposal
                 * for the leader to understand
                 * if it can propose a new value or was there already an ongoing transaction
                 * that must be now completed
                 * OR
                 * just reject the request if the server has already accepted a higher proposal
                 * 
                 */
                DadkvsPaxos.PhaseOneReply.Builder phase1_reply = DadkvsPaxos.PhaseOneReply.newBuilder();

                if (request.getPhase1Timestamp() < requestHandler.getRequestByOrder(request.getPhase1Index())
                                .getReadTS()) {
                        // reject the request

                        phase1_reply.setPhase1Accepted(false);

                        responseObserver.onNext(phase1_reply.build());
                        responseObserver.onCompleted();
                        return;
                }

                // Update the readTS of the request
                requestHandler.getRequestByOrder(request.getPhase1Index()).setReadTS(request.getPhase1Timestamp());

                int writeTS = requestHandler.getRequestByOrder(request.getPhase1Index()).getWriteTS();

                phase1_reply.setPhase1Config(server_state.getConfig())
                                .setPhase1Index(requestHandler.getRequestsProcessed())
                                .setPhase1Timestamp(writeTS).setPhase1Accepted(true)
                                .setPhase1Value(requestHandler.getRequestByOrder(request.getPhase1Index()).getReqId());

                responseObserver.onNext(phase1_reply.build());
                responseObserver.onCompleted();
        }

        @Override
        public void phasetwo(DadkvsPaxos.PhaseTwoRequest request,
                        StreamObserver<DadkvsPaxos.PhaseTwoReply> responseObserver) {
                // for debug purposes

                // TODO fix timeouts, and use debug console, use debug varaible to decide
                // TODO SEND LEARN

                if (!assertNotLearner("phasetwo") || !assertNotCommited(request.getPhase2Index())) {
                        return;
                }

                System.out.println("Receive phase two request: idx=" + request.getPhase2Index() + " val="
                                + request.getPhase2Value() + " ts=" + request.getPhase2Timestamp());

                /*
                 * this is the request received from the leader to anounce the chosen value
                 * so the server will respond with an OK if it hanst already accepted a higher
                 * proposal and update the request order with the new index (but not commited)
                 * OR
                 * just reject the request if the server has already accepted a higher proposal
                 * 
                 */

                DadkvsPaxos.PhaseTwoReply.Builder phase2_reply = DadkvsPaxos.PhaseTwoReply.newBuilder();

                if (request.getPhase2Timestamp() < requestHandler.getRequestByOrder(request.getPhase2Index())
                                .getReadTS()) {
                        // reject the request
                        System.out.println("Rejecting phase two request: idx=" + request.getPhase2Index() + " ts="
                                        + request.getPhase2Timestamp());

                        phase2_reply.setPhase2Accepted(false);

                        responseObserver.onNext(phase2_reply.build());
                        responseObserver.onCompleted();
                        return;
                }

                // TODO update read and write TS

                // Print debug message
                System.out.println(
                                "Accepting phase two request: idx=" + request.getPhase2Index() + " ts="
                                                + request.getPhase2Timestamp());
                // Fix the requests order with the new index
                requestHandler.SwapRequestOrder(request.getPhase2Index(), request.getPhase2Value());

                // Update the writeTS of the request
                // TODO Update the readTS of the request (is this necessary?)
                requestHandler.getRequestByOrder(request.getPhase2Index()).setWriteTS(request.getPhase2Timestamp())
                                .setReadTS(request.getPhase2Timestamp());

                phase2_reply.setPhase2Config(server_state.getConfig()).setPhase2Index(request.getPhase2Index())
                                .setPhase2Accepted(true);

                // Print debug message
                System.out.println(
                                "Phase two request accepted: idx=" + request.getPhase2Index() + " ts="
                                                + request.getPhase2Timestamp());

                responseObserver.onNext(phase2_reply.build());
                responseObserver.onCompleted();
        }

        @Override
        public void learn(DadkvsPaxos.LearnRequest request, StreamObserver<DadkvsPaxos.LearnReply> responseObserver) {
                // for debug purposes
                System.out.println("Receive learn request: " + request);

                if (!assertNotLearner("learn") || !assertNotCommited(request.getLearnindex())) {
                        return;
                }

                /*
                 * this is the request received from the leader to anounce the value to commit
                 * so the server will take the chosen value and commit it to the store, it is
                 * decided
                 * 
                 */

                // TODO no need to check for the leader ID because "learns" are never rejected?

                // Commit the transaction locally
                requestHandler.SwapRequestOrder(request.getLearnindex(), request.getLearnvalue());
                requestHandler.getRequestByOrder(request.getLearnindex()).setCommited(true);
                requestHandler.handleCommits();

                // Respond with OK
                // TODO is it neceessary to set the request attributes?
                DadkvsPaxos.LearnReply.Builder learn_reply = DadkvsPaxos.LearnReply.newBuilder();

                responseObserver.onNext(learn_reply.build());
                responseObserver.onCompleted();
        }

}
