package dadkvs.server;

import java.util.concurrent.ConcurrentHashMap;

public class LearnCounter {
	private class LeaderLearnCounter {
		private int leaderId;
		private int counter;

		public LeaderLearnCounter(int leaderId) {
			this.leaderId = leaderId;
			this.counter = 0;
		}

		public synchronized int incrementCounter(int id) {
			if (this.leaderId != id) {
				this.counter = 0;
				this.leaderId = id;
			}
			this.counter++;
			return this.counter;
		}
	}
	
	private ConcurrentHashMap<Integer, LeaderLearnCounter> learnCounterMap;

	public LearnCounter() {
		learnCounterMap = new ConcurrentHashMap<>();
	}

    public synchronized int incrementCounter(int order, int id) {
		LeaderLearnCounter counter = learnCounterMap.get(order);

		if (counter == null) {
			counter = new LeaderLearnCounter(id);
			learnCounterMap.put(order, counter);
		}

		return counter.incrementCounter(id);
	}
}
