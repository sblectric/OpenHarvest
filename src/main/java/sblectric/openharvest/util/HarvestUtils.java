package sblectric.openharvest.util;

public class HarvestUtils {
	
	/** The maximum of a list of numbers */
	public static int max(Iterable<Integer> nums) {
		int prevMax = Integer.MIN_VALUE;
		for(int i : nums) {
			prevMax = Math.max(i, prevMax);
		}
		return prevMax;
	}
	
	/** The minimum of a list of numbers */
	public static int min(Iterable<Integer> nums) {
		int prevMin = Integer.MAX_VALUE;
		for(int i : nums) {
			prevMin = Math.min(i, prevMin);
		}
		return prevMin;
	}

}
