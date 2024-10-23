package dadkvs.server;

import java.util.ArrayList;

import dadkvs.DadkvsMain;
import dadkvs.DadkvsPaxos;
import dadkvs.DadkvsPaxos.PhaseOneReply;
import dadkvs.DadkvsPaxos.PhaseTwoReply;
import dadkvs.DadkvsPaxosServiceGrpc.DadkvsPaxosServiceStub;
import dadkvs.util.CollectorStreamObserver;
import dadkvs.util.GenericResponseCollector;
import dadkvs.util.PaxosLog;
import dadkvs.util.PaxosProposal;
import dadkvs.util.RequestArchive;
import dadkvs.util.RequestArchiveStore;

public class PaxosProposer extends PaxosParticipant {
    int paxosRoundsProposed;

    public PaxosProposer(DadkvsServerState state, RequestArchiveStore requestArchiveStore, PaxosLog paxosLog) {
        super(state, requestArchiveStore, paxosLog);
        paxosRoundsProposed = 0;
    }

    private synchronized void incrementPaxosRound() {
        paxosRoundsProposed++;
        System.out.println(".................................................. PAXOS: " + (paxosRoundsProposed - 1)
                + " --> " + paxosRoundsProposed);
    }

    private synchronized void checkReconfiguration(
            RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArch) {

        if (reqArch.getRequest().getWritekey() == super.getServerState().getKvsConfigIdx()) {
            // is writing a value in the config index, therefore changing the configuration
            // (reconfig)
            super.getServerState().setConfig(reqArch.getRequest().getWriteval());
        }
    }

    private Integer chooseOwnProposeValue() {
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArch = super.getRequestArchiveStore()
                .getNext();
        if (reqArch == null) {
            System.out.println(
                    "PROPOSER: \n\n\n\n\n!!!!!!!!!!PANIC 001!!!!!!!!!!\n\n\n\n\n");
            return null;
        }
        return reqArch.getReqId();
    }

    /**
     * This will be the main flow of the leader and will be called with:
     * setLeader(On)
     * OR
     * when a new request enters the machine (RequestArchiveStore.add(req))
     * Once the Proposer wakes up it will check if it is the leader and start
     * proposing requests that were waiting (if any).
     * 
     * FLOW:
     * send phase1 request to all acceptor servers to prepare transaction
     * check for the validity of the majority to see if it is safe to proceed for
     * the commit
     * send phase2 request to all acceptor servers with the value to commit (order
     * number and req num)
     * check for the replies and if the majority of the servers have accepted the
     * value
     * send learn request to ALL servers to inform the commit has been completed and
     * its value, now they should commit the value to their local store
     */
    public synchronized void wakeUp() {
        RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArch;
        boolean iAmLeader;
        boolean thereAreRequestsToPropose;

        while (true) {

            // Check if can skip some rounds that were already proposed by other paxos
            // leaders
            PaxosProposal prop = super.getPaxosLog().getPropose(paxosRoundsProposed);
            if (prop != null && prop.isCommited()) {
                // round is already commited might be skippable

                reqArch = super.getRequestArchiveStore().getRequest(prop.getReqId());
                if (reqArch == null) {
                    // TODO could be otimized in a way so that it doesnt need to have the request to
                    // know the reconfigs and instead it uses the 'config' field of the messages to
                    // anounce config changes


                    // cant advance to the next paxos round if it doesnt know the
                    // request yet (could be a reconfig), just go to "sleep" until the request
                    // arrives
                    break;
                }

                // Skiping round because it was already commited and request was known
                advancePaxosRound(reqArch);
                continue;
            }

            // check if is still a valid leader and can start the paxos round
            iAmLeader = super.getServerState().isValidLeader();
            reqArch = super.getRequestArchiveStore().getNext();
            thereAreRequestsToPropose = (reqArch != null);
            if (!(iAmLeader && thereAreRequestsToPropose)) {
                // If im not a valid leader or there are no requests to propose, go to "sleep"
                break;
            }

            // Start a Paxos Round

            Integer reqIdToPropose = executePhaseOne();
            if (reqIdToPropose == null) {
                // phase1 rejected try again
                incrementLeaderId();
                continue;
            }

            if (!executesPhaseTwo(reqIdToPropose)) {
                // phase2 rejected try again
                incrementLeaderId();
                continue;
            }

            advancePaxosRound(reqArch);
        }
    }

    private void advancePaxosRound(RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArch) {
        // Reached the end of the paxos round successfully, go to next one
        reqArch.markUnproposable(); // mark the proposed request as unporoposable to next rounds)
        checkReconfiguration(reqArch);
        incrementPaxosRound();
    }

    private void incrementLeaderId() {
        super.getServerState().incrementBallotNumber();
    }

