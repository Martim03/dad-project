package dadkvs.server;

import dadkvs.DadkvsPaxosServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class DadkvsServerState {

    final int INITIAL_CONFIG = 0;
    final int[][] CONFIG_MEMBERS = { { 0, 1, 2 }, { 1, 2, 3 }, { 2, 3, 4 } };
    final int INITIAL_LEADER_ID = CONFIG_MEMBERS[INITIAL_CONFIG][0];

    private int config;
    private boolean i_am_leader;
    private int debug_mode;
    private int base_port;
    private int my_id;
    private int store_size;
    private KeyValueStore store;
    private MainLoop main_loop;
    private Thread main_loop_worker;
    private int num_servers;
    private ManagedChannel[] channels;
    private DadkvsPaxosServiceGrpc.DadkvsPaxosServiceStub[] async_stubs;

    public DadkvsServerState(int kv_size, int port, int myself) {
        base_port = port;
        my_id = myself;
        i_am_leader = myself == INITIAL_LEADER_ID;
        config = INITIAL_CONFIG;
        debug_mode = 0;
        store_size = kv_size;
        store = new KeyValueStore(kv_size);
        main_loop = new MainLoop(this);
        main_loop_worker = new Thread(main_loop);
        main_loop_worker.start();
        this.num_servers = 5;
        this.channels = new ManagedChannel[this.num_servers];
        this.async_stubs = new DadkvsPaxosServiceGrpc.DadkvsPaxosServiceStub[this.num_servers];
        startComms();
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

    public synchronized int getConfig() {
        return config;
    }

    public synchronized void setConfig(int config) {
        this.config = config;
    }

    public synchronized int[] getConfigMembers() {
        return CONFIG_MEMBERS[config];
    }

    public KeyValueStore getStore() {
        return store;
    }

    public synchronized boolean isLeader() {
        return i_am_leader;
    }

    public synchronized void setLeader(boolean leader) {
        i_am_leader = leader;
    }

    public synchronized boolean isOnlyLearner() {
        for (int member : CONFIG_MEMBERS[config]) {
            if (my_id == member) {
                return false;
            }
        }
        return true;
    }

    public synchronized void setId(int my_id) {
        this.my_id = my_id;
    }

    public synchronized void incrementId() {
        setId(getId() + getNumServers());
    }

    public synchronized int getId() {
        return my_id;
    }

    public synchronized int getNumServers() {
        return num_servers;
    }

    public synchronized DadkvsPaxosServiceGrpc.DadkvsPaxosServiceStub[] getAsyncStubs() {
        return async_stubs;
    }

    public synchronized int getNumLearners() {
        return getNumServers();
    }

    public synchronized int getNumAceptors() {
        return CONFIG_MEMBERS[this.getConfig()].length;
    }
}
