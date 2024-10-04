package dadkvs.server;

/* these imported classes are generated by the contract */
// import java.awt.color.ICC_Profile;
// import java.lang.classfile.instruction.ThrowInstruction;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dadkvs.DadkvsMain;
import dadkvs.DadkvsMainServiceGrpc;
import dadkvs.DadkvsPaxos;
import dadkvs.DadkvsPaxos.PhaseOneReply;
import dadkvs.DadkvsPaxos.PhaseTwoReply;
import dadkvs.DadkvsPaxosServiceGrpc;
import dadkvs.util.CollectorStreamObserver;
import dadkvs.util.GenericResponseCollector;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

// TODO always put if statements to check if the request was alaready comitted and no work is necessary! 

public class DadkvsMainServiceImpl extends DadkvsMainServiceGrpc.DadkvsMainServiceImplBase {

    DadkvsServerState server_state;
    RequestHandler requestHandler;
    int num_servers;
    int my_id;
    ManagedChannel[] channels;
    DadkvsPaxosServiceGrpc.DadkvsPaxosServiceStub[] async_stubs;
    private final Lock expOrderAttemptLock = new ReentrantLock();
    int attempt = 0;
    int orderAttempted;

    public DadkvsMainServiceImpl(DadkvsServerState state, RequestHandler handler, int my_id) {
        this.server_state = state;
        this.my_id = my_id;
        this.requestHandler = handler;
        this.orderAttempted = requestHandler.getRequestsProcessed();
        this.num_servers = 5;
        this.channels = new ManagedChannel[this.num_servers];
        this.async_stubs = new DadkvsPaxosServiceGrpc.DadkvsPaxosServiceStub[this.num_servers];
        startComms();
    }

