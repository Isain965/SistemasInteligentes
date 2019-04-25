package jaybot;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.CommandCenter;
import enumerate.Action;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;

public class Node {

	public static final int UCT_TIME = 165 * 100000;

	public static final double UCB_C = 3;

	public static final int UCT_TREE_DEPTH = 2;

	public static final int UCT_CREATE_NODE_THRESHOULD = 10;

	public static final int SIMULATION_TIME = 60;

	private Random rnd;

	private Node parent;

	private Node[] children;

	private int depth;

	private int games;

	private double ucb;

	private double score;

	private LinkedList<Action> myActions;

	private LinkedList<Action> oppActions;

	private Simulator simulator;

	private LinkedList<Action> selectedMyActions;

	private int myOriginalHp;

	private int oppOriginalHp;

	private FrameData frameData;
	private boolean playerNumber;
	private CommandCenter commandCenter;
	private GameData gameData;

	private boolean isCreateNode;

	Deque<Action> mAction;
	Deque<Action> oppAction;

	public Node(FrameData frameData, Node parent, LinkedList<Action> myActions, LinkedList<Action> oppActions,
			GameData gameData, boolean playerNumber, CommandCenter commandCenter,
			LinkedList<Action> selectedMyActions) {
		this(frameData, parent, myActions, oppActions, gameData, playerNumber, commandCenter);

		this.selectedMyActions = selectedMyActions;
	}

	public Node(FrameData frameData, Node parent, LinkedList<Action> myActions, LinkedList<Action> oppActions,
			GameData gameData, boolean playerNumber, CommandCenter commandCenter) {
		this.frameData = frameData;
		this.parent = parent;
		this.myActions = myActions;
		this.oppActions = oppActions;
		this.gameData = gameData;
		this.simulator = new Simulator(gameData);
		this.playerNumber = playerNumber;
		this.commandCenter = commandCenter;

		this.selectedMyActions = new LinkedList<Action>();

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

	public Action mcts() {
		// 時間�?��?り�?UCTを繰り返�?�
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= UCT_TIME;) {
			uct();
		}

		return getBestVisitAction();
	}

	public double playout() {

		mAction.clear();
		oppAction.clear();

		for (int i = 0; i < selectedMyActions.size(); i++) {
			mAction.add(selectedMyActions.get(i));
		}

		for (int i = 0; i < 5 - selectedMyActions.size(); i++) {
			mAction.add(myActions.get(rnd.nextInt(myActions.size())));
		}
		for (int i = 0; i < 5; i++) {
			oppAction.add(oppActions.get(rnd.nextInt(oppActions.size())));
		}

		FrameData nFrameData = simulator.simulate(frameData, playerNumber, mAction, oppAction, SIMULATION_TIME); // シミュレーションを実行

		return getScore(nFrameData);
	}

	public double uct() {

		Node selectedNode = null;
		double bestUcb;

		bestUcb = -99999;

		for (Node child : this.children) {
			if (child.games == 0) {
				child.ucb = 9999 + rnd.nextInt(50);
			} else {
				child.ucb = getUcb(child.score / child.games, games, child.games);
			}

			if (bestUcb < child.ucb) {
				selectedNode = child;
				bestUcb = child.ucb;
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

		this.children = new Node[myActions.size()];

		for (int i = 0; i < children.length; i++) {

			LinkedList<Action> my = new LinkedList<Action>();
			for (Action act : selectedMyActions) {
				my.add(act);
			}

			my.add(myActions.get(i));

			children[i] = new Node(frameData, this, myActions, oppActions, gameData, playerNumber, commandCenter, my);
		}
	}


	public Action getBestVisitAction() {

		int selected = -1;
		double bestGames = -9999;

		for (int i = 0; i < children.length; i++) {

			if (JayBot_2017.DEBUG_MODE) {
				System.out.println("評価値:" + children[i].score / children[i].games + ",試行回数:" + children[i].games
						+ ",ucb:" + children[i].ucb + ",Action:" + myActions.get(i));
			}

			if (bestGames < children[i].games) {
				bestGames = children[i].games;
				selected = i;
			}
		}

		if (JayBot_2017.DEBUG_MODE) {
			System.out.println(myActions.get(selected) + ",全試行回数:" + games);
			System.out.println("");
		}

		return this.myActions.get(selected);
	}


	public Action getBestScoreAction() {

		int selected = -1;
		double bestScore = -9999;

		for (int i = 0; i < children.length; i++) {

			System.out.println("評価値:" + children[i].score / children[i].games + ",試行回数:" + children[i].games + ",ucb:"
					+ children[i].ucb + ",Action:" + myActions.get(i));

			double meanScore = children[i].score / children[i].games;
			if (bestScore < meanScore) {
				bestScore = meanScore;
				selected = i;
			}
		}

		System.out.println(myActions.get(selected) + ",全試行回数:" + games);
		System.out.println("");

		return this.myActions.get(selected);
	}

	public int getScore(FrameData fd) {
		if (playerNumber) {
			return (fd.getCharacter(true).getHp() - myOriginalHp) - (fd.getCharacter(false).getHp() - oppOriginalHp);
		} else {
			return (fd.getCharacter(false).getHp() - myOriginalHp) - (fd.getCharacter(true).getHp() - oppOriginalHp);
		}
	}

	public double getUcb(double score, int n, int ni) {
		return score + UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
	}

	public void printNode(Node node) {
		System.out.println("全試行回数:" + node.games);
		for (int i = 0; i < node.children.length; i++) {
			System.out.println(i + ",回数:" + node.children[i].games + ",深�?�:" + node.children[i].depth + ",score:"
					+ node.children[i].score / node.children[i].games + ",ucb:" + node.children[i].ucb);
		}
		System.out.println("");
		for (int i = 0; i < node.children.length; i++) {
			if (node.children[i].isCreateNode) {
				printNode(node.children[i]);
			}
		}
	}
}
