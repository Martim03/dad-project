package dadkvs.server;

import java.util.ArrayList;

import dadkvs.DadkvsPaxos;
import dadkvs.util.CollectorStreamObserver;
import dadkvs.util.GenericResponseCollector;
import dadkvs.util.PaxosLog;
import dadkvs.util.PaxosProposal;
import dadkvs.util.RequestArchiveStore;
import io.grpc.stub.StreamObserver;

public class PaxosAceptor extends PaxosParticipant {
	public PaxosAceptor(DadkvsServerState state, RequestArchiveStore requestArchiveStore, PaxosLog paxosLog) {
		super(state, requestArchiveStore, paxosLog);
	}

	/*
	 * This is the request received from the leader to prepare the transaction
	 * so the server will respond with the value of the highest accepted proposal
	 * for the leader to understand.
	 * Just reject the request if the server has already accepted a higher proposal.
	 */
	public void receivePhaseOne(DadkvsPaxos.PhaseOneRequest request,
			StreamObserver<DadkvsPaxos.PhaseOneReply> responseObserver) {

		System.out.println("Receive phase1 request: " + request);

		PaxosLog paxosLog = super.getPaxosLog();
		PaxosProposal currentProposal = paxosLog.getPropose(request.getPhase1Index());

		if (currentProposal == null) {
			// if it does not exist answer with default values (0, 0, -1)
			currentProposal = new PaxosProposal();
		}

		DadkvsPaxos.PhaseOneReply.Builder phase1Reply = DadkvsPaxos.PhaseOneReply.newBuilder();

		if (request.getPhase1Timestamp() < currentProposal.getReadTS()) {
			// reject the request
			phase1Reply.setPhase1Accepted(false);

			responseObserver.onNext(phase1Reply.build());
			responseObserver.onCompleted();
			return;
		}

		// Update the readTS of the request
		currentProposal.setReadTS(request.getPhase1Timestamp());

		phase1Reply.setPhase1Config(request.getPhase1Config())
				.setPhase1Index(request.getPhase1Index())
				.setPhase1Timestamp(currentProposal.getWriteTS())
				.setPhase1Value(currentProposal.getReqId())
				.setPhase1Accepted(true);

		responseObserver.onNext(phase1Reply.build());
		responseObserver.onCompleted();
	}

	/*
	 * This is the request received from the leader to anounce the chosen value
	 * so the server will respond with an OK if it hanst already accepted a higher
	 * proposal and update the request order with the new index (but not commited)
	 * OR
	 * just reject the request if the server has already accepted a higher proposal.
	 */
	public void receivePhaseTwo(DadkvsPaxos.PhaseTwoRequest request,
			StreamObserver<DadkvsPaxos.PhaseTwoReply> responseObserver) {

		System.out.println("Receive phase two request: idx=" + request.getPhase2Index() + " val="
				+ request.getPhase2Value() + " ts=" + request.getPhase2Timestamp());

		PaxosLog paxosLog = super.getPaxosLog();
		PaxosProposal currentProposal = paxosLog.getPropose(request.getPhase2Index());
		DadkvsPaxos.PhaseTwoReply.Builder phase2Reply = DadkvsPaxos.PhaseTwoReply.newBuilder();

		if (request.getPhase2Timestamp() < currentProposal.getReadTS()) {
			// reject the request
			System.out.println("Rejecting phase two request: idx=" + request.getPhase2Index() + " ts="
					+ request.getPhase2Timestamp());

			phase2Reply.setPhase2Accepted(false);

			responseObserver.onNext(phase2Reply.build());
			responseObserver.onCompleted();
			return;
		}

		System.out.println(
				"Accepting phase two request: idx=" + request.getPhase2Index() + " ts="
						+ request.getPhase2Timestamp());

		// Update the writeTS and readTS of the request
		currentProposal.setWriteTS(request.getPhase2Timestamp()).setReadTS(request.getPhase2Timestamp());

		phase2Reply.setPhase2Config(request.getPhase2Config())
				.setPhase2Index(request.getPhase2Index())
				.setPhase2Accepted(true);

		System.out.println(
				"Phase two request accepted: idx=" + request.getPhase2Index() + " ts="
						+ request.getPhase2Timestamp());

		responseObserver.onNext(phase2Reply.build());
		responseObserver.onCompleted();

		sendLearn(request.getPhase2Config(), request.getPhase2Index(), request.getPhase2Value(),
				request.getPhase2Timestamp());
	}

	private void sendLearn(int config, int order, int reqId, int leaderId) {
		DadkvsServerState state = super.getServerState();

		DadkvsPaxos.LearnRequest.Builder learnRequest = DadkvsPaxos.LearnRequest.newBuilder();
		learnRequest.setLearnconfig(config)
				.setLearnindex(order)
				.setLearnvalue(reqId)
				.setLearntimestamp(leaderId);

		ArrayList<DadkvsPaxos.LearnReply> learnResponses = new ArrayList<>();
		GenericResponseCollector<DadkvsPaxos.LearnReply> commit_collector = new GenericResponseCollector<>(
				learnResponses, state.getNumServers());

		for (int i = 0; i < state.getNumServers(); i++) {
			CollectorStreamObserver<DadkvsPaxos.LearnReply> commit_observer = new CollectorStreamObserver<>(
					commit_collector);
			state.getAsyncStubs()[i].learn(learnRequest.build(), commit_observer);
			System.out.println("Sending Learn request to server " + i);
		}
		// TODO figure out how to wait
	}
}
