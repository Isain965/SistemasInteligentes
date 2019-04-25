import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.CommandCenter;
import enumerate.Action;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;

/**
 * MCTSÃ£ï¿½Â§Ã¥Ë†Â©Ã§â€�Â¨Ã£ï¿½â„¢Ã£â€šâ€¹Node
 *
 * @author Taichi Miyazaki
 */
public class Node {

	/** UCTÃ£ï¿½Â®Ã¥Â®Å¸Ã¨Â¡Å’Ã¦â„¢â€šÃ©â€“â€œ */
	public static final int UCT_TIME = 165 * 100000;

	/** UCB1Ã£ï¿½Â®Ã¥Â®Å¡Ã¦â€¢Â°CÃ£ï¿½Â®Ã¥â‚¬Â¤ */
	public static final double UCB_C = 3;

	/** Ã¦Å½Â¢Ã§Â´Â¢Ã£ï¿½â„¢Ã£â€šâ€¹Ã¦Å“Â¨Ã£ï¿½Â®Ã¦Â·Â±Ã£ï¿½â€¢ */
	public static final int UCT_TREE_DEPTH = 2;

	/**
	 * Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£â€šâ€™Ã§â€�Å¸Ã¦Ë†ï¿½Ã£ï¿½â„¢Ã£â€šâ€¹Ã©â€“Â¾Ã¥â‚¬Â¤
	 */
	public static final int UCT_CREATE_NODE_THRESHOULD = 10;

	/**
	 * Ã£â€šÂ·Ã£Æ’Å¸Ã£Æ’Â¥Ã£Æ’Â¬Ã£Æ’Â¼Ã£â€šÂ·Ã£Æ’Â§Ã£Æ’Â³Ã£â€šâ€™Ã¨Â¡Å’Ã£ï¿½â€ Ã
	 * ¦â„¢â€šÃ©â€“â€œ
	 */
	public int simulate_time;

	/**
	 * Ã¤Â¹Â±Ã¦â€¢Â°Ã£â€šâ€™Ã¥Ë†Â©Ã§â€�Â¨Ã£ï¿½â„¢Ã£â€šâ€¹Ã£ï¿½Â¨Ã£ï¿½ï¿½Ã£ï¿½Â«
	 * Ã¤Â½Â¿Ã£ï¿½â€ 
	 */
	private Random rnd;

	/** Ã¨Â¦ÂªÃ£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€° */
	private Node parent;

	/** Ã¥Â­ï¿½Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€° */
	private Node[] children;

	/** Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£ï¿½Â®Ã¦Â·Â±Ã£ï¿½â€¢ */
	private int depth;

	/**
	 * Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£ï¿½Å’Ã¦Å½Â¢Ã§Â´Â¢Ã£ï¿½â€¢Ã£â€šÅ’Ã£ï¿½Å¸Ã¥â€ºÅ¾Ã¦
	 * â€¢Â°
	 */
	private int games;

	/** UCB1Ã¥â‚¬Â¤ */
	private double ucb;

	/** Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤ */
	private double score = 0;

	/**
	 * Ã©ï¿½Â¸Ã¦Å Å¾Ã£ï¿½Â§Ã£ï¿½ï¿½Ã£â€šâ€¹Ã¨â€¡ÂªÃ¥Ë†â€ Ã£ï¿½Â®Ã¥â€¦Â¨Action
	 */
	private LinkedList<Action> myActions;

	/**
	 * Ã©ï¿½Â¸Ã¦Å Å¾Ã£ï¿½Â§Ã£ï¿½ï¿½Ã£â€šâ€¹Ã§â€ºÂ¸Ã¦â€°â€¹Ã£ï¿½Â®Ã¥â€¦Â¨Action
	 */
	private LinkedList<Action> oppActions;

	/**
	 * Ã£â€šÂ·Ã£Æ’Å¸Ã£Æ’Â¥Ã£Æ’Â¬Ã£Æ’Â¼Ã£â€šÂ·Ã£Æ’Â§Ã£Æ’Â³Ã£ï¿½â„¢Ã£â€šâ€¹Ã£ï¿½Â¨
	 * Ã£ï¿½ï¿½Ã£ï¿½Â«Ã¥Ë†Â©Ã§â€�Â¨Ã£ï¿½â„¢Ã£â€šâ€¹
	 */
	private Simulator simulator;