    @Override
    public void read(DadkvsMain.ReadRequest request, StreamObserver<DadkvsMain.ReadReply> responseObserver) {
        // for debug purposesd
        System.out.println("Receiving read request:" + request);

        int reqid = request.getReqid();
        int key = request.getKey();
        VersionedValue vv = this.server_state.store.read(key);

        DadkvsMain.ReadReply response = DadkvsMain.ReadReply.newBuilder()
                .setReqid(reqid).setValue(vv.getValue()).setTimestamp(vv.getVersion()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void committx(DadkvsMain.CommitRequest request, StreamObserver<DadkvsMain.CommitReply> responseObserver) {

        // for debug purposes
        System.out.println("Receiving commit request at "
                + java.time.LocalDateTime.now() + ": " + request);

        int reqid = request.getReqid();
        int key1 = request.getKey1();
        int version1 = request.getVersion1();
        int key2 = request.getKey2();
        int version2 = request.getVersion2();
        int writekey = request.getWritekey();
        int writeval = request.getWriteval();

        // // for debug purposes
        // System.out.println(">>> RECEIVING:\n reqid " + reqid + " key1 " + key1 + " v1
        // " + version1 + " k2 " + key2 + " v2 "
        // + version2 + " wk " + writekey + " writeval " + writeval);
        requestHandler.registerClientRequest(request, responseObserver); // TODO could be locked to ensure no problems
                                                                         // with multiple requests at same time, also
                                                                         // ensure no duplicates(even though would never
                                                                         // happen)

        if (requestHandler.getRequestByOrder(requestHandler.getRequestsProcessed()).isCommited()) {
            // Already commited, skiping paxos
            return;
        }

        /*
         * TODO ensure no 2 requests are handlel at same time
         * requests must be put on hold and handled again after ending a paxus round
         * 
         * if (currently on paxos ) skip ?
         */

        if (server_state.i_am_leader == true) {
            // server is a LEADER
            System.out.println("I think i am the leader, starting paxos consensus");

            /*
             * send phase1 request to all acceptor servers to prepare transaction
             * check for the validity of the majority to see if it is safe to proceed for
             * the commit
             * send phase2 request to all acceptor servers with the value to commit (order
             * number and req num)
             * check for the replies and if the majority of the servers have accepted the
             * value
             * send learn request to ALL servers to inform the commit has been completed and
             * its value, now they should commit the value to their local store
             * 
             */

            if (server_state.my_id == 0) {
                // if its the first leader skips phase 1

                sendPhase2();
            } else {
                sendPhase1();
            }

        } else if (!server_state.isOnlyLearner()) {
            // server is a ACCEPTOR
            requestTimeout();
        } else {
            // server is a LEARNER
            System.out.println("I am just a learner not interfere in paxos");
        }

        System.out.println("CHEGUEI AO FIM!!!!!!");
    }

    public void startComms() {
        String host = "localhost";
        int port = 8080;

        for (int i = 0; i < this.num_servers; i++) {
            String target = host + ":" + Integer.toString(port + i);
            channels[i] = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            async_stubs[i] = DadkvsPaxosServiceGrpc.newStub(channels[i]);
        }
    }

    private void requestTimeout() {
        // Does a Timeout + random mini Timeout

        try {
            int timeout = 1000; // base timeout in milliseconds
            int randomTimeout = (int) (Math.random() * 500); // random additional timeout
            Thread.sleep(timeout + randomTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Request timeout interrupted: " + e.getMessage());
        }

        boolean requestWasCommited = requestHandler.getRequestByOrder(requestHandler.getRequestsProcessed())
                .isCommited();
        boolean receivedPaxosMessages = requestHandler.getRequestByOrder(requestHandler.getRequestsProcessed())
                .getReadTS() > 0;

        if (requestWasCommited || receivedPaxosMessages) {
            // some leader started paxos, so dont assume leadership
            return;
        }

        server_state.i_am_leader = true;
        sendPhase1();
    }

    private void exponentialTimeout() {
        expOrderAttemptLock.lock();
        try {
            if (requestHandler.getRequestsProcessed() == orderAttempted) {
                attempt++;
            } else {
                attempt = 0;
                orderAttempted = requestHandler.getRequestsProcessed();
            }
        } finally {
            expOrderAttemptLock.unlock();
        }

        try {
            int timeout = 1000; // base timeout in milliseconds
            int randomTimeout = (int) (Math.random() * 500); // random additional timeout
            Thread.sleep(timeout + randomTimeout * (int) Math.pow(2, attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Request timeout interrupted: " + e.getMessage());
        }
    }

    public void onRefusal() {
        server_state.i_am_leader = false;
        my_id += num_servers;
        exponentialTimeout();

        if (requestHandler.getRequestByOrder(requestHandler.getRequestsProcessed()).isCommited()) { 
            // Already commited, skiping paxos
            return;
        }

        server_state.i_am_leader = true;
        sendPhase1();
    }

    public void WaitForMajority(GenericResponseCollector responseCollector) {
        responseCollector.waitForTarget((server_state.getConfigMembers().length / 2) + 1);
    }

    public void sendPhase1() {
        DadkvsPaxos.PhaseOneRequest.Builder phase1_request = DadkvsPaxos.PhaseOneRequest.newBuilder();
        phase1_request.setPhase1Config(server_state.getConfig())
                .setPhase1Index(this.requestHandler.getRequestsProcessed())
                .setPhase1Timestamp(this.my_id);
        ArrayList<DadkvsPaxos.PhaseOneReply> phase1_responses = new ArrayList<>();
        GenericResponseCollector<DadkvsPaxos.PhaseOneReply> commit_collector = new GenericResponseCollector<>(
                phase1_responses, this.num_servers);

        for (int i = 0; i < server_state.getConfigMembers().length; i++) {
            CollectorStreamObserver<DadkvsPaxos.PhaseOneReply> commit_observer = new CollectorStreamObserver<>(
                    commit_collector);
            this.async_stubs[server_state.getConfigMembers()[i]].phaseone(phase1_request.build(), commit_observer);
            System.out.println("Sending Phase1 request to server " + i);
        }

        boolean success = true;
        Integer latestValue = null;
        int highestWriteTS = 0;
        WaitForMajority(commit_collector);
        for (PhaseOneReply phaseOneReply : phase1_responses) {
            if (!phaseOneReply.getPhase1Accepted()) {
                // there is a new leader with higher ID, abort!
                success = false;
                break;
            }
            if (phaseOneReply.getPhase1Timestamp() > highestWriteTS) {
                highestWriteTS = phaseOneReply.getPhase1Timestamp();
                latestValue = phaseOneReply.getPhase1Value();
            }
        }

        if (!success) {
            onRefusal();
        } else {
            // if all are null, choses own value
            // else selects most recent value (highest writeTS)

            if (latestValue != null) {
                requestHandler.SwapRequestOrder(requestHandler.getRequestsProcessed(), latestValue);
            }

            sendPhase2();
        }
    }

    public void sendPhase2() {
        DadkvsPaxos.PhaseTwoRequest.Builder phase2_request = DadkvsPaxos.PhaseTwoRequest.newBuilder();
        phase2_request.setPhase2Config(server_state.getConfig()).setPhase2Index(requestHandler.getRequestsProcessed())
                .setPhase2Timestamp(this.my_id)
                .setPhase2Value(requestHandler.getRequestByOrder(requestHandler.getRequestsProcessed()).getReqId());

        ArrayList<DadkvsPaxos.PhaseTwoReply> phase2_responses = new ArrayList<>();
        GenericResponseCollector<DadkvsPaxos.PhaseTwoReply> phase2_collector = new GenericResponseCollector<>(
                phase2_responses, this.num_servers);

        for (int i = 0; i < server_state.getConfigMembers().length; i++) {
            CollectorStreamObserver<DadkvsPaxos.PhaseTwoReply> phase2_observer = new CollectorStreamObserver<>(
                    phase2_collector);
            this.async_stubs[server_state.getConfigMembers()[i]].phasetwo(phase2_request.build(), phase2_observer);
            System.out.println("Sending Phase2 request to server " + i);
        }

        boolean success = true;
        WaitForMajority(phase2_collector);
        System.out.println("Phase 2 responses majority was received");
        for (PhaseTwoReply phaseTwoReply : phase2_responses) {
            if (!phaseTwoReply.getPhase2Accepted()) {
                // there is a new leader with higher ID, abort!
                success = false;
                break;
            }
        }

        if (!success) {
            System.out.println("Phase 2 failed, initiating onRefusal process");
            onRefusal();
        } else {
            System.out.println("Phase 2 succeeded, sending learn request");
            sendLearn();
        }
    }

    public void sendLearn() {
        DadkvsPaxos.LearnRequest.Builder learn_request = DadkvsPaxos.LearnRequest.newBuilder();
        learn_request.setLearnconfig(server_state.getConfig()).setLearnindex(requestHandler.getRequestsProcessed())
                .setLearnvalue(requestHandler.getRequestByOrder(requestHandler.getRequestsProcessed()).getReqId())
                .setLearntimestamp(my_id);
        ArrayList<DadkvsPaxos.LearnReply> learn_responses = new ArrayList<>();
        GenericResponseCollector<DadkvsPaxos.LearnReply> commit_collector = new GenericResponseCollector<>(
                learn_responses, this.num_servers);

        for (int i = 0; i < this.num_servers; i++) {
            CollectorStreamObserver<DadkvsPaxos.LearnReply> commit_observer = new CollectorStreamObserver<>(
                    commit_collector);
            this.async_stubs[i].learn(learn_request.build(), commit_observer);
            System.out.println("Sending Learn request to server " + i);
        }

        // TODO Do we need to wait?? do errors matter?
        commit_collector.waitForTarget(this.num_servers);
        // TODO UNLOCK SECRET FORMULA AND NOTIFY ALL
    }
}
