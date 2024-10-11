"""

    State Machine Replication with Paxos

 ---------------------------------------------------------------------------

    
    DATA Structs

RequestArchStore: stores all the client requests received
    - get(id)
    - add(req)
    - getNext() // to get a value to purpose
    - remove(id) // for cleanup Purposes
    - THREAD SAFE

PaxosOrderer: keeps track of the agreed order to process the requests
    - getPropose(order) // phase1
    - setPropose(order) // phase2
    - commitPropose(order, val) // learn
    - THRAD SAFE

StateCommitHandler: has the task to execute(commit to the "DB") the requests when they 
                                    are ready (iscommited(order) && existsRequest(id)):
    - handleCommits() // should have a iterative while to ensure a chain reaction of commits
                         can happen, for example req3 and req4 were paxosReady and
                            the requestExisted but were waiting on req2 to be executed, 
                            so when req2 is completed it should also check for other waiting requests
                            , in this case req3 and req4
    - private currentStateOrder
    - THREAD SAFE

 ---------------------------------------------------------------------------

    PROTOCOL/MESSAGES

On the Receiver side:

Phase1(Prepare):
    - Ignore if id is low
    - Answer with the current proposal for the order(idx) in question: 
        [PaxosOrderer.getPropose(idx)], be aware of the cases where it comes as NULL
                        ^ maybe update here the readTS
    - Update ReadTS (with the promise leader id)

Phase2(Accept):
    - Ignore if id is low (See implication sof promising ids that are lower BUT are not the same as the previous readTS)
    - Write the proposed value for order(idx):
        [PaxosOrderer.setPropose(idx, val)]
            ^ maybe update here the writeTS (and the ReadTS???)
    - Update writeTS (And readTS???)
    - (Send Learn??? Check this optimization implications -> MUST CHANGE LEARN/COMMIT TO WAIT FOR A MAJORITY NOW!!!!)

Learn:
    - Commit the proposed value for order(idx) , if didnt exist in Paxos create??
        [PaxosOrderer.commitProposed(idx,val)] , must ignore a lot of things if is already committed !

Client Read: 
    - Does not interefere with anything? Needs no change?

Client committx:
    - Add the received request to the requestArchStore
        [RequestArchStore.add(req)]
    - Start Paxos if leader (maybe leave timeouts?)
    - Must be THREAD SAFE cant start 2 concurrent paxos

    
On the Sender side:

SendPhase1(SendPrepare):
    - Can be skipped for 1st leader(check for 2nd round)
    - Send Message to ACCEPTORS and wait Majority OR single decline message (which means there is a new leader)
    - If Majority answered all null, propose new value 
        [RequestArchStore.getNext()]
    - If Majority didnt answer all null then choose to propose the value with the highest writeTS (in sendPhase2)

SendPhase2(Accept):
    - Send Message of desired idx to ACCEPTORSamd wait Majority OR single decline (which means there is a new leader)
    - If Majority Accepted (ack) then send Learn with the choosen value

SendLearn:
    - Send Message of desired idx to LEARNERS , no need to wait for the responses

 ---------------------------------------------------------------------------

    THREAD SAFETY

RequestArchStore:
    - Ensure synchronized(ReadWrite?) access to the main data struct (CRUD operations)

StateCommitHandler:
    - Must be fully synchronized so it cant be called multiple instances at the same time,
     could lead to double commits or mess up order counter (use good checks inside of
       synchronized blocks to ensure no double actions happen) 
    - Be aware that it will call a lot of "Thread Risky" variables;
        From RequestArchStore is probably already safe BUT
        From commitHandler -> careful when reading states like iscommited and values, be synched

PaxosOrderer:
    - Simple Paxos: Protect access to "Order"
    - Multi Paxos: keep lock for each order position?
    - keep synchorinzied(order block) to ensure that no more than 1 instance of paxos is changing/reading
         the same Read/Write TS's or values at the same time, 
    - The dataStructure it self should ensure that each object's properties are only changed by 1 thread at a time

After lcok checks:
    - First thing to do when inside of synched blocks is checking if still makes sense(order, commited, etc...)

DeadLocks:
    - Could create deadlocks in the paxos leader because locks to send message an then receiving the
         same message might need the lock??

 ---------------------------------------------------------------------------

    MULTI-PAXOS

IGNORE FOR NOW


 ---------------------------------------------------------------------------

    Restrictions for paxos rounds

Leader:
    - Can only start round 'i' if dall the rounds previous to 'i' are commited in the leader Log 
        (if it isnt then maybe just go sleep until log is filled up to 'i' and keep looping this, 'Delayed Stop'?)

Acceptor:
    Can always participate in paxos rounds (even if Log has holes)

 ---------------------------------------------------------------------------

    Simplifications

RealWorld Assumptions(IGNORE THIS IMPLEMENTATION WE ARE GOING FOR THE SIMPLIFICATION): 
    - Receives a client request:
        - isLeader: startPaxos
        - NotLeader: startExpTimeout
            - If not committed when woke up then assime leaderance

    - Leader Executing Paxos:
        - Message was majority accepted -> Continue
        - got ANY refuse message:
            - exponential Timeout
            - If not committed when woke up then assime leaderance

SimpleProject Assumptions:
- Receives a client request:
        - isLeader: startPaxos
        - NotLeader: Do Nothing
            - If receives a setLeaderON -> startPaxos with the first uncommited values

    - Leader Executing Paxos:
        - Message was majority accepted -> Continue
        - got ANY refuse message:
            - dont wait any timeout just keep on repeating!
            - Eventually could receive a setLeaderOFF and just give up this process (use locks for the reading of isLeader)

 ---------------------------------------------------------------------------

    Reconfiguration

- Can decide on configMode using the state machine with a paxos consensus round for the DKVStore[0] = configMode
- Be careful with concurrency on the config variables used to check if leader, if learner, if etc...)
- When configMode is changed propagate the state variables so the code is "updated"
     (may be unnecessary if it always do THEAD SAFE reads on the config variables before executing)
- Are VIEWS needed? probabbly not for step 3
- Ensure concurrency between changin config and PaxosProcesses goes smooth (again just maybe always read 
                                                                                the variables when neeeded)
- Para o step 4 , see views, see vertical paxos, stoppable paxos, maybe centralized config unit

 ---------------------------------------------------------------------------

    Admin Console:

setLeader: when becomes the leader must ensure that the code is written in a way that it will start paxos
    for the rounds that are behind!

 ---------------------------------------------------------------------------

    Fault Tolerance and Scenarios

Scenario1: Client request arrives before paxos
    - save the request (requestArchStore) and dont execute

Scenario2: Paxos consensus for request didnt arrive
    - Save the consesus result (PaxosOrderer), dont execute

Scenario3: Phase2 before Phase1
    - No problem just accept Phase2 if it has a higher Id 
        (succesfully dening the outdated leader, ensure that the read and write TS are correctly updated)

Scenario4: learn before paxos messages:
    - just accept, and have some controll to check if things are already commited when the 
        other messages finally arrive

Scenario5: change config mode during paxos round
    - One of the messages (changeConf or ClientRequest) will be order first by Paxos consensus and then execute 
        in order. Now concurrent controll is key to stop the servers from starting next Paxos round before updating
        their config state so that they confirm they still are Acceotirs/Proposers before acting on the next Paxos round.

"""