	/**
	 * Ã¦Å½Â¢Ã§Â´Â¢Ã¦â„¢â€šÃ£ï¿½Â«Ã©ï¿½Â¸Ã£â€šâ€œÃ£ï¿½Â Ã¨â€¡ÂªÃ¥Ë†â€ Ã£ï¿½Â®
	 * Action
	 */
	private LinkedList<Action> selectedMyActions;

	/**
	 * Ã£â€šÂ·Ã£Æ’Å¸Ã£Æ’Â¥Ã£Æ’Â¬Ã£Æ’Â¼Ã£â€šÂ·Ã£Æ’Â§Ã£Æ’Â³Ã£ï¿½â„¢Ã£â€šâ€¹Ã¥â€°ï¿
	 * ½Ã£ï¿½Â®Ã¨â€¡ÂªÃ¥Ë†â€ Ã£ï¿½Â®HP
	 */
	private int myOriginalHp;

	/**
	 * Ã£â€šÂ·Ã£Æ’Å¸Ã£Æ’Â¥Ã£Æ’Â¬Ã£Æ’Â¼Ã£â€šÂ·Ã£Æ’Â§Ã£Æ’Â³Ã£ï¿½â„¢Ã£â€šâ€¹Ã¥â€°ï¿
	 * ½Ã£ï¿½Â®Ã§â€ºÂ¸Ã¦â€°â€¹Ã£ï¿½Â®HP
	 */
	private int oppOriginalHp;

	private FrameData frameData;
	private boolean playerNumber;
	private CommandCenter commandCenter;
	private GameData gameData;

	private boolean isCreateNode;

	Deque<Action> mAction;
	Deque<Action> oppAction;

	public Node(FrameData frameData, Node parent, LinkedList<Action> myActions, LinkedList<Action> oppActions,
			GameData gameData, boolean playerNumber, CommandCenter commandCenter, int sim_time,
			LinkedList<Action> selectedMyActions) {
		this(frameData, parent, myActions, oppActions, gameData, playerNumber, commandCenter, sim_time);

		this.selectedMyActions = selectedMyActions;
	}

	public Node(FrameData frameData, Node parent, LinkedList<Action> myActions, LinkedList<Action> oppActions,
			GameData gameData, boolean playerNumber, CommandCenter commandCenter, int sim_time) {
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

		/* Minhas Alteracoes */
		this.simulate_time = sim_time;

		if (this.parent != null) {
			this.depth = this.parent.depth + 1;
		} else {
			this.depth = 0;
		}
	}

	/**
	 * MCTSÃ£â€šâ€™Ã¨Â¡Å’Ã£ï¿½â€ 
	 *
	 * @return Ã¦Å“â‚¬Ã§Âµâ€šÃ§Å¡â€žÃ£ï¿½ÂªÃ£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£ï¿½Â®Ã¦Å½Â¢Ã§Â´
	 *         Â¢Ã¥â€ºÅ¾Ã¦â€¢Â°Ã£ï¿½Å’Ã¥Â¤Å¡Ã£ï¿½â€žAction
	 */
	public Action mcts() {
		// Ã¦â„¢â€šÃ©â€“â€œÃ£ï¿½Â®Ã©â„¢ï¿½Ã£â€šÅ Ã£â‚¬ï¿½UCTÃ£â€šâ€™Ã§Â¹Â°Ã£â€šÅ Ã¨Â¿â€�Ã£ï¿½â„¢
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= UCT_TIME;) {
			uct();
		}

