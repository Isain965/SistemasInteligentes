import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import simulator.Simulator;
import struct.FrameData;

public class Agent {
	Simulator simulator;
	int features_number;
	double[] features;
	double[] previous_features;
	double[] eTraces;
	double discount_factor;
	public double e;
	double alpha;
	double lastVal;
	boolean use_Q_Function = true;

	GameState state;
	ArrayList actions_weigths = new ArrayList();
	// ArrayList opp_actions_weigths = new ArrayList();
	Random r = new Random();
	boolean debug;
	double lambda;
	boolean use_expReplay;
	CircularFifoQueue<Transition> storage;
	int max_batch_size = 10000;
	int batch_update_size = 32;
	boolean player;

	public Agent(GameState state, double e, double df, double a, double l, int feat_len, boolean q, boolean player,
			boolean use_expReplay, boolean debug) {
		this.state = state;
		simulator = this.state.gd.getSimulator();
		System.out.println("teste");
		this.e = e;
		discount_factor = df;
		alpha = a;
		lambda = l;
		this.debug = debug;
		eTraces = new double[feat_len];
		this.use_Q_Function = q;
		this.use_expReplay = use_expReplay;
		storage = new CircularFifoQueue<Transition>(max_batch_size);
		System.out.println("Storage size: " + storage.maxSize());
		this.player = player;
	}

	void fillTraces(double[] feat) {
		for (int i = 0; i < feat.length; i++) {
			if (feat[i] > 0) {
				eTraces[i] += 1;
			}
		}
	}

	double[] decayTraces(double[] feat) {
		if (lambda > 0) {
			for (int i = 0; i < feat.length; i++) {
				if (feat[i] > 0) {
					eTraces[i] = eTraces[i] * discount_factor * lambda;
				} else if (feat[i] == 0) {
					eTraces[i] = 0;
				}
			}
			return eTraces;
		} else {
			return feat;
		}
	}

	void setMultipleWeights(ArrayList weigths) {
		actions_weigths = weigths;
	}

	/**
	 * Avalia acoes disponivels no estado atual em state.myIndexActions,
	 * retornando o index correspondente do state.myActions
	 */
	ActionVal getActFromPolicy(double reward) {
		Deque<ActionBehaviours> myAction = new LinkedList<ActionBehaviours>();
		int myOriginalHp = state.myChar.getHp();
		int oppOriginalHp = state.opp.getHp();
		double randomValue = 0 + (1 - 0) * r.nextDouble();
		// So pega uma acao aleatoria dentre as disponiveis em myActions
		if (randomValue <= this.e) {
			int max = state.myIndexActions.size();
			int randomAct = ThreadLocalRandom.current().nextInt(0, max);
			myAction.clear();
			myAction.add(state.myActions.get(randomAct));

			// double qVal =getSimDotProduct(myAction,reward);
			double qVal = getDotProduct(state.myIndexActions.get(randomAct), reward, myAction);

			ActionVal action = new ActionVal(randomAct, state.myIndexActions.get(randomAct), qVal);
			return action;
		}
		double qMaxVal = -999999999;
		int choosenAct = 0;
		int choosenAct_index = 0;
		// System.out.println("Values: ");
		for (int i = 0; i < state.myIndexActions.size(); i++) {
			myAction.clear();
			myAction.add(state.myActions.get(i));
			double qVal = getDotProduct(state.myIndexActions.get(i), reward, myAction);
			// System.out.print(qVal+", ");
			if (qVal > qMaxVal) {
				choosenAct = state.myIndexActions.get(i);
				choosenAct_index = i;
				qMaxVal = qVal;
			}
		}

		ActionVal action = new ActionVal(choosenAct_index, choosenAct, qMaxVal);
		return action;

	}

	ActionVal update(FrameData frameData, double reward, int act) {
		ActionVal next_action;
		double[] weight;
		weight = (double[]) actions_weigths.get(act); // Multiple regression

		// frameData = simulator.simulate(frameData, this.player, null, null,
		// 14);
		double[] f = state.getParameters(frameData, reward);
		fillTraces(f);

		// state.dense_features.setData(decayTraces(f));
		state.features = f;

		// long startTime = System.nanoTime();
		next_action = getActFromPolicy(reward);

		int activeFeatures = 0;
		for (int i = 0; i < f.length; i++) {
			if (f[i] == 1) {
				activeFeatures++;
			}
		}
		double new_alpha = 0;
		if (activeFeatures == 0) {
			new_alpha = alpha;
		} else {
			new_alpha = alpha / activeFeatures;
		}

		double td_target = reward + discount_factor * next_action.val;
		double delta = td_target - lastVal;
		double fact1 = new_alpha * delta;
		// System.out.println("fact: "+fact1);
		// System.out.println("Next_action.val: "+next_action.val);
		// Co.addEquals(weight,fact1,state.previous_features);
		update(weight, fact1, state.previous_features);
		// System.out.println("Weight: "+weight);
		if (use_expReplay) {
			// System.out.println(storage.size());
			// realizar varios updates com exp replay
			if (storage.size() >= max_batch_size) {
				// System.out.println("NOW WITH EXP REPLAY!!");

				// System.exit(0);
				batchUpdates(batch_update_size);
			}
			// armazena o novo update
			storage.add(new Transition(state.previous_features, fact1, act));

		}
		lastVal = next_action.val;
		state.previous_features = state.features;
		return next_action;
	}

	public void batchUpdates(int size) {
		Random random = new Random();
		Set<Integer> intSet = new HashSet<>();
		int storageSize = storage.maxSize();
		while (intSet.size() < size) {
			int rd = random.nextInt(storageSize);
			intSet.add(rd);
		}
		// ArrayList<Transition> batch = new ArrayList<>(size);
		Iterator<Integer> iter = intSet.iterator();
		while (iter.hasNext()) {
			Transition trans = storage.get(iter.next());
			double[] weight = (double[]) actions_weigths.get(trans.action);
			update(weight, trans.target, trans.features);
			// Co.addEquals(weight,trans.target,trans.features);
		}
	}

	void update(double[] w, double a, double[] feat) {
		for (int i = 0; i < w.length; i++) {
			w[i] = w[i] + state.features[i] * a;
		}

	}

	/** Prediction of the given act from one of act weights */
	double getDotProduct(int qAct_index, double r, Deque<ActionBehaviours> myAction) {
		double[] weight = (double[]) actions_weigths.get(qAct_index);

		double sum = 0;
		for (int i = 0; i < weight.length; i++) {
			sum += weight[i] * state.features[i];
		}
		return sum;
	}

	public int getScore(FrameData fd, int myOriginalHp, int oppOriginalHp) {
		int diffHpOp = 0;
		int diffHpMy = 0;
		if (state.p) {
			diffHpOp = Math.abs(fd.getCharacter(false).getHp() - oppOriginalHp);
			diffHpMy = Math.abs(fd.getCharacter(true).getHp() - myOriginalHp);
		} else {
			diffHpOp = Math.abs(fd.getCharacter(true).getHp() - oppOriginalHp);
			diffHpMy = Math.abs(fd.getCharacter(false).getHp() - myOriginalHp);
		}
		if (diffHpMy == diffHpOp && diffHpMy != 0) {
			return -1;
		} else {
			return diffHpOp - diffHpMy;
		}
	}

}
