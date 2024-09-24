package dadkvs.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dadkvs.DadkvsMain;
import dadkvs.util.RequestArchive;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class DadkvsServer {

    static DadkvsServerState server_state;

    /**
     * Server host port.
     */
    private static int port;

    public static void main(String[] args) throws Exception {
        final int kvsize = 1000;

        System.out.println(DadkvsServer.class.getSimpleName());

        Map<Integer, RequestArchive<DadkvsMain.CommitRequest, DadkvsMain.CommitReply>> request_map = new HashMap<>();
        request_map = Collections.synchronizedMap(request_map);

        Map<Integer, Integer> request_ordered_map = new HashMap<>();
        request_ordered_map = Collections.synchronizedMap(request_ordered_map);

        int base_port = Integer.parseInt(args[0]);
        int my_id = Integer.parseInt(args[1]);

        server_state = new DadkvsServerState(kvsize, base_port, my_id);

        CommitHandler handler = new CommitHandler(request_map, request_ordered_map, server_state);

        port = base_port + my_id;

        final BindableService service_impl = new DadkvsMainServiceImpl(server_state, handler);
        final BindableService console_impl = new DadkvsConsoleServiceImpl(server_state);
        final BindableService paxos_impl = new DadkvsPaxosServiceImpl(server_state);
        final BindableService step1_impl = new DadkvsStep1ServiceImpl(handler);

        // Create a new server to listen on port.
        Server server = ServerBuilder.forPort(port).addService(service_impl).addService(console_impl)
                .addService(paxos_impl).addService(step1_impl).build();
        // Start the server.
        server.start();
        // Server threads are running in the background.
        System.out.println("Server started");

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }
}
