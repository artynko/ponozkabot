import bwapi.Position;

public class Point {
	public float x;
	public float y;

	public Point(Position pos) {
		x = pos.getX();
		y = pos.getY();
	}

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Point multiply(float n) {
		return new Point(x * n, y * n);
	}

	public Point subtract(Point p) {
		return new Point(x - p.x, y - p.y);
	}

	public Point add(Point p) {
		return new Point(x + p.x, y + p.y);
	}

	public Position toPosition() {
		return new Position((int) x, (int) y);
	}
}
