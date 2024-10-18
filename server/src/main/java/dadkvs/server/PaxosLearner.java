package dadkvs.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator.LeapableGenerator;

import dadkvs.DadkvsMain;
import dadkvs.DadkvsPaxos;
import dadkvs.util.PaxosLog;
import dadkvs.util.PaxosProposal;
import dadkvs.util.RequestArchive;
import dadkvs.util.RequestArchiveStore;
import io.grpc.stub.StreamObserver;

public class PaxosLearner extends PaxosParticipant {
	private int requestsProcessed;
	LearnCounter learnCounter;

	public PaxosLearner(DadkvsServerState state, RequestArchiveStore requestArchiveStore, PaxosLog paxosLog) {
		super(state, requestArchiveStore, paxosLog);
		requestsProcessed = 0;
		learnCounter = new LearnCounter();
	}

	/**
	 * This is the request received from the leader to anounce the value to commit
	 * so the server will take the chosen value and commit it to the store, it is
	 * decided.
	 */
	public void receiveLearn(DadkvsPaxos.LearnRequest request,
			StreamObserver<DadkvsPaxos.LearnReply> responseObserver) {

		System.out.println("LEARNER: Receive learn request: " + request);

		// reply to the sender with "empty" message
		DadkvsPaxos.LearnReply.Builder learn_reply = DadkvsPaxos.LearnReply.newBuilder();
		responseObserver.onNext(learn_reply.build());
		responseObserver.onCompleted();

		int order = request.getLearnindex();
		int leaderId = request.getLearntimestamp();
		int learnCount = learnCounter.incrementCounter(order, leaderId);

		if (learnCount >= (super.getServerState().getNumAceptors() / 2 + 1)) {
			// having a majority can procede to commit the request for this order
			super.getPaxosLog().commitPropose(request.getLearnindex(),
					new PaxosProposal().setReadTS(request.getLearntimestamp()).setWriteTS(request.getLearntimestamp())
							.setReqId(request.getLearnvalue()).setCommited(true));
			System.out.println("::::::::::::::::::::::: NEW PROPOSAL --> " + "order: " + request.getLearnindex() + ", reqId: "
					+ request.getLearnvalue());

			// try to execute the request if all the requirements are met
			handleCommits();
		}
	}

	/** Learner receives client Request and stores it in RequestArchive */
	public void registerClientRequest(DadkvsMain.CommitRequest request,
			StreamObserver<DadkvsMain.CommitReply> responseObserver) {

		super.getRequestArchiveStore().addRequest(new RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>(
				request, responseObserver, request.getReqid()));

		handleCommits();
	}

	/**
	 * Learner executes all requests that are currently ready to be executed
	 * (after executing a request it will try to execute all consecutive requests
	 * that were "waiting").
	 * Note that since it always just tries to execute the next request based on
	 * how many requests it already executed , even if an already commited request
	 * is commited again it wont execute again because it wont check for
	 * older(comitted)
	 * requests, only the next ones
	 */
	private synchronized void handleCommits() {
		PaxosLog paxosLog = super.getPaxosLog();
		RequestArchiveStore requestArchiveStore = super.getRequestArchiveStore();
		Integer reqId;
		RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply> reqArchive;

		while ((reqId = paxosLog.getCommitedRequestId(requestsProcessed)) != null
				&& (reqArchive = requestArchiveStore.getRequest(reqId)) != null) {
			DadkvsMain.CommitRequest request = reqArchive.getRequest();

			int key1 = request.getKey1();
			int version1 = request.getVersion1();
			int key2 = request.getKey2();
			int version2 = request.getVersion2();
			int writekey = request.getWritekey();
			int writeval = request.getWriteval();

			System.out.println(
					"LEARNER: >>> EXECUTING:\n reqid " + reqId + " key1 " + key1 + " v1 " + version1 + " k2 " + key2
							+ " v2 "
							+ version2 + " wk " + writekey + " writeval " + writeval);

			TransactionRecord txrecord = new TransactionRecord(key1, version1, key2, version2, writekey, writeval,
					this.requestsProcessed);

			boolean commit_success = super.getServerState().getStore().commit(txrecord);

			//TODO IF WE WANT TO CLEAR THE LIST WE NEED TO UPDATE THE CURSOR
			//requestArchiveStore.removeRequest(reqId);

			this.requestsProcessed++;

			System.out.println(
					"LEARNER: Commit was " + (commit_success ? "SUCCESSFUL" : "ABORTED") + " for request with reqid "
							+ reqId);

			DadkvsMain.CommitReply response = DadkvsMain.CommitReply.newBuilder()
					.setReqid(reqId).setAck(commit_success).build();

			StreamObserver<DadkvsMain.CommitReply> responseObserver = reqArchive.getResponseObserver();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	public VersionedValue readStore(int key) {
		return super.getServerState().getStore().read(key);
	}
}
