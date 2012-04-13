public class ServerState {

	private VectorClock serverClock;
	private int ID;
	private long lastSeen;
	public String ip;
	public int port;
	
	public ServerState(String ip, int port) {
		serverClock = new VectorClock();
		this.ip = ip;
		this.port = port;
		updateLastSeen();
	}
	
	public void updateLastSeen() {
		this.lastSeen = System.currentTimeMillis();
	}
	
	public long getLastSeen() {
		return lastSeen;
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}
	
	public int getID() {
		return ID;
	}
	
	public String toString() {
		return ID + ": IP: " + ip + ":" + port + " last seen: " + lastSeen;
	}
	
	public VectorClock getClockVal() {
		return serverClock;
	}
	
	public VectorClock getClockCopy() {
		VectorClock retval = null;
		retval = serverClock.getCopy();
		return retval;
	}
	
	public void incrementClock() {
		serverClock.increment();
	}

	public void mergeIn(VectorClock c) {
		serverClock.mergeIn(c);
	}
}
