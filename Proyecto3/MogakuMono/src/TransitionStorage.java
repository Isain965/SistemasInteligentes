import java.io.Serializable;
import java.util.ArrayList;

public class TransitionStorage implements Serializable {
	ArrayList<double[]> features;
	ArrayList<Double> targets;
	ArrayList<Integer> acts;
	int size = 0;

	public ArrayList<double[]> getFeatures() {
		return features;
	}

	public void setFeatures(ArrayList<double[]> features) {
		this.features = features;
	}

	public ArrayList<Double> getTargets() {
		return targets;
	}

	public void setTargets(ArrayList<Double> targets) {
		this.targets = targets;
	}

	public ArrayList<Integer> getActs() {
		return acts;
	}

	public void setActs(ArrayList<Integer> acts) {
		this.acts = acts;
	}

	public TransitionStorage() {
		super();
		features = new ArrayList<double[]>();
		targets = new ArrayList<Double>();
		acts = new ArrayList<Integer>();
	}

}
