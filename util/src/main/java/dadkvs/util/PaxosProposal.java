package dadkvs.util;
public class PaxosProposal {
    public static final int UNDEFINED_VALUE = -1;
    
    private int writeTS;
    private int readTS;
    private int reqId;
    private boolean commited;

    public PaxosProposal() {
        this.reqId = UNDEFINED_VALUE;
        this.writeTS = UNDEFINED_VALUE;
        this.readTS = UNDEFINED_VALUE;
        this.commited = false;
    }

    public synchronized int getWriteTS() {
        return writeTS;
    }

    public synchronized int getReadTS() {
        return readTS;
    }

    public synchronized int getReqId() {
        return reqId;
    }

    public synchronized boolean isCommited() {
        return commited;
    }

    public synchronized PaxosProposal setWriteTS(int writeTS) {
        this.writeTS = writeTS;
        return this;
    }

    public synchronized PaxosProposal setReadTS(int readTS) {
        this.readTS = readTS;
        return this;
    }

    public synchronized PaxosProposal setReqId(int reqId) {
        this.reqId = reqId;
        return this;
    }

    public synchronized PaxosProposal setCommited(boolean commited) {
        this.commited = commited;
        return this;
    }

    
}
