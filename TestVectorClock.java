import java.util.ArrayList;
import java.util.Collections;


public class TestVectorClock {

	public static void main(String[] args) {
		ArrayList<VectorClock> clocks = new ArrayList<VectorClock>();
		VectorClock.NumElems = 2;
		
		clocks.add(new VectorClock(new int[]{2, 0}));
		clocks.add(new VectorClock(new int[]{2, 4}));
		clocks.add(new VectorClock(new int[]{3, 4}));
		clocks.add(new VectorClock(new int[]{2, 3}));
		
		Collections.sort(clocks);
		
		System.out.println(clocks);
	}

}
