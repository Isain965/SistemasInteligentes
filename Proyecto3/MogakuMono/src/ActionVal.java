
class ActionVal{
		
	int act_index;
	int act_weight;
	
	ActionVal(int act_index,int act_weight,double val){
		this.act_index = act_index;
		this.act_weight = act_weight;
		this.val = val;
	}
	/** get index of this act in the current possible actions*/
	public int getAct_index() {
		return act_index;
	}
	public void setAct_index(int act_index) {
		this.act_index = act_index;
	}
	public int getAct_weight() {
		return act_weight;
	}
	public void setAct_weight(int act_weight) {
		this.act_weight = act_weight;
	}
	
	double val;
	
}