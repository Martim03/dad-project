package dadkvs.server;
import java.util.Map;

import dadkvs.DadkvsMain;


public class CommitHandler {
    int            requestsProcessed;
    Map<Integer, DadkvsMain.CommitRequest> request_map ;
	Map<Integer, Integer> request_order_map ;

    
    public CommitHandler(Map<Integer, DadkvsMain.CommitRequest> request_map, Map<Integer, Integer> request_order_map ) {
        this.requestsProcessed = 0;
        this.request_map = request_map;
		this.request_order_map = request_order_map;
    }

    public void addOrderedRequest(int order, int reqid){
        request_order_map.put(reqid,order);
        handleCommits();
    }

    public void addRequest(DadkvsMain.CommitRequest request){

        request_map.put(request.getReqid(),request);
        handleCommits();
    }

    public void handleCommits(){
        while( true ) {
			Integer requestid = request_order_map.get(this.requestsProcessed);
			if( !(requestid != null && request_map.containsKey(requestid)))  {
				continue; //isto somehow tem que estar sempre a correr até o servidor fazer outra cena
			}
			else {

				DadkvsMain.CommitRequest request = request_map.get(requestid) ;

				int reqid = request.getReqid();
				int key1 = request.getKey1();
				int version1 = request.getVersion1();
				int key2 = request.getKey2();
				int version2 = request.getVersion2();
				int writekey = request.getWritekey();
				int writeval = request.getWriteval();

				System.out.println("executing:\n reqid " + reqid + " key1 " + key1 + " v1 " + version1 + " k2 " + key2 + " v2 " + version2 + " wk " + writekey + " writeval " + writeval);

				this.requestsProcessed++;
				TransactionRecord txrecord = new TransactionRecord (key1, version1, key2, version2, writekey, writeval, this.requestsProcessed);
				boolean result = this.server_state.store.commit (txrecord);
				
                // for debug purposes
                System.out.println("Result is ready for request with reqid " + reqid);
                
                DadkvsMain.CommitReply response =DadkvsMain.CommitReply.newBuilder()
                    .setReqid(reqid).setAck(result).build();
            }
        }
    }
}
