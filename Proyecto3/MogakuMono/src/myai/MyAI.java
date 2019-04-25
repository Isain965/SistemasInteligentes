package myai;

import java.util.ArrayList;
/**
 * å…¨ã‚­ãƒ£ãƒ©ã�®ãƒ™ãƒ¼ã‚¹ã�¨ã�ªã‚‹AI
 * ã�“ã‚Œã‚’ã‚ªãƒ¼ãƒ�ãƒ¼ãƒ©ã‚¤ãƒ‰ã�—ã�¦å�„ã‚­ãƒ£ãƒ©ã�®ç‰¹å¾´ã��?ã�¨ã�«è¡Œå‹•ã‚’å¤‰ã�ˆã‚‹
 * MidTermã�¯ã�»ã�¼ã�“ã�®AIã�®çŠ¶æ…‹ã�§æ��å‡ºã�—ã�Ÿ
 * LudSpeedAIã�¯ã�“ã�®ã�¾ã�¾ã�„ã��
 * @author Eita Aoki
 */
import java.util.LinkedList;

import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import myutil.Calclator;
import myutil.My;
import myutil.Node;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.MotionData;

public class MyAI {

	boolean player;
	FrameData frameData;
	CommandCenter cc;
	Simulator simulator;
	GameData gd;

	int distance;
	CharacterData opp;
	CharacterData my;
	ArrayList<MotionData> myMotion;

	ArrayList<MotionData> oppMotion;

	LinkedList<Action> myActionsEnoughEnergy;

	LinkedList<Action> oppActionsEnoughEnergy;

	boolean firstflag = true;

	Action[] actionGround;
	Action[] actionAir;

	FrameData simFrameData;
	Calclator calc;

	Action[] myacs;
	Action[] opacs;

	double bestscore;
	Action bestac;

	Action preAct;

	LinkedList<Action> HittingMyActions = new LinkedList<Action>();

	public MyAI(GameData arg0, boolean player) {
		this.player = player;
		frameData = new FrameData();
		cc = new CommandCenter();
		gd = arg0;
		simulator = gd.getSimulator();
		myMotion = gd.getMotionData(player);
		oppMotion = gd.getMotionData(!player);

		this.myActionsEnoughEnergy = new LinkedList<Action>();
		this.oppActionsEnoughEnergy = new LinkedList<Action>();
		this.preAct = null;
		setRootActions();

	}

	void setRootActions() {
		this.actionGround = My.actionGround;
		this.actionAir = My.actionAir;

	}

	void setCalclator() {
		calc = new Calclator(simFrameData, gd, player, Calclator.NONACT);
	}

	void setHittingMyAction() {

		bestscore = -9999;
		bestac = null;
		HittingMyActions.clear();
		for (Action ac : myacs) {
			double hpscore = calc.getHpScore(ac);
			MotionData mo = myMotion.get(ac.ordinal());

			if (hpscore > 0) {
				HittingMyActions.add(ac);
				double turnscore = (double) hpscore + (mo.isAttackDownProp() ? 30.0 : 0.0)
						- ((double) mo.attackStartUp) * 0.01 - ((double) mo.cancelAbleFrame) * 0.0001;
				if (turnscore > bestscore) {
					bestscore = turnscore;
					bestac = ac;
				}
			}
		}
	}

	long start;

	public void getInformation(FrameData frameData) {
		start = System.nanoTime();
		this.frameData = frameData;

		if (frameData.getFramesNumber() >= 0) {
			if (frameData.getFramesNumber() < 14) {
				simFrameData = new FrameData(frameData);
			} else {
				simFrameData = simulator.simulate(frameData, this.player, null, null, 14);
			}
			cc.setFrameData(simFrameData, player);
			distance = simFrameData.getDistanceX();
			my = simFrameData.getCharacter(player);
			opp = simFrameData.getCharacter(player);

			setCalclator();

			if (my.getState() != State.AIR) {
				myacs = this.actionGround;

			} else {
				myacs = this.actionAir;
			}
			if (opp.getState() != State.AIR) {
				opacs = My.actionGround;
			} else {
				opacs = My.actionAir;
			}
			this.myActionsEnoughEnergy = calc.getEnoughEnergyActions(true, myacs);
			this.oppActionsEnoughEnergy = calc.getEnoughEnergyActions(false, opacs);

			setHittingMyAction();

		}
	}

	Node getRootNode(FrameData simFrameData) {
		return new Node(165 * 100000 - (System.nanoTime() - start), simFrameData, null, myActionsEnoughEnergy,
				oppActionsEnoughEnergy, gd, player, cc);
	}

	Action mctsprocessing(FrameData simFrameData, LinkedList<Action> mystartActions) {

		Node rootNode = getRootNode(simFrameData);

		if (mystartActions.isEmpty()) {
			rootNode.createNode();
		} else
			rootNode.createNode(mystartActions);

		Action bestAction = rootNode.mcts();

		return (bestAction);
	}

	public Action getDoAction() {
		Action act = getPrepareAction();
		this.preAct = act;
		return act;
	}

	Action getPrepareAction() {

		if (HittingMyActions.size() > 0){
			System.out.println("in !");
			return this.mctsprocessing(simFrameData, HittingMyActions);
		}		else {

			LinkedList<Action> moveActs = (distance < 300)
					? calc.getEnoughEnergyActions(true, Action.FOR_JUMP, Action.FORWARD_WALK, Action.JUMP,
							Action.BACK_JUMP, Action.BACK_STEP)
					: calc.getEnoughEnergyActions(true, Action.FORWARD_WALK, Action.FOR_JUMP, Action.JUMP,
							Action.BACK_JUMP, Action.BACK_STEP);

			return (calc.getMinMaxIfHadouken(moveActs));

		}

	}

}
