import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.function.ToDoubleFunction;

public class PointList extends ArrayList<Point2D.Double> {

	private ArrayList<Double> x = new ArrayList<>();
	public double[] getX() { return get(x); }
	private ArrayList<Double> y = new ArrayList<>();
	public double[] getY() { return get(y); }
	
	private double[] get(ArrayList<Double> array) {
		double[] tempArray = new double[array.size()];
		int i = 0;
		for(Double d : array) {
		  tempArray[i] = (double) d;
		  i++;
		}
		return tempArray;
	}
	
	@Override
	public boolean add(Point2D.Double e) {
		super.add(e);
		
		x.add(e.getX());
		y.add(e.getY());
		
		return true;
	}
	
	public double average() {
		return this.stream().mapToDouble(p -> p.y).average().orElse(Double.NaN);
	}
}
