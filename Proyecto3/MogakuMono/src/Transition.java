import java.io.Serializable;

public class Transition implements Serializable {

	double[] features;
	Double target;
	Integer action;

	public Transition(double[] dense_previous_features, Double target, Integer act) {
		features = dense_previous_features;
		this.target = target;
		action = act;
		// TODO Auto-generated constructor stub
	}

	public Integer getAction() {
		return action;
	}

	public void setAction(Integer action) {
		this.action = action;
	}

	public Double getTarget() {
		return target;
	}

	public void setTarget(Double target) {
		this.target = target;
	}

}
