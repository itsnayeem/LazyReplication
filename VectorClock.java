import java.util.HashMap;

public class VectorClock implements Comparable<VectorClock> {

	public HashMap<Integer, Integer> clock = new HashMap<Integer, Integer>();
	public static int NumElems = 0;
	
	public VectorClock() {
	}
	
	public VectorClock(int[] a) {
		for (int i = 0; i < NumElems; i++) {
			for (int j = 0; j < a[i]; j++) {
				increment(i);
			}
		}
	}
	
	public void increment() {
		increment(ServerStateManager.getID());
	}

	public void increment(int ID) {
		if (clock.containsKey(ID))
			clock.put(ID, clock.get(ID) + 1);
		else
			clock.put(ID, 1);
	}
	
	public void setValue(int ID, int value) {
		clock.put(ID, value);
	}

	public void mergeIn(VectorClock v) {

		for (Integer i : v.clock.keySet()) {
			if (!clock.containsKey(i) || clock.get(i) < v.clock.get(i)) {
				clock.put(i, v.clock.get(i));
			}
		}
	}
	
	public VectorClock getCopy() {
		VectorClock retval = new VectorClock();
		for (int i : clock.keySet()) {
			retval.setValue(i, clock.get(i).intValue());
		}
		return retval;
	}
	
	public String toString() {
		StringBuffer retval = new StringBuffer();
		retval.append("( ");
		for (int i = 0; i < NumElems; i++) {
			retval.append((clock.containsKey(i) ? clock.get(i) : 0) + " ");
		}
		retval.append(")");
		return new String(retval);
	}

	public int compareTo(VectorClock v) {

		boolean isEqual = true;
		boolean isGreater = true;
		boolean isSmaller = true;

		for (Integer i : clock.keySet()) {
			if (v.clock.containsKey(i)) {
				if (clock.get(i) < v.clock.get(i)) {
					isEqual = false;
					isGreater = false;
				} else if (clock.get(i) > v.clock.get(i)) {
					isEqual = false;
					isSmaller = false;
				}
			} else {
				isEqual = false;
				isSmaller = false;
			}
		}

		for (Integer i : v.clock.keySet()) {
			if (!clock.containsKey(i)) {
				isEqual = false;
				isGreater = false;
			}
		}

		// Return based on determined information.
		if (isEqual) {
			return 0;
		} else if (isGreater && !isSmaller) {
			return 1;
		} else if (isSmaller && !isGreater) {
			return -1;
		} else {
			System.out.println("Conflict");
			return 0;
		}
	}
}