package dadkvs.server;

import dadkvs.DadkvsMain;
import dadkvs.DadkvsPaxos;
import dadkvs.util.PaxosLog;
import dadkvs.util.PaxosProposal;
import dadkvs.util.RequestArchive;
import dadkvs.util.RequestArchiveStore;
import io.grpc.stub.StreamObserver;

public class PaxosLearner extends PaxosParticipant {
	private int requestsProcessed;

	public PaxosLearner(DadkvsServerState state, RequestArchiveStore requestArchiveStore, PaxosLog paxosLog) {
		super(state, requestArchiveStore, paxosLog);
		requestsProcessed = 0;
	}

	/**
	 * This is the request received from the leader to anounce the value to commit
	 * so the server will take the chosen value and commit it to the store, it is
	 * decided.
	 */
	public void receiveLearn(DadkvsPaxos.LearnRequest request, StreamObserver<DadkvsPaxos.LearnReply> responseObserver) {
		System.out.println("Receive learn request: " + request);

		// TODO wait for majority

		super.getPaxosLog().commitPropose(requestsProcessed,
				new PaxosProposal().setReadTS(request.getLearntimestamp()).setWriteTS(request.getLearntimestamp())
						.setReqId(request.getLearnvalue()).setCommited(true));

		handleCommits();

		DadkvsPaxos.LearnReply.Builder learn_reply = DadkvsPaxos.LearnReply.newBuilder();

		responseObserver.onNext(learn_reply.build());
		responseObserver.onCompleted();
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
	 * that were "waiting")
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
					">>> EXECUTING:\n reqid " + reqId + " key1 " + key1 + " v1 " + version1 + " k2 " + key2 + " v2 "
							+ version2 + " wk " + writekey + " writeval " + writeval);

			TransactionRecord txrecord = new TransactionRecord(key1, version1, key2, version2, writekey, writeval,
					this.requestsProcessed);

			boolean commit_success = super.getServerState().getStore().commit(txrecord);

			requestArchiveStore.removeRequest(reqId);

			this.requestsProcessed++;

			System.out.println(
					"Commit was " + (commit_success ? "SUCCESSFUL" : "ABORTED") + " for request with reqid "
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
