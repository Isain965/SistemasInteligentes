package foo;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.CommandCenter;
import enumerate.Action;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.MotionData;

public class ExtendedNode {

	public static final int UCT_TIME = 165 * 100000;

	public static final double UCB_C = 3;

	// public static final int UCT_TREE_DEPTH = 2;
	public static final int UCT_TREE_DEPTH = 1;

	// public static final int UCT_CREATE_NODE_THRESHOULD = 10;
	public static final int UCT_CREATE_NODE_THRESHOULD = 5;

	public static final int SIMULATION_TIME = 60;

	private Random rnd;

	private ExtendedNode parent;

	private ExtendedNode[][] children;

	private int depth;

	private int games;

	private double score;

	private LinkedList<Action> myActions;

	private LinkedList<Action> oppActions;

	private Simulator simulator;

	private LinkedList<Action> selectedMyActions;

	private LinkedList<Action> selectedOppActions;

	private int myOriginalHp;

	private int oppOriginalHp;

	private FrameData frameData;
	private boolean playerNumber;
	private CommandCenter commandCenter;
	private GameData gameData;

	private boolean isCreateNode;

	Deque<Action> mAction;
	Deque<Action> oppAction;

	private int myEnergy;
	private int oppEnergy;
	CharacterName charName;

	public ExtendedNode(CharacterName charName, int myEnergy, int oppEnergy, FrameData frameData, ExtendedNode parent,
			LinkedList<Action> myActions, LinkedList<Action> oppActions, GameData gameData, boolean playerNumber,
			CommandCenter commandCenter, LinkedList<Action> selectedMyActions, LinkedList<Action> selectedOppActions) {
		this(charName, myEnergy, oppEnergy, frameData, parent, myActions, oppActions, gameData, playerNumber,
				commandCenter);

		this.selectedMyActions = selectedMyActions;
		this.selectedOppActions = selectedOppActions;

	}

	public ExtendedNode(CharacterName charName, int myEnergy, int oppEnergy, FrameData frameData, ExtendedNode parent,
			LinkedList<Action> myActions, LinkedList<Action> oppActions, GameData gameData, boolean playerNumber,
			CommandCenter commandCenter) {
		this.charName = charName;
		this.myEnergy = myEnergy;
		this.oppEnergy = oppEnergy;
		this.frameData = frameData;
		this.parent = parent;
		this.myActions = myActions;
		this.oppActions = oppActions;
		this.gameData = gameData;
		this.simulator = new Simulator(gameData);
		this.playerNumber = playerNumber;
		this.commandCenter = commandCenter;

		this.selectedMyActions = new LinkedList<Action>();
		this.selectedOppActions = new LinkedList<Action>();

		this.rnd = new Random();
		this.mAction = new LinkedList<Action>();
		this.oppAction = new LinkedList<Action>();

		CharacterData myCharacter = frameData.getCharacter(playerNumber);
		CharacterData oppCharacter = frameData.getCharacter(!playerNumber);
		myOriginalHp = myCharacter.getHp();
		oppOriginalHp = oppCharacter.getHp();

		if (this.parent != null) {
			this.depth = this.parent.depth + 1;
		} else {
			this.depth = 0;
		}
	}

	public int sumGames(ExtendedNode[] nodes) {
		int sum = 0;
		for (ExtendedNode node : nodes)
			sum += node.games;
		return sum;
	}

	public int sumScore(ExtendedNode[] nodes) {
		int sum = 0;
		for (ExtendedNode node : nodes)
			sum += node.score;
		return sum;
	}

	public Action mcts() {
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= UCT_TIME;) {
			uct();
		}

