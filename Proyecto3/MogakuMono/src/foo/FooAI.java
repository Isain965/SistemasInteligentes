package foo;

import java.util.ArrayList;
import java.util.LinkedList;

import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.MotionData;

public class FooAI  {

	private Simulator simulator;
	private Key key;
	private CommandCenter commandCenter;
	private boolean playerNumber;
	private GameData gameData;

	private FrameData frameData;

	private FrameData simulatorAheadFrameData;

	private LinkedList<Action> myActions;

	private LinkedList<Action> oppActions;

	private CharacterData myCharacter;

	private CharacterData oppCharacter;

	private static final int FRAME_AHEAD = 14;

	private ArrayList<MotionData> myMotion;

	private ArrayList<MotionData> oppMotion;

	private Action[] actionAir;

	private Action[] actionGround;

	private Action spSkill;

	private ExtendedNode rootNode;

	public static final boolean DEBUG_MODE = false;

	private CharacterName charName;

	public void close() {
	}

	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, playerNumber);

		myCharacter = frameData.getCharacter(playerNumber);
		oppCharacter = frameData.getCharacter(!playerNumber);
	}

	public int initialize(GameData gameData, boolean playerNumber,
			FrameData frameData, CommandCenter commandCenter) {

		this.playerNumber = playerNumber;
		this.gameData = gameData;

		this.key = new Key();
		this.frameData =frameData;
		this.commandCenter = commandCenter;

		this.myActions = new LinkedList<Action>();
		this.oppActions = new LinkedList<Action>();

		simulator = gameData.getSimulator();
		System.out.println("thunder:" + gameData.getCharacterName(playerNumber));
		actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA, Action.AIR_DB,
				Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA, Action.AIR_D_DF_FB,
				Action.AIR_F_D_DFA, Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
		actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
		spSkill = Action.STAND_D_DF_FC;

		myMotion = gameData.getMotionData(playerNumber);
		oppMotion = gameData.getMotionData(!playerNumber);

		String tmpcharname = this.gameData.getCharacterName(this.playerNumber);
		if (tmpcharname.equals("ZEN"))
			charName = CharacterName.ZEN;
		else if (tmpcharname.equals("LUD"))
			charName = CharacterName.LUD;
		else
			charName = CharacterName.OTHER;

		return 0;
	}

	public Key input() {
		return key;
	}

	private void mctsProcessing() {
		rootNode = new ExtendedNode(charName, myCharacter.getEnergy(), oppCharacter.getEnergy(),
				simulatorAheadFrameData, null, myActions, oppActions, gameData, playerNumber, commandCenter);
		rootNode.createNode();

		Action bestAction = rootNode.mcts();

		commandCenter.commandCall(bestAction.name());
	}

	private void zenProcessing() {
		FrameData tmpFrameData = simulator.simulate(frameData, this.playerNumber, null, null, 17);
		CommandCenter cc = this.commandCenter;
		cc.setFrameData(tmpFrameData, playerNumber);
		int distance = frameData.getDistanceX();
		int energy = frameData.getCharacter(playerNumber).getEnergy();
		CharacterData my = frameData.getCharacter(playerNumber);
		CharacterData opp = frameData.getCharacter(!playerNumber);
		int myX = (my.getLeft() + my.getRight()) / 2;
		int oppX = (opp.getLeft() + opp.getRight()) / 2;
		int xDifference = myX - oppX;

		if ((opp.getEnergy() >= 300) && ((my.getHp() - opp.getHp()) <= 300))
			cc.commandCall("FOR_JUMP _B B B");
		// if the opp has 300 of energy, it is dangerous, so better jump!!
		// if the health difference is high we are dominating so we are fearless
		// :)
		else if (!my.getState().equals(State.AIR) && !my.getState().equals(State.DOWN)) { // if
																							// not
																							// in
																							// air
			if ((distance > 150)) {
				cc.commandCall("FOR_JUMP"); // If its too far, then jump to get
											// closer fast
			} else if (energy >= 300)
				cc.commandCall("STAND_D_DF_FC"); // High energy projectile
			else if ((distance > 100) && (energy >= 50))
				cc.commandCall("STAND_D_DB_BB"); // Perform a slide kick
			else if (opp.getState().equals(State.AIR)) // if enemy on Air
				cc.commandCall("STAND_F_D_DFA"); // Perform a big punch
			else
				cc.commandCall("B"); // Perform a kick in all other cases,
										// introduces randomness
		} else if ((distance <= 150) && (my.getState().equals(State.AIR) || my.getState().equals(State.DOWN))
				&& (((gameData.getStageWidth() - myX) >= 200) || (xDifference > 0))
				&& ((myX >= 200) || xDifference < 0)) { // Conditions to handle
														// game corners
			if (energy >= 5)
				cc.commandCall("AIR_DB"); // Perform air down kick when in air
			else
				cc.commandCall("B"); // Perform a kick in all other cases,
										// introduces randomness
		} else
			cc.commandCall("B"); // Perform a kick in all other cases,
									// introduces randomness
	}

	private boolean printnameflag = true;

	public void processing() {

		if (canProcessing()) {
			if (commandCenter.getSkillFlag()) {
				key = commandCenter.getSkillKey();
			} else {
				key.empty();
				commandCenter.skillCancel();

				mctsPrepare();

				if (charName == CharacterName.ZEN) {
					zenProcessing();
					if (printnameflag)
						System.out.println("zenProcessing");
				}else {
					if (printnameflag)
						System.out.println("elseProcessing");
					mctsProcessing();
				}
				printnameflag = false;
			}
		}
	}

	public boolean canProcessing() {
		return !frameData.getEmptyFlag() && frameData.getRemainingTimeMilliseconds() > 0;
	}

	public void mctsPrepare() {
		simulatorAheadFrameData = simulator.simulate(frameData, playerNumber, null, null, FRAME_AHEAD);

		myCharacter = simulatorAheadFrameData.getCharacter(playerNumber);
		oppCharacter = simulatorAheadFrameData.getCharacter(!playerNumber);

		setMyAction();
		setOppAction();
	}

	public void setMyAction() {
		myActions.clear();

		int energy = myCharacter.getEnergy();

		if (myCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(myMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionAir[i]);
				}
			}
		} else {
			if (Math.abs(myMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
				myActions.add(spSkill);
			}

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(myMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionGround[i]);
				}
			}
		}

	}

	public void setOppAction() {
		oppActions.clear();

		int energy = oppCharacter.getEnergy();

		if (oppCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(oppMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionAir[i]);
				}
			}
		} else {
			if (Math.abs(oppMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
				oppActions.add(spSkill);
			}

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(oppMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionGround[i]);
				}
			}
		}
	}

	public void roundEnd(int arg0, int arg1, int arg2) {
		key.empty();
		commandCenter.skillCancel();
	}

}
