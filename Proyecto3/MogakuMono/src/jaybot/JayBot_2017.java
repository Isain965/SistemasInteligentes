package jaybot;
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

/**
 * BASIC MCTS AI
 *
 * @author Taichi
 *
 */

/**
 * JayBot_2017 ( Using MCTS & Robustic Action-Table AI)
 *
 * @author Man-Je Kim
 *
 */
public class JayBot_2017 implements AIInterface {

	private Simulator simulator;
	private Key key;
	private CommandCenter commandCenter;
	private boolean playerNumber;
	private GameData gameData;

	/** å¤§æœ¬ã?®FrameData */
	private FrameData frameData;

	/** å¤§æœ¬ã‚ˆã‚ŠFRAME_AHEADåˆ†é?…ã‚Œã?ŸFrameData */
	private FrameData simulatorAheadFrameData;

	/** è‡ªåˆ†ã?Œè¡Œã?ˆã‚‹è¡Œå‹•å…¨ã?¦ */
	private LinkedList<Action> myActions;

	/** ç›¸æ‰‹ã?Œè¡Œã?ˆã‚‹è¡Œå‹•å…¨ã?¦ */
	private LinkedList<Action> oppActions;

	/** è‡ªåˆ†ã?®æƒ…å ± */
	private CharacterData myCharacter;

	/** ç›¸æ‰‹ã?®æƒ…å ± */
	private CharacterData oppCharacter;

	/** ãƒ•ãƒ¬ãƒ¼ãƒ ã?®èª¿æ•´ç”¨æ™‚é–“(JerryMizunoAIã‚’å?‚è€ƒ) */
	private static final int FRAME_AHEAD = 14;

	private ArrayList<MotionData> myMotion;

	private ArrayList<MotionData> oppMotion;

	private Action[] actionAir;

	private Action[] actionGround;

	private Action spSkill;

	private Node rootNode;

	private CharacterName charName;

	/** ãƒ‡ãƒ?ãƒƒã‚°ãƒ¢ãƒ¼ãƒ‰ã?§ã?‚ã‚‹ã?‹ã?©ã?†ã?‹ã€‚trueã?®å ´å?ˆã€?æ§˜ã€…ã?ªãƒ­ã‚°ã?Œå‡ºåŠ›ã?•ã‚Œã‚‹ */
	public static final boolean DEBUG_MODE = false;

	@Override
	public void close() {

	}