		return getBestVisitAction();
	}

	/**
	 * Ã£Æ’â€”Ã£Æ’Â¬Ã£â€šÂ¤Ã£â€šÂ¢Ã£â€šÂ¦Ã£Æ’Ë†(Ã£â€šÂ·Ã£Æ’Å¸Ã£Æ’Â¥Ã£Æ’Â¬Ã£Æ’Â¼
	 * Ã£â€šÂ·Ã£Æ’Â§Ã£Æ’Â³)Ã£â€šâ€™Ã¨Â¡Å’Ã£ï¿½â€ 
	 *
	 * @return Ã£Æ’â€”Ã£Æ’Â¬Ã£â€šÂ¤Ã£â€šÂ¢Ã£â€šÂ¦Ã£Æ’Ë†Ã§Âµï¿½Ã¦Å¾Å“Ã£ï¿½Â®Ã¨Â©
	 *         â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤
	 */
	public double playout() {

		mAction.clear();
		oppAction.clear();

		for (int i = 0; i < selectedMyActions.size(); i++) {
			mAction.add(selectedMyActions.get(i));
		}
		// System.out.println(selectedMyActions.size());
		for (int i = 0; i < 5 - selectedMyActions.size(); i++) {
			mAction.add(myActions.get(rnd.nextInt(myActions.size())));
		}
		// System.out.println("X:
		// "+frameData.getOpponentCharacter(playerNumber).x+", Y:
		// "+frameData.getOpponentCharacter(playerNumber).y);

		for (int i = 0; i < 5; i++) {
			oppAction.add(oppActions.get(rnd.nextInt(oppActions.size())));
		}
		FrameData nFrameData = simulator.simulate(frameData, playerNumber, mAction, oppAction, simulate_time); // Ã£â€šÂ·Ã£Æ’Å¸Ã£Æ’Â¥Ã£Æ’Â¬Ã£Æ’Â¼Ã£â€šÂ·Ã£Æ’Â§Ã£Æ’Â³Ã£â€šâ€™Ã¥Â®Å¸Ã¨Â¡Å’
		// int s = getScore(nFrameData);
		// System.out.println(s);
		// return s;
		return getScore(nFrameData);
	}

	/**
	 * UCTÃ£â€šâ€™Ã¨Â¡Å’Ã£ï¿½â€  <br>
	 *
	 * @return Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤
	 */
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

	/**
	 * Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£â€šâ€™Ã§â€�Å¸Ã¦Ë†ï¿½Ã£ï¿½â„¢Ã£â€šâ€¹
	 */
	public void createNode() {

		this.children = new Node[myActions.size()];

		for (int i = 0; i < children.length; i++) {

			LinkedList<Action> my = new LinkedList<Action>();
			for (Action act : selectedMyActions) {
				my.add(act);
			}

			my.add(myActions.get(i));

			children[i] = new Node(frameData, this, myActions, oppActions, gameData, playerNumber, commandCenter,
					simulate_time, my);
		}
	}

	/**
	 * Ã¦Å“â‚¬Ã¥Â¤Å¡Ã¨Â¨ÂªÃ¥â€¢ï¿½Ã¥â€ºÅ¾Ã¦â€¢Â°Ã£ï¿½Â®Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£ï¿½Â
	 * ®ActionÃ£â€šâ€™Ã¨Â¿â€�Ã£ï¿½â„¢
	 *
	 * @return Ã¦Å“â‚¬Ã¥Â¤Å¡Ã¨Â¨ÂªÃ¥â€¢ï¿½Ã¥â€ºÅ¾Ã¦â€¢Â°Ã£ï¿½Â®Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’
	 *         â€°Ã£ï¿½Â®Action
	 */
	public Action getBestVisitAction() {

		int selected = -1;
		double bestGames = -9999;
		boolean neutral_cond = true; // se nao tiver score algum pra todas as
										// acoes, usar a neutra, para considerar
										// mais rapidamente a prox
		// System.out.println("########################### CHILDREN SCORE
		// #######################################");
		for (int i = 0; i < children.length; i++) {
			// System.out.println(children[i].score);
			if (children[i].score != 0) {
				neutral_cond = false;
			}

			if (bestGames < children[i].games) {
				bestGames = children[i].games;
				selected = i;
			}
		}

		if (neutral_cond) {

			return Action.NEUTRAL;
		}

		// System.out.println(neutral_cond);
		return this.myActions.get(selected);
	}

	/**
	 * Ã¦Å“â‚¬Ã¥Â¤Å¡Ã£â€šÂ¹Ã£â€šÂ³Ã£â€šÂ¢Ã£ï¿½Â®Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£ï¿½Â®
	 * ActionÃ£â€šâ€™Ã¨Â¿â€�Ã£ï¿½â„¢
	 *
	 * @return Ã¦Å“â‚¬Ã¥Â¤Å¡Ã£â€šÂ¹Ã£â€šÂ³Ã£â€šÂ¢Ã£ï¿½Â®Ã£Æ’Å½Ã£Æ’Â¼Ã£Æ’â€°Ã£ï¿½
	 *         Â®Action
	 */
	public Action getBestScoreAction() {

		int selected = -1;
		double bestScore = -9999;

		for (int i = 0; i < children.length; i++) {

			System.out.println(
					"Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤:" + children[i].score / children[i].games + ",Ã¨Â©Â¦Ã¨Â¡Å’Ã¥â€ºÅ¾Ã¦â€¢Â°:"
							+ children[i].games + ",ucb:" + children[i].ucb + ",Action:" + myActions.get(i));

			double meanScore = children[i].score / children[i].games;
			if (bestScore < meanScore) {
				bestScore = meanScore;
				selected = i;
			}
		}

		System.out.println(myActions.get(selected) + ",Ã¥â€¦Â¨Ã¨Â©Â¦Ã¨Â¡Å’Ã¥â€ºÅ¾Ã¦â€¢Â°:" + games);
		System.out.println("");

		return this.myActions.get(selected);
	}

	/**
	 * Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤Ã£â€šâ€™Ã¨Â¿â€�Ã£ï¿½â„¢
	 *
	 * @param fd
	 *            Ã£Æ’â€¢Ã£Æ’Â¬Ã£Æ’Â¼Ã£Æ’Â Ã£Æ’â€¡Ã£Æ’Â¼Ã£â€šÂ¿(Ã£ï¿½â€œÃ£â€šÅ’
	 *            Ã£ï¿½Â«hpÃ£ï¿½Â¨Ã£ï¿½â€¹Ã£ï¿½Â®Ã¦Æ’â€¦Ã¥Â Â±Ã£ï¿½Å’Ã¥â€¦Â¥Ã£ï¿
	 *            ½Â£Ã£ï¿½Â¦Ã£ï¿½â€žÃ£â€šâ€¹)
	 * @return Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤
	 */
	public int getScore(FrameData fd) {
		if (playerNumber) {
			return (fd.getCharacter(true).getHp() - myOriginalHp) - (fd.getCharacter(false).getHp() - oppOriginalHp);
		} else {
			return (fd.getCharacter(false).getHp() - myOriginalHp) - (fd.getCharacter(true).getHp() - oppOriginalHp);
		}
	}

	/**
	 * Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤Ã£ï¿½Â¨Ã¥â€¦Â¨Ã£Æ’â€”Ã£Æ’Â¬Ã£â€šÂ¤Ã£â€šÂ¢Ã£â€šÂ¦Ã£Æ’Ë
	 * †Ã¨Â©Â¦Ã¨Â¡Å’Ã¥â€ºÅ¾Ã¦â€¢Â°Ã£ï¿½Â¨Ã£ï¿½ï¿½Ã£ï¿½Â®ActionÃ£ï¿½Â®Ã£Æ’â€”Ã£Æ’
	 * Â¬Ã£â€šÂ¤Ã£â€šÂ¢Ã£â€šÂ¦Ã£Æ’Ë†Ã¨Â©Â¦Ã¨Â¡Å’Ã¥â€ºÅ¾Ã¦â€¢Â°Ã£ï¿½â€¹Ã£â€šâ€°
	 * UCB1Ã¥â‚¬Â¤Ã£â€šâ€™Ã¨Â¿â€�Ã£ï¿½â„¢
	 *
	 * @param score
	 *            Ã¨Â©â€¢Ã¤Â¾Â¡Ã¥â‚¬Â¤
	 * @param n
	 *            Ã¥â€¦Â¨Ã£Æ’â€”Ã£Æ’Â¬Ã£â€šÂ¤Ã£â€šÂ¢Ã£â€šÂ¦Ã£Æ’Ë†Ã¨Â©Â¦Ã¨Â¡Å’
	 *            Ã¥â€ºÅ¾Ã¦â€¢Â°
	 * @param ni
	 *            Ã£ï¿½ï¿½Ã£ï¿½Â®ActionÃ£ï¿½Â®Ã£Æ’â€”Ã£Æ’Â¬Ã£â€šÂ¤Ã£â€šÂ¢Ã£â€šÂ¦
	 *            Ã£Æ’Ë†Ã¨Â©Â¦Ã¨Â¡Å’Ã¥â€ºÅ¾Ã¦â€¢Â°
	 * @return UCB1Ã¥â‚¬Â¤
	 */
	public double getUcb(double score, int n, int ni) {
		return score + UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
	}

	public void printNode(Node node) {
		System.out.println("Ã¥â€¦Â¨Ã¨Â©Â¦Ã¨Â¡Å’Ã¥â€ºÅ¾Ã¦â€¢Â°:" + node.games);
		for (int i = 0; i < node.children.length; i++) {
			System.out.println(i + ",Ã¥â€ºÅ¾Ã¦â€¢Â°:" + node.children[i].games + ",Ã¦Â·Â±Ã£ï¿½â€¢:"
					+ node.children[i].depth + ",score:" + node.children[i].score / node.children[i].games + ",ucb:"
					+ node.children[i].ucb);
		}
		System.out.println("");
		for (int i = 0; i < node.children.length; i++) {
			if (node.children[i].isCreateNode) {
				printNode(node.children[i]);
			}
		}
	}
}
