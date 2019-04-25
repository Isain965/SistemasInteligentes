package foo;

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

	/** UCT execution time */
	public static final int UCT_TIME = 165 * 100000;

	/** Value of C in UCB1 */
	public static final double UCB_C = 3;

	/** Depth of tree search */
	public static final int UCT_TREE_DEPTH = 2;

	/** Threshold for generating a node */
	public static final int UCT_CREATE_NODE_THRESHOULD = 10;

	/** Time for performing simulation */
	public static final int SIMULATION_TIME = 60;

	/** Use when in need of random numbers */
	private Random rnd;

	/** Parent node */
	private Node parent;

	/** Child node */
	private Node[] children;

	/** Node depth */
	private int depth;

	/** Number of node visiting times */
	private int games;

	/** UCB1 Value */
	private double ucb;

	/** Evaluation value */
	private double score;

	/** All selectable actions of self AI */
	private LinkedList<Action> myActions;

	/** All selectable actions of the opponent */
	private LinkedList<Action> oppActions;

	/** Use in simulation */
	private Simulator simulator;

	/** Selected action by self AI during search */
	private LinkedList<Action> selectedMyActions;

	/** Self HP before simulation */
	private int myOriginalHp;

	/** Opponent HP before simulation */
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
		// Repeat UCT as many times as possible
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

		FrameData nFrameData = simulator.simulate(frameData, playerNumber, mAction, oppAction, SIMULATION_TIME); // Perform
																													// simulation

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

			if (FooAI.DEBUG_MODE) {
				System.out.println("Evaluation value:" + children[i].score / children[i].games + ",Number of trials:"
						+ children[i].games + ",ucb:" + children[i].ucb + ",Action:" + myActions.get(i));
			}

			if (bestGames < children[i].games) {
				bestGames = children[i].games;
				selected = i;
			}
		}

		if (FooAI.DEBUG_MODE) {
			System.out.println(myActions.get(selected) + ",Total number of trails:" + games);
			System.out.println("");
		}

		return this.myActions.get(selected);
	}

	public Action getBestScoreAction() {

		int selected = -1;
		double bestScore = -9999;

		for (int i = 0; i < children.length; i++) {

			System.out.println("Evaluation value:" + children[i].score / children[i].games + ",Number of trials:"
					+ children[i].games + ",ucb:" + children[i].ucb + ",Action:" + myActions.get(i));

			double meanScore = children[i].score / children[i].games;
			if (bestScore < meanScore) {
				bestScore = meanScore;
				selected = i;
			}
		}

		System.out.println(myActions.get(selected) + ",Total number of trails:" + games);
		System.out.println("");

		return this.myActions.get(selected);
	}

	public int getScore(FrameData fd) {
		return (fd.getCharacter(playerNumber).getHp() - myOriginalHp)
				- (fd.getCharacter(!playerNumber).getHp() - oppOriginalHp);
	}

	public double getUcb(double score, int n, int ni) {
		return score + UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
	}

	public void printNode(Node node) {
		System.out.println("Total number of trails:" + node.games);
		for (int i = 0; i < node.children.length; i++) {
			System.out.println(i + ",Trails:" + node.children[i].games + ",Depth:" + node.children[i].depth + ",score:"
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