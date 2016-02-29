import bwapi.Position;


public class MathUtil {
	
	/**
	 * Move from start location to destination by offset
	 * @param start
	 * @param end
	 * @param offset
	 * @return
	 */
	public static Position moveToLocation(Position start, Position end, int offset) {
		Position direction = new Position(end.getX() - start.getX(), end.getY() - start.getY());
		if (offset < 0) {
			offset = (int) start.getDistance(end) + offset;
		}
		Point vector = getUnitVector(direction).multiply(offset);
		return new Position(start.getX() + (int) vector.x, start.getY() + (int) vector.y); // TODO: loses precision but sc only handles ints as positions
		
	}

	private static Point getUnitVector(Position direction) {
		final float sqLength = getLengthSquared(direction.getX(), direction.getY());
		final float invLength = invSqrtFast(sqLength);
		return new Point(direction.getX() * invLength, direction.getY() * invLength);
	}
	
	public static float getLengthSquared(int x, int y) {
		return x * x + y * y;
	}
	
	public static float invSqrtFast(final float a) {
    	final float half_a = 0.5f*a;
        
        float ux = a;
        final int ui = 0x5f3759df - (Float.floatToIntBits(ux) >> 1);
        
        final float floatBits = Float.intBitsToFloat(ui);
        ux = floatBits * (1.5f - half_a * floatBits * floatBits);
        
        // After the first computation of 'ux' above, we no longer have to use 'floatBits'.
        // We can just use 'ux' as is.  Note that this line can be repeated arbitrarily 
        // many times to increase accuracy.  Based on a few tests, I see that two 
        // iterations produces the same result as 1 / Math.sqrt() does.
        ux = ux * (1.5f - half_a * ux * ux);
        
        return ux;
    }

}
