package dadkvs.util;
public class PaxosProposal {
    private int writeTS;
    private int readTS;
    private int reqId;
    private boolean commited;

    public PaxosProposal() {
        this.reqId = -1;
        this.writeTS = 0;
        this.readTS = 0;
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

    public synchronized void setWriteTS(int writeTS) {
        this.writeTS = writeTS;
    }

    public synchronized void setReadTS(int readTS) {
        this.readTS = readTS;
    }

    public synchronized void setReqId(int reqId) {
        this.reqId = reqId;
    }

    public synchronized void setCommited(boolean commited) {
        this.commited = commited;
    }

    
}
