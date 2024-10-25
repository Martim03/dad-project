# SCENARIOS


### Debug Modes

| Debug Mode                  | Code |
| --------------------------- | ---- |
| `CRASH`                     | 1    |
| `FREEZE`                    | 2    |
| `UNFREEZE`                  | 3    |
| `SLOW_MODE_ON`              | 4    |
| `SLOW_MODE_OFF`             | 5    |
| `BLOCK_PHASE_ONE`           | 6    |
| `UNBLOCK_PHASE_ONE`         | 7    |
| `BLOCK_PHASE_ONE_AND_TWO`   | 8    |
| `UNBLOCK_PHASE_ONE_AND_TWO` | 9    |


---

### Scenario 1:  Request Arrival Before Paxos Consensus

In the first scenario, when a client request arrives before Paxos has initiated, the learner stores the request in the `RequestArchiveStore` but does not execute it immediately. The request will eventually be proposed during a Paxos round, and once it is learned, the transaction can be executed—provided all previous requests, according to Paxos, have already been completed.

**EXECUTION:**
1. Just unset leaders (no leader), and it will make it so that the client requests are received but paxos doesn’t start.
2. After the requests are received unfreeze the leader.
3. Verify the learners storing the values but not executing and only when the paxos was decided did they execute.

---

### Scenario 2: Paxos Consensus Before Request Arrival

In the second scenario, Paxos may propose and reach consensus on a transaction before the actual client request arrives. From the **acceptor's perspective**, when this occurs, the consensus result is saved in the `PaxosLog`, but no execution takes place until the delayed client request arrives. Once the learner receives the request, it attempts to execute the transactions, ensuring the correct sequence of operations is preserved.

**EXECUTION:** 
1. Freeze at least one of the acceptors (not the leader) so that it doesn’t receive client requests.
2. However, the frozen acceptor will still participate in the paxos and will know that it was decided (learner).
3. Unfreeze the frozen acceptor, and the client requests will "arrive" and see how the learner will now execute them because they are already paxos decided.

---

### Scenario 3: Phase Two Before Phase One

Occasionally, **from the acceptor's perspective**, phase two of the Paxos process might occur before phase one. This is not an issue, as the acceptor can still process phase two. The acceptor checks if the leader's ID is greater than or equal to its current ID. If so, it accepts the proposal, denying any outdated leader attempts, and updates the read and write timestamps accordingly.

**EXECUTION:**
1. Set leader off on server0 and leader on server2 because server0 wont do phase1 because its first leader
2. `IgnorePhase1Debug` on 1 of the acceptors and see how it will manage to receive the phase2 first and unfold until the exectuion
3. After the complete round and execution set `IgnorePhase1Debug` off and see it receiving the delayed phase1 and hanlding it gracefully (idempotent and idependent)

---

### Scenario 4: Learn Messages Before Phase One and Phase Two

From the **learner's perspective**, it is possible to receive a "learn" message before any preceding Paxos messages have arrived. This is acceptable because the system is designed to handle multiple instances of learn or accept messages without altering the final outcome in the `PaxosLog`. The learner ensures that each request is executed only once in the correct order. The system handles commits sequentially, executing request *i* first, followed by request *i+1*, and so forth, guaranteeing that a request will not be executed again once processed.

**EXECUTION:**
1. Set leader off on server0 and leader on server2 because server0 wont do phase1 because its first leader
2. `OnlyLearnsDebug` on 1 of the acceptors/learners , and see how it will manage to receive the learn withou any prior paxos message and execute the requests
3. Afterwards let the `OnlyLearnsDebug` off and see it receiving the delayed messages and it not influencing the outcome of the paxos nor executing again (idempotent and idependent)

---

### Scenario 5: Leader Must Know the Previous Rounds Before Proposing The Next One

Leaders cannot initiate a new Paxos round before achieving consensus on all previous rounds. The system enforces this by requiring leaders to know the outcome of every prior round before proceeding, ensuring they always make the correct decision and propose valid transactions.

**EXECUTION:** 
1. Leave `server2` as acceptor only and freeze it so that it doesn’t receive client requests.
2. Then do a bunch of transactions that will be proposed by another leader.
3. Now set leader off for the other leader and make `server2` the leader.
4. Make a new transaction and see how it wasn’t proposed because `server2` hasn’t yet updated its `PaxosLog` due to lacking data (doesn’t have the requests yet, can’t know if there was a reconfig in previous rounds even though they are already committed).
5. Now unfreeze `server2` and see how it will update its `PaxosLog` until the end without reproposing, and then it will finally propose the most recent request.

---

### Scenario 6: Multiple Leaders Ballot Competition

If there are multiple leaders, they will "fight" for the propose in a way that a leader will be invalidating the others and so on, until eventually at least one of them can propose until the end.

**EXECUTION:**
1. Simply set multiple servers and leaders and put them on slow mode.
2. Make a transaction and watch them have to retry with higher ballot numbers until they make it.

---

### Scenario 7: Reconfiguration with External(different config) Leader

Reconfiguration to leader that is outside of the initial config

**EXECUTION:**
1. Leave the `server0` as leader and make a reconfiguration request for the config 2 (2,3,4).
2. Now make a transaction and see how it isn’t proposed because even though `server0` is still a leader it is outside of the current config so it can’t propose.
3. Now set `server4` as leader and watch it proposing the transaction.

---

### Scenario 8: Consistent Ordering Even With Different Leaders Receiving/Proposing Requests By Different Order

Even though client requests might reach different servers by different orders, they will still reach a consensus that makes it so that the final order is the same (maybe do this test with 2 leaders at same time would be cool).

**EXECUTION:**
1. Set delays from `c1->s2` and delays from `c2->s1`, where `s1` and `s2` are both leaders with slow mode on.
2. Watch how even though 2 leaders are proposing different values for the same order, every server should execute the transactions in the same order.
3. Check that the 2 requests were committed with the same order for all the servers.

---

**Note:**

> *When a configuration change occurs during an 'ongoing Paxos round', either the configuration change or a client request will be ordered and executed first by Paxos consensus. The system ensures that servers do not initiate a new Paxos round until knowing the last paxos round, so if it was a reconfiguration then the next propose will be done on the new configuration. This guarantees that Paxos messages are only sent to the correct acceptors for the current configuration. Additionally, before starting a new round, the leader verifies that it remains a valid leader under the updated configuration. Acceptors can trust the leader’s decisions, knowing that the leader is always operating within the correct configuration, so if this acceptor was included in the requests for this proposal it means that the acceptor is a participant of the configuration of the proposal.*
