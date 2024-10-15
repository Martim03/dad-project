package dadkvs.util;

import java.util.concurrent.ConcurrentHashMap;

import dadkvs.DadkvsMain;

public class PaxosOrdered {

    private ConcurrentHashMap<Integer, PaxosProposal> orderedRequestsMap;

    public PaxosOrdered() {
        orderedRequestsMap = new ConcurrentHashMap<>();
    }

    /** (Aceptor POV) Called by PhaseOne: Returns the current propose (reqId, readTs, writeTs), 
     * if its null there is no proposal so the Proposer must choose */
    public PaxosProposal getPropose(int order) {
        return orderedRequestsMap.get(order);
    }

    /** (Aceptor POV) Called by PhaseTwo: Sets a proposal for a given order */
    public void setPropose(int order, PaxosProposal propose) {
        orderedRequestsMap.put(order, propose);
    }

    /** (Leaner POV) Called by Learn: Commits proposal for a given order */
    public void commitPropose(int order, PaxosProposal propose) {
        propose.setCommited(true);
        setPropose(order, propose);
    }

    /** (Leaner POV) Called by Learner: Get current proposal requestId, if its not commited or does not exist return null */
    public Integer getCommitedRequestId(int order) {
        PaxosProposal prop = this.getPropose(order);
        if (prop != null && prop.isCommited()) {
            return orderedRequestsMap.get(order).getReqId();
        }

        return null;
    }
}