		return getBestVisitAction();
	}

	static final int playout_turn = 3;

	public double playout() {
		mAction.clear();
		oppAction.clear();

		int tmp_playout_turn = (selectedMyActions.size() > playout_turn ? selectedMyActions.size() : playout_turn);
		for (int i = 0; i < selectedMyActions.size(); i++) {
			mAction.add(selectedMyActions.get(i));
		}

		for (int i = 0; i < tmp_playout_turn - selectedMyActions.size(); i++) {
			mAction.add(myActions.get(rnd.nextInt(myActions.size())));
		}
		for (int i = 0; i < selectedOppActions.size(); i++) {
			oppAction.add(selectedOppActions.get(i));
		}

		for (int i = 0; i < tmp_playout_turn - selectedOppActions.size(); i++) {
			oppAction.add(oppActions.get(rnd.nextInt(oppActions.size())));
		}

		FrameData nFrameData = simulator.simulate(frameData, playerNumber, mAction, oppAction, SIMULATION_TIME);

		return getScore(nFrameData);
	}

	public double uct() {

		ExtendedNode[] selectedNodes = null;
		double bestUcb;

		bestUcb = -99999;

		for (ExtendedNode[] child : this.children) {
			int sumgames = sumGames(child);
			double ucb;
			if (sumgames == 0) {
				ucb = 9999 + rnd.nextInt(50);
			} else {
				ucb = getUcb(sumScore(child) / sumgames, games, sumgames);
			}

			if (bestUcb < ucb) {
				selectedNodes = child;
				bestUcb = ucb;
			}

		}
		ExtendedNode selectedNode = null;
		bestUcb = -99999;
		for (ExtendedNode child : selectedNodes) {
			double ucb;
			if (child.games == 0) {
				ucb = 9999 + rnd.nextInt(50);
			} else {
				ucb = getUcb(-child.score / child.games, sumGames(selectedNodes), child.games);
			}

			if (bestUcb < ucb) {
				selectedNode = child;
				bestUcb = ucb;
			}

		}

		double score = 0;
		if (selectedNode.games == 0) {
			score = selectedNode.playout();
		} else {
			if (selectedNode.children == null) {
				if (selectedNode.depth < UCT_TREE_DEPTH) {
					if (UCT_CREATE_NODE_THRESHOULD <= selectedNode.games) {
						selectedNode.createNode();
						selectedNode.isCreateNode = true;
						score = selectedNode.uct();
					} else {
						score = selectedNode.playout();
					}
				} else {
					score = selectedNode.playout();
				}
			} else {
				if (selectedNode.depth < UCT_TREE_DEPTH) {
					score = selectedNode.uct();
				} else {
					selectedNode.playout();
				}
			}

		}

		selectedNode.games++;
		selectedNode.score += score;

		if (depth == 0) {
			games++;
		}

		return score;
	}

	public void createNode() {

		ArrayList<MotionData> myMotion = gameData.getMotionData(playerNumber);
		ArrayList<MotionData> oppMotion = gameData.getMotionData(!playerNumber);
		this.children = new ExtendedNode[myActions.size()][oppActions.size()];
		for (int i = 0; i < myActions.size(); i++) {
			int myEnergy = this.myEnergy;
			LinkedList<Action> my = new LinkedList<Action>();
			for (Action act : selectedMyActions) {
				my.add(act);
			}

			Action mai = myActions.get(i);
			int maiEnergy = Math.abs(myMotion.get(mai.ordinal()).getAttackStartAddEnergy());

			if (maiEnergy > myEnergy)
				continue;
			my.add(mai);

			for (int j = 0; j < oppActions.size(); j++) {
				LinkedList<Action> opp = new LinkedList<Action>();
				for (Action act : selectedOppActions) {
					opp.add(act);
				}

				Action oaj = oppActions.get(j);
				int oajEnergy = Math.abs(oppMotion.get(oaj.ordinal()).getAttackStartAddEnergy());
				if (oajEnergy > oppEnergy)
					continue;
				opp.add(oaj);

				children[i][j] = new ExtendedNode(charName, myEnergy - maiEnergy, oppEnergy - oajEnergy, frameData,
						this, myActions, oppActions, gameData, playerNumber, commandCenter, my, opp);
			}
		}
	}

	public Action getBestVisitAction() {

		int selected = -1;
		double bestGames = -9999;

		for (int i = 0; i < children.length; i++) {

			int tmpgames = sumGames(children[i]);

			if (bestGames < tmpgames) {
				bestGames = tmpgames;
				selected = i;
			}

		}

		return this.myActions.get(selected);
	}

	public Action getBestScoreAction() {

		int selected = -1;
		double bestScore = -9999;
		for (int i = 0; i < children.length; i++) {

			int sumgames = sumGames(children[i]);
			if (sumgames == 0)
				continue;
			double meanScore = sumScore(children[i]) / sumgames;
			if (bestScore < meanScore) {
				bestScore = meanScore;
				selected = i;
			}

		}

		return this.myActions.get(selected);
	}

	public int getScore(FrameData fd) {

		CharacterData myP = frameData.getCharacter(playerNumber);
		CharacterData oppP = frameData.getCharacter(!playerNumber);
		int baseScore = (myP.getHp() - myOriginalHp) - (oppP.getHp() - oppOriginalHp);
		int myPX = (myP.getLeft() + myP.getRight()) / 2;
		int oppPX = (oppP.getLeft() + oppP.getRight()) / 2;
		int distanceX = Math.abs(myPX - oppPX);
		if (distanceX < 50)
			distanceX = 50;
		if (this.charName == CharacterName.LUD) {
			if (myOriginalHp - oppOriginalHp <= 0)
				return baseScore * 100 - distanceX;
			else
				return baseScore + (myP.getHp() - myOriginalHp);
		} else {
			return baseScore;
		}

	}

	public double getUcb(double score, int n, int ni) {
		return score + UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
	}

}