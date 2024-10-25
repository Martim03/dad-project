package dadkvs.server;

import dadkvs.DadkvsPaxos;
import dadkvs.DadkvsPaxosServiceGrpc;
import io.grpc.stub.StreamObserver;

public class DadkvsPaxosServiceImpl extends DadkvsPaxosServiceGrpc.DadkvsPaxosServiceImplBase {
	// TODO VERIFY SYNCHRONIZED VS LOCKS

	PaxosAceptor aceptor;
	PaxosLearner learner;
	DebugModes debug;

	public DadkvsPaxosServiceImpl(PaxosAceptor aceptor, PaxosLearner learner, DebugModes debug) {
		this.aceptor = aceptor;
		this.learner = learner;
		this.debug = debug;
	}

	@Override
	public void phaseone(DadkvsPaxos.PhaseOneRequest request,
			StreamObserver<DadkvsPaxos.PhaseOneReply> responseObserver) {

		debug.applyDebugMode(RequestTypes.PHASE_ONE_REQUEST);

		System.out.println("Receive phase1 request: " + request);
		aceptor.receivePhaseOne(request, responseObserver);
	}

	@Override
	public void phasetwo(DadkvsPaxos.PhaseTwoRequest request,
			StreamObserver<DadkvsPaxos.PhaseTwoReply> responseObserver) {

		debug.applyDebugMode(RequestTypes.PHASE_TWO_REQUEST);

		System.out.println("Receive phase two request: idx=" + request.getPhase2Index() + " val="
				+ request.getPhase2Value() + " ts=" + request.getPhase2Timestamp());

		aceptor.receivePhaseTwo(request, responseObserver);
	}

	@Override
	public void learn(DadkvsPaxos.LearnRequest request, StreamObserver<DadkvsPaxos.LearnReply> responseObserver) {
		debug.applyDebugMode(RequestTypes.LEARN_REQUEST);
		System.out.println("Receive learn request: " + request);

		learner.receiveLearn(request, responseObserver);
	}

}