    /**
     * Sends phase1 to the Acceptors and waits for a majority of answers.
     * Returns the reqId that was promised to propose on phase2,
     * could be an already accepted value or one that was chosen by this proposer
     * (if no values were accepted yet).
     * Returns null if received at least 1 NACK
     */
    private Integer executePhaseOne() {
        DadkvsServerState state = super.getServerState();
        ArrayList<DadkvsPaxos.PhaseOneReply> phase1Responses = new ArrayList<>();
        GenericResponseCollector<DadkvsPaxos.PhaseOneReply> commit_collector = new GenericResponseCollector<>(
                phase1Responses, state.getNumAcceptors());

        if (state.getBallotNumber() == state.getInitialLeaderid()) {
            // skip phase1 if its the first leader
            return chooseOwnProposeValue();
        }

        sendPhaseOne(commit_collector);
        Integer reqIdToPropose = waitPhaseOneReplies(commit_collector, phase1Responses);
        return reqIdToPropose;
    }

    private void sendPhaseOne(GenericResponseCollector<DadkvsPaxos.PhaseOneReply> commit_collector) {
        DadkvsServerState state = super.getServerState();

        DadkvsPaxos.PhaseOneRequest.Builder phase1Request = DadkvsPaxos.PhaseOneRequest.newBuilder();
        phase1Request.setPhase1Config(state.getConfig())
                .setPhase1Index(paxosRoundsProposed)
                .setPhase1Timestamp(state.getBallotNumber());

        for (DadkvsPaxosServiceStub acceptorStub : state.getAcceptorStubs()) {
            CollectorStreamObserver<DadkvsPaxos.PhaseOneReply> commit_observer = new CollectorStreamObserver<>(
                    commit_collector);
            acceptorStub.phaseone(phase1Request.build(), commit_observer);
            System.out.println("PROPOSER: Sending Phase1 request to server (order: " + paxosRoundsProposed + ")");
        }
    }

    private Integer waitPhaseOneReplies(GenericResponseCollector<DadkvsPaxos.PhaseOneReply> commit_collector,
            ArrayList<DadkvsPaxos.PhaseOneReply> phase1Responses) {

        Integer latestValue = null;
        int highestWriteTS = -1;
        WaitForMajority(commit_collector, super.getServerState().getNumAcceptors());
        for (PhaseOneReply phaseOneReply : phase1Responses) {
            if (!phaseOneReply.getPhase1Accepted()) {
                // there is a new leader with higher ID, phase1 rejected!
                return null;
            }

            System.out.println("PROPOSER: CHECKING: " + phaseOneReply.getPhase1Timestamp() + " > " + highestWriteTS);
            if (phaseOneReply.getPhase1Timestamp() > highestWriteTS) {
                highestWriteTS = phaseOneReply.getPhase1Timestamp();
                latestValue = phaseOneReply.getPhase1Value();
            }
        }

        if (latestValue == null) {
            System.out.println("PROPOSER: CHOOSING OWN VALUE");
            // chooses own value
            return chooseOwnProposeValue();
        } else {
            System.out.println("PROPOSER: VALUE ALREADY EXISTS");
            // choose the value with the highest writeTS
            return latestValue;
        }
    }

    /**
     * Sends phase2 to the Acceptors and waits for a majority of answers.
     * Returns true if it was successfull (accepted) or false otherwise
     */
    private boolean executesPhaseTwo(int reqId) {
        ArrayList<DadkvsPaxos.PhaseTwoReply> phase2_responses = new ArrayList<>();
        GenericResponseCollector<DadkvsPaxos.PhaseTwoReply> phase2_collector = new GenericResponseCollector<>(
                phase2_responses, super.getServerState().getNumAcceptors());

        sendPhaseTwo(reqId, phase2_collector);
        return waitPhaseTwoReplies(phase2_collector, phase2_responses);
    }

    private void sendPhaseTwo(int reqId, GenericResponseCollector<DadkvsPaxos.PhaseTwoReply> phase2_collector) {
        DadkvsServerState state = super.getServerState();

        DadkvsPaxos.PhaseTwoRequest.Builder phase2Request = DadkvsPaxos.PhaseTwoRequest.newBuilder();
        phase2Request.setPhase2Config(state.getConfig())
                .setPhase2Index(paxosRoundsProposed)
                .setPhase2Timestamp(state.getBallotNumber())
                .setPhase2Value(reqId);

        for (DadkvsPaxosServiceStub acceptorStub : state.getAcceptorStubs()) {
            CollectorStreamObserver<DadkvsPaxos.PhaseTwoReply> phase2_observer = new CollectorStreamObserver<>(
                    phase2_collector);
            acceptorStub.phasetwo(phase2Request.build(), phase2_observer);
            System.out.println("PROPOSER: Sending Phase2 request to server");
        }
    }

    private boolean waitPhaseTwoReplies(GenericResponseCollector<DadkvsPaxos.PhaseTwoReply> phase2_collector,
            ArrayList<DadkvsPaxos.PhaseTwoReply> phase2_responses) {
        WaitForMajority(phase2_collector, super.getServerState().getNumAcceptors());
        System.out.println("PROPOSER: Phase 2 responses majority was received");
        for (PhaseTwoReply phaseTwoReply : phase2_responses) {
            if (!phaseTwoReply.getPhase2Accepted()) {
                // there is a new leader with higher ID, phase2 rejected!
                return false;
            }
        }

        return true;
    }
}