	@Override
	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, playerNumber);

		myCharacter = frameData.getCharacter(playerNumber);
		oppCharacter = frameData.getCharacter(!playerNumber);
	}

	@Override
	public int initialize(GameData gameData, boolean playerNumber) {
		this.playerNumber = playerNumber;
		this.gameData = gameData;

		this.key = new Key();
		this.frameData = new FrameData();
		this.commandCenter = new CommandCenter();

		this.myActions = new LinkedList<Action>();
		this.oppActions = new LinkedList<Action>();

		simulator = gameData.getSimulator();
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

	@Override
	public Key input() {
		return key;
	}

	@Override
	public void processing() {

		if (canProcessing()) {
			if (commandCenter.getSkillFlag()) {
				key = commandCenter.getSkillKey();
			} else {
				key.empty();
				commandCenter.skillCancel();

				mctsPrepare(); // MCTSã?®ä¸‹æº–å‚™ã‚’è¡Œã?†
				rootNode = new Node(simulatorAheadFrameData, null, myActions, oppActions, gameData, playerNumber,
						commandCenter);
				rootNode.createNode();

				Action bestAction = rootNode.mcts(); // MCTSã?®å®Ÿè¡Œ
				if (JayBot_2017.DEBUG_MODE) {
					rootNode.printNode(rootNode);
				}

				commandCenter.commandCall(bestAction.name()); // MCTSã?§é?¸æŠžã?•ã‚Œã?Ÿè¡Œå‹•ã‚’å®Ÿè¡Œã?™ã‚‹
			}
		}
	}

	/**
	 * AIã?Œè¡Œå‹•ã?§ã??ã‚‹ã?‹ã?©ã?†ã?‹ã‚’åˆ¤åˆ¥ã?™ã‚‹
	 *
	 * @return AIã?Œè¡Œå‹•ã?§ã??ã‚‹ã?‹ã?©ã?†ã?‹
	 */
	public boolean canProcessing() {
		return !frameData.getEmptyFlag() && frameData.getRemainingTime() > 0;
	}

	/**
	 * MCTSã?®ä¸‹æº–å‚™ <br>
	 * 14ãƒ•ãƒ¬ãƒ¼ãƒ é€²ã?¾ã?›ã?ŸFrameDataã?®å?–å¾—ã?ªã?©ã‚’è¡Œã?†
	 */
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

		if (charName.name() == "ZEN") {
			if (myCharacter.getState() == State.AIR) {
				for (int i = 0; i < actionAir.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionAir[i]);
					}
				}
			} else if (simulatorAheadFrameData.getDistanceX() < 50) {

				myActions.add(Action.STAND_D_DF_FA);
				myActions.add(Action.STAND_B);
				myActions.add(Action.CROUCH_B);
				myActions.add(Action.STAND_D_DB_BA);
				if (myCharacter.getEnergy() >= 50)
					myActions.add(Action.STAND_D_DB_BB);
			}

			else if (simulatorAheadFrameData.getDistanceX() < 85) {

				myActions.add(Action.STAND_B);
				myActions.add(Action.AIR_B);
				myActions.add(Action.CROUCH_B);
				myActions.add(Action.AIR_DB);
				if (myCharacter.getEnergy() >= 50)
					myActions.add(Action.STAND_D_DB_BB);
			} else if (simulatorAheadFrameData.getDistanceX() < 100) {

				myActions.add(Action.STAND_B);
				myActions.add(Action.CROUCH_FB);
				myActions.add(Action.CROUCH_B);
				myActions.add(Action.STAND_FB);
				myActions.add(Action.AIR_DB);
				if (myCharacter.getEnergy() >= 50)
					myActions.add(Action.STAND_D_DB_BB);
			} else if (simulatorAheadFrameData.getDistanceX() < 105) {
				myActions.add(Action.CROUCH_FB);
				myActions.add(Action.CROUCH_B);
				myActions.add(Action.STAND_FB);
				myActions.add(Action.STAND_D_DF_FA);
				myActions.add(Action.AIR_DB);
				if (myCharacter.getEnergy() >= 30)
					myActions.add(Action.STAND_D_DF_FB);
				if (myCharacter.getEnergy() >= 50)
					myActions.add(Action.STAND_D_DB_BB);
			} else {
				if (Math.abs(
						myMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
					myActions.add(spSkill);
				}

				for (int i = 0; i < actionGround.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionGround[i]);
					}
				}
			}
		} else if (charName.name() == "LUD") {
			if (myCharacter.getState() == State.AIR) {
				for (int i = 0; i < actionAir.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionAir[i]);
					}
				}
			} else if (simulatorAheadFrameData.getDistanceX() < 50) {
				myActions.add(Action.AIR_DB);
				myActions.add(Action.AIR_B);
				myActions.add(Action.CROUCH_A);
				myActions.add(Action.CROUCH_B);
				myActions.add(Action.AIR_A);
				myActions.add(Action.CROUCH_FA);
				myActions.add(Action.STAND_B);
				if (myCharacter.getEnergy() >= 1)
					myActions.add(Action.STAND_A);
				if (myCharacter.getEnergy() >= 8)
					myActions.add(Action.STAND_FA);
				if (myCharacter.getEnergy() >= 10)
					myActions.add(Action.THROW_A);
				if (myCharacter.getEnergy() >= 20)
					myActions.add(Action.STAND_D_DB_BB);

			}

			else if (simulatorAheadFrameData.getDistanceX() < 85) {

				myActions.add(Action.STAND_B);
				myActions.add(Action.CROUCH_B);
				myActions.add(Action.AIR_B);
				myActions.add(Action.CROUCH_A);
				myActions.add(Action.CROUCH_FA);
				if (myCharacter.getEnergy() >= 30)
					myActions.add(Action.STAND_D_DB_BA);

				if (myCharacter.getEnergy() >= 30)
					myActions.add(Action.STAND_D_DF_FA);
				if (myCharacter.getEnergy() >= 20)
					myActions.add(Action.STAND_D_DF_FB);
				if (myCharacter.getEnergy() >= 10)
					myActions.add(Action.AIR_UB);
				if (myCharacter.getEnergy() >= 20)
					myActions.add(Action.STAND_FB);
				if (myCharacter.getEnergy() >= 30)
					myActions.add(Action.STAND_F_D_DFA);

			} else if (simulatorAheadFrameData.getDistanceX() < 100) {

				myActions.add(Action.CROUCH_B);
				myActions.add(Action.CROUCH_FA);
				myActions.add(Action.STAND_B);
				myActions.add(Action.CROUCH_A);
				myActions.add(Action.AIR_B);
				if (myCharacter.getEnergy() >= 8)
					myActions.add(Action.STAND_FA);
				if (myCharacter.getEnergy() >= 1)
					myActions.add(Action.STAND_A);
				if (myCharacter.getEnergy() >= 10)
					myActions.add(Action.THROW_A);
				if (myCharacter.getEnergy() >= 20)
					myActions.add(Action.CROUCH_FB);
				if (myCharacter.getEnergy() >= 30)
					myActions.add(Action.STAND_F_D_DFA);
			} else {
				if (Math.abs(
						myMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
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

	}

	public void setOppAction() {
		oppActions.clear();

		int energy = oppCharacter.getEnergy();

		if (charName.name() == "ZEN") {
			if (oppCharacter.getState() == State.AIR) {
				for (int i = 0; i < actionAir.length; i++) {
					if (Math.abs(oppMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						oppActions.add(actionAir[i]);
					}
				}
			} else if (simulatorAheadFrameData.getDistanceX() < 50) {

				oppActions.add(Action.CROUCH_FA);
				oppActions.add(Action.STAND_B);
				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.AIR_B);
				oppActions.add(Action.AIR_DB);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_D_DF_FB);
				if (oppCharacter.getEnergy() >= 50)
					oppActions.add(Action.STAND_D_DB_BB);
			}

			else if (simulatorAheadFrameData.getDistanceX() < 85) {

				oppActions.add(Action.STAND_B);
				oppActions.add(Action.CROUCH_FA);
				oppActions.add(Action.AIR_B);
				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.AIR_DB);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_D_DF_FB);
				if (oppCharacter.getEnergy() >= 50)
					oppActions.add(Action.STAND_D_DB_BB);
			} else if (simulatorAheadFrameData.getDistanceX() < 100) {

				oppActions.add(Action.STAND_B);
				oppActions.add(Action.CROUCH_FB);
				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.STAND_FB);
				oppActions.add(Action.AIR_DB);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_D_DF_FB);
				if (oppCharacter.getEnergy() >= 50)
					oppActions.add(Action.STAND_D_DB_BB);
			} else if (simulatorAheadFrameData.getDistanceX() < 105) {
				oppActions.add(Action.CROUCH_FB);
				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.STAND_FB);
				oppActions.add(Action.STAND_D_DF_FA);
				oppActions.add(Action.AIR_DB);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_D_DF_FB);
				if (oppCharacter.getEnergy() >= 50)
					oppActions.add(Action.STAND_D_DB_BB);
			}

			else {
				if (Math.abs(
						oppMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
					oppActions.add(spSkill);
				}

				for (int i = 0; i < actionGround.length; i++) {
					if (Math.abs(oppMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						oppActions.add(actionGround[i]);
					}
				}
			}
		} else if (charName.name() == "LUD") {
			if (oppCharacter.getState() == State.AIR) {
				for (int i = 0; i < actionAir.length; i++) {
					if (Math.abs(oppMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						oppActions.add(actionAir[i]);
					}
				}
			} else if (simulatorAheadFrameData.getDistanceX() < 50) {
				oppActions.add(Action.AIR_DB);
				oppActions.add(Action.AIR_B);
				oppActions.add(Action.CROUCH_A);
				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.AIR_A);
				oppActions.add(Action.CROUCH_FA);
				oppActions.add(Action.STAND_B);
				if (oppCharacter.getEnergy() >= 1)
					oppActions.add(Action.STAND_A);
				if (oppCharacter.getEnergy() >= 8)
					oppActions.add(Action.STAND_FA);
				if (oppCharacter.getEnergy() >= 10)
					oppActions.add(Action.THROW_A);
				if (oppCharacter.getEnergy() >= 20)
					oppActions.add(Action.STAND_D_DB_BB);

			}

			else if (simulatorAheadFrameData.getDistanceX() < 85) {

				oppActions.add(Action.STAND_B);
				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.AIR_B);
				oppActions.add(Action.CROUCH_A);
				oppActions.add(Action.CROUCH_FA);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_D_DB_BA);

				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_D_DF_FA);
				if (oppCharacter.getEnergy() >= 20)
					oppActions.add(Action.STAND_D_DF_FB);
				if (oppCharacter.getEnergy() >= 10)
					oppActions.add(Action.AIR_UB);
				if (oppCharacter.getEnergy() >= 20)
					oppActions.add(Action.STAND_FB);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_F_D_DFA);

			} else if (simulatorAheadFrameData.getDistanceX() < 100) {

				oppActions.add(Action.CROUCH_B);
				oppActions.add(Action.CROUCH_FA);
				oppActions.add(Action.STAND_B);
				oppActions.add(Action.CROUCH_A);
				oppActions.add(Action.AIR_B);
				if (oppCharacter.getEnergy() >= 8)
					oppActions.add(Action.STAND_FA);
				if (oppCharacter.getEnergy() >= 1)
					oppActions.add(Action.STAND_A);
				if (oppCharacter.getEnergy() >= 10)
					oppActions.add(Action.THROW_A);
				if (oppCharacter.getEnergy() >= 20)
					oppActions.add(Action.CROUCH_FB);
				if (oppCharacter.getEnergy() >= 30)
					oppActions.add(Action.STAND_F_D_DFA);
			} else {
				if (Math.abs(
						oppMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
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
	}

	public boolean EqulCheck(Action a, Action b) {

		if (a == b)
			return true;
		else
			return false;
	}

	public enum CharacterName {
		ZEN, LUD, OTHER;

	}

	@Override
	public void roundEnd(int arg0, int arg1, int arg2) {
		commandCenter.skillCancel();
		key.empty();
	}

}
