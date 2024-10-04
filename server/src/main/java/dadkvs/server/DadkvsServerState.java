package dadkvs.server;

public class DadkvsServerState {

    final int INITIAL_CONFIG = 0;
    final int[][] CONFIG_MEMBERS = { { 0, 1, 2 }, { 1, 2, 3 }, { 2, 3, 4 } };
    final int INITIAL_LEADER_ID = CONFIG_MEMBERS[INITIAL_CONFIG][0];

    int config;
    boolean i_am_leader;
    int debug_mode;
    int base_port;
    int my_id;
    int store_size;
    KeyValueStore store;
    MainLoop main_loop;
    Thread main_loop_worker;

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

}
