package dadkvs.server;

import dadkvs.util.PaxosLog;
import dadkvs.util.RequestArchiveStore;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class DadkvsServer {
    /**
     * Server host port.
     */
    private static int port;

    public static void main(String[] args) throws Exception {
        final int kvsize = 1000;

        System.out.println(DadkvsServer.class.getSimpleName());

        int base_port = Integer.parseInt(args[0]);
        int my_id = Integer.parseInt(args[1]);

        port = base_port + my_id;

        RequestArchiveStore requestArchiveStore = new RequestArchiveStore();
        PaxosLog paxosLog = new PaxosLog();
        DebugModes debug = new DebugModes();
        DadkvsServerState state = new DadkvsServerState(kvsize, base_port, my_id, debug);
        PaxosProposer proposer = new PaxosProposer(state, requestArchiveStore, paxosLog);
        PaxosAceptor aceptor = new PaxosAceptor(state, requestArchiveStore, paxosLog);
        PaxosLearner learner = new PaxosLearner(state, requestArchiveStore, paxosLog);

        final BindableService service_impl = new DadkvsMainServiceImpl(proposer, learner, debug);
        final BindableService paxos_impl = new DadkvsPaxosServiceImpl(aceptor, learner, debug);
        final BindableService console_impl = new DadkvsConsoleServiceImpl(state);
        
        // Create a new server to listen on port.
        Server server = ServerBuilder.forPort(port).addService(service_impl).addService(console_impl)
                .addService(paxos_impl).build();
        // Start the server.
        server.start();
        // Server threads are running in the background.
        System.out.println("Server started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server immediately...");
            server.shutdownNow(); // Force shutdown
            System.out.println("gRPC server shut down immediately.");
        }));

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }
}
