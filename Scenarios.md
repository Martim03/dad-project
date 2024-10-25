SCENARIOS

In the first scenario, when a client request arrives before Paxos has initiated, the learner stores the request in the `RequestArchiveStore` but does not execute it immediately. The request will eventually be proposed during a Paxos round, and once it is learned, the transaction can be executed—provided all previous requests, according to Paxos, have already been completed.

EXECUTION:
- Just unset leaders (no leader), and it will make it so that the client requests are received but paxos doesnt start
- After the requests are received unfreeze the leader
- Verify the learners storing the values but not executing and only when the paxos was decided did they execute
----------------------------------------------------------------------------------------------------------


In the second scenario, Paxos may propose and reach consensus on a transaction before the actual client request arrives. From the **acceptor's perspective**, when this occurs, the consensus result is saved in the `PaxosLog`, but no execution takes place until the delayed client request arrives. Once the learner receives the request, it attempts to execute the transactions, ensuring the correct sequence of operations is preserved.

EXECUTION: 
- Freeze at least one of the acceptors (not the leader) so that it doesnt receive client requests
- However the frozen acceptor will still participate in the paxos and will know that it was decided (learner)
- Unfreeze the frozen acceptor and the client requests will "arrive" and see how the learner will now execute them because they are already paxos decided


-----------------------------------------------------------------------------------------------------------

Occasionally, **from the acceptor's perspective**, phase two of the Paxos process might occur before phase one. This is not an issue, as the acceptor can still process phase two. The acceptor checks if the leader's ID is greater than or equal to its current ID. If so, it accepts the proposal, denying any outdated leader attempts, and updates the read and write timestamps accordingly.

EXECUTION:
- Set leader off on server0 and leader on server2 because server0 wont do phase1 because its first leader
- IgnorePhase1Debug on 1 of the acceptors and see how it will manage to receive the phase2 first and unfold until the exectuion
- After the complete round and execution set IgnorePhase1Debug off and see it receiving the delayed phase1 and hanlding it gracefully (idempotent and idependent)


-----------------------------------------------------------------------------------------------------------

From the **learner's perspective**, it is possible to receive a "learn" message before any preceding Paxos messages have arrived. This is acceptable because the system is designed to handle multiple instances of learn or accept messages without altering the final outcome in the `PaxosLog`. The learner ensures that each request is executed only once in the correct order. The system handles commits sequentially, executing request *i* first, followed by request *i+1*, and so forth, guaranteeing that a request will not be executed again once processed.

EXECUTION:
- Set leader off on server0 and leader on server2 because server0 wont do phase1 because its first leader
- OnlyLearnsDebug on 1 of the acceptors/learners , and see how it will manage to receive the learn withou any prior paxos message and execute the requests
- Afterwards let the OnlyLearnsDebug off and see it receiving the delayed messages and it not influencing the outcome of the paxos nor executing again (idempotent and idependent)

-----------------------------------------------------------------------------------------------------------

Leaders cannot initiate a new Paxos round before achieving consensus on all previous rounds. The system enforces this by requiring leaders to know the outcome of every prior round before proceeding, ensuring they always make the correct decision and proscenariospose valid transactions.


EXECUTION: 
- leave server2 as acceptor only and freeze it so that it doenst receive clientRequests
- then do a bunch of transactions that will be proposed by another leader
- now setleade off for the other leader and make server2 the leader
- make a new transaction and see how it wasnt proposed because server2 hasnt yet updated its paxosLog due to lacking DATA(doesnt have the requests yet, cant know if there was a reconfig in previous rounds even though they are already commited)
- now unfreeze server2 and see how it will update its paxosLog untill the end without reproposing and then it will finally Propose the most recent request


-----------------------------------------------------------------------------------------------------------

If there are multiple leaders they will "fight" for the propose in a way that a leader will be invalidationg the others and so on, until eventually at least one of them can propose until the end

EXECUTION:
- simply set multiple servers and leaders and put them on slow mode
- make a transaction and watch them have to retry with higher ballot numbers until they make it


-----------------------------------------------------------------------------------------------------------

Reconfiguration to leader that is outside of the initial config

EXECUTION:
- leave the server0 as leader and make a reconfiguration request for the config 2 (2,3,4)
- now make a transaction and see how it isnt proposed because even though server0 is still a leader it is outside of the current config so it cant propose
- now set server4 as leader and watch it proposing the transaction


-----------------------------------------------------------------------------------------------------------

Even though client requests might reach different servers by different orders, they will still reach a consensus that make it so that the final order is the same (maybe do this test with 2 leaders at same time would be cool)

EXECUTION:
- set delays from c1->s2 and delays from c2->s1 , where s1 and s2 are both leaders with slow mode on
- watch how even though 2 leaders are proposing different values for the same order every server should execute the transactions in the same order.
- check that the 2 requests were commited with the same order for all the servers 

-----------------------------------------------------------------------------------------------------------


Note:
*When a configuration change occurs during an 'ongoing Paxos round', either the configuration change or a client request will be ordered and executed first by Paxos consensus. The system ensures that servers do not initiate a new Paxos round until knowing the last paxos round , so if it was a reconfiguration then the next propose will be done on the new configuration. This guarantees that Paxos messages are only sent to the correct acceptors for the current configuration. Additionally, before starting a new round, the leader verifies that it remains a valid leader under the updated configuration. Acceptors can trust the leader’s decisions, knowing that the leader is always operating within the correct configuration, so if this acceptor was included in the requests for this proposal it means that the acceptor is a participant of the configuration of the proposal.*