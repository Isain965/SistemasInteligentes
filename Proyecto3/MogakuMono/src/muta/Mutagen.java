package muta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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
 * Based heavily off of MCTS AI by Taichi Miyazaki. Uses LGR and finite states
 * to improve decisions.
 *
 * @author Connor Gregorich-Trevor
 */
public class Mutagen  {

	/** General AI Data */
	private Simulator simulator;
	private Key key;
	private CommandCenter commandCenter;
	private boolean playerNumber;
	private GameData gameData;
	private String name;

	/** File IO Resources */
	private File file;

	/** The queue of moves that were input and actually used. */
	private Queue<Situation> moveQueue;

	/**
	 * The queue of moves input. Read and then pushed into moveQueue. For
	 * further documentation, see the comments in updateSituationMap()
	 */
	private Queue<Situation> inputQueue;

	/** The map containing optimal situations to use moves in. */
	// TODO: Document more.
	private Map<String, Situation> situationMap;

	/** Main FrameData */
	private FrameData frameData;

	/** Data with FRAME_AHEAD frames ahead of FrameData */
	private FrameData simulatorAheadFrameData;

	/** All actions that could be performed by self character */
	private LinkedList<Action> myActions;

	/** All actions that could be performed by the opponent character */
	private LinkedList<Action> oppActions;

	/** self information */
	private CharacterData myCharacter;

	/** opponent information */
	private CharacterData oppCharacter;

	/**
	 * Number of adjusted frames (following the same recipe in JerryMizunoAI)
	 */
	private static final int FRAME_AHEAD = 14;

	/** Number of frames to wait to check the success of a move */
	private static final int FRAME_WAIT = 60;

	/** Stage Data */
	private static final int STAGELEFT = -240;
	private static final int STAGERIGHT = 680;
	private static final int CORNERLENIENCY = 200;

	/** Motion and Actions */
	private ArrayList<MotionData> myMotion;
	private ArrayList<MotionData> oppMotion;
	private Action[] actionAir;
	private Action[] actionGround;
	private Action[] actionGroundMeleeMid;
	private Action[] actionGroundFar;
	private Action[] actionGroundPersonal;
	private Action[] actionGroundNeutral;
	private Action[] actionMyAir;
	// TODO: Remove spSkill from options if Zen is too close to opponent.
	private Action spSkill;

	/** Node for performing MCTS */
	private Node rootNode;

	/** Spacing Constants */
	private int closeConst = 12;

	/** True if in debug mode, which will output related log */
	public static final boolean DEBUG_MODE = false;

	/**
	 * True if in mutagen debug mode, which will output log specific to Mutagen
	 */
	public static final boolean MUTAGEN_DEBUG = false;

	/** Closes the AI and writes the situation map to a .ser file. */
	public void close() {
		if (MUTAGEN_DEBUG) {
			System.out.println("Mutagen: Writing to file...");
		}
		try {
			FileOutputStream fileout = new FileOutputStream(file);
			ObjectOutputStream objout = new ObjectOutputStream(fileout);
			objout.writeObject(situationMap);
			objout.close();
			if (MUTAGEN_DEBUG) {
				System.out.println("Mutagen: Serialization complete.");
			}
		} catch (IOException e) {
			System.out.println("Mutagen: Error Fatal File Write.");
			e.printStackTrace();
		}
		if (MUTAGEN_DEBUG) {
			System.out.println("Mutagen: Closed successfully.");
		}
	}

	/** Gets the frameData for both AIs and sets up a command center. */
	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, playerNumber);

		myCharacter = frameData.getCharacter(playerNumber);
		oppCharacter = frameData.getCharacter(!playerNumber);
	}

	/** Initializes the AI. */
	@SuppressWarnings("unchecked")
	public int initialize(GameData gameData, boolean playerNumber,FrameData frameData,
			CommandCenter commandCenter) {
		this.playerNumber = playerNumber;
		this.gameData = gameData;

		this.key = new Key();
		this.frameData = frameData;
		this.commandCenter = commandCenter;

		this.myActions = new LinkedList<Action>();
		this.oppActions = new LinkedList<Action>();

		this.name = gameData.getCharacterName(playerNumber);

		simulator = gameData.getSimulator();

		// Sets up the queues of moves and inputs to be used in learning and
		// adaption.
		situationMap = new HashMap<String, Situation>();
		moveQueue = new LinkedList<Situation>();
		inputQueue = new LinkedList<Situation>();
		for (int i = 0; i < FRAME_WAIT; i++) {
			this.moveQueue.add(new Situation());
		}
		for (int i = 0; i < FRAME_AHEAD + 4; i++) {
			this.inputQueue.add(new Situation());
		}

		// Gets the motion data for both players.
		myMotion = gameData.getMotionData(playerNumber);
		oppMotion = gameData.getMotionData(!playerNumber);

		// The default action arrays. Also used for predicting opponent actions.
		actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA, Action.AIR_DB,
				Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA, Action.AIR_D_DF_FB,
				Action.AIR_F_D_DFA, Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
		actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
		spSkill = Action.STAND_D_DF_FC;

		// Sets up the AI states for Zen
		// Zen has far and away the most complicated state system, and is the
		// one I spent the
		// most time testing. For anyone trying to base their AI off this, take
		// note: The most
		// important part of Mutagen's rules for Zen is that he doesn't use
		// STAND_DB_BB at close
		// range. This is a huge issue for MCTS, since using that move at close
		// range yields an
		// immediate reward in terms of a lot of damage that is very hard to
		// block, but it also
		// leaves Zen totally vulnerable for every type of punish. Preventing
		// Zen from firing
		// projectiles in neutral is also important, since the endlag on them is
		// ridiculous.
		if (name.equals("ZEN")) {
			if (MUTAGEN_DEBUG) {
				System.out.println("Mutagen: Now switching to Zen mode.");
			}
			closeConst = 12;
			actionMyAir = new Action[] { Action.AIR_GUARD, Action.AIR_B, Action.AIR_DA, Action.AIR_DB, Action.AIR_FA,
					Action.AIR_FB, Action.AIR_UB, Action.AIR_D_DF_FA, Action.AIR_D_DF_FB, Action.AIR_F_D_DFA,
					Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
			actionGroundNeutral = new Action[] { Action.BACK_STEP, Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP,
					Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A, Action.THROW_B, Action.STAND_A,
					Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB,
					Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_F_D_DFA, Action.STAND_F_D_DFB,
					Action.STAND_D_DB_BB };
			actionGroundPersonal = new Action[] { Action.STAND_D_DB_BA, Action.THROW_A, Action.THROW_B, Action.STAND_A,
					Action.CROUCH_A, Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_F_D_DFA,
					Action.CROUCH_B, Action.STAND_B, Action.CROUCH_GUARD, Action.STAND_GUARD };
			actionGroundFar = new Action[] { Action.FORWARD_WALK, Action.DASH, Action.CROUCH_GUARD, Action.STAND_GUARD,
					Action.JUMP, Action.FOR_JUMP, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB };
			actionGroundMeleeMid = new Action[] { Action.BACK_STEP, Action.STAND_GUARD, Action.CROUCH_GUARD,
					Action.THROW_A, Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B,
					Action.STAND_FA, Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_F_D_DFA,
					Action.STAND_F_D_DFB, Action.JUMP, Action.FOR_JUMP };
		}

		// Sets up the AI states for Lud.
		// These states are essentially the same as the MCTS AI, since most of
		// Lud's strength
		// comes from the rules implemented for him and from the LGR algorithm.
		if (name.equals("LUD")) {
			if (MUTAGEN_DEBUG) {
				System.out.println("Mutagen: Now switching to Lud mode.");
			}
			closeConst = 30;
			actionGroundMeleeMid = actionGround;
			actionGroundFar = actionGround;
			actionGroundNeutral = actionGround;
			actionMyAir = actionAir;
			actionGroundPersonal = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.STAND_GUARD,
					Action.CROUCH_GUARD, Action.THROW_A, Action.THROW_B, Action.STAND_A, Action.STAND_B,
					Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB, Action.CROUCH_FA,
					Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB, Action.STAND_F_D_DFA,
					Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
		}

		if (MUTAGEN_DEBUG) {
			System.out.println("Mutagen: Attempting to load data...");
		}
		file = new File("data/aiData/Mutagen/" + name + ".ser");
		try {
			FileInputStream filein = new FileInputStream(file);
			ObjectInputStream objin = new ObjectInputStream(filein);
			situationMap = (Map<String, Situation>) objin.readObject();
			if (MUTAGEN_DEBUG) {
				for (String str : situationMap.keySet()) {
					System.out.println(str);
				}
			}
			objin.close();
			if (MUTAGEN_DEBUG) {
				System.out.println("Mutagen: Successfully loaded data from data/aiData/Mutagen/" + name + ".ser.");
			}
		} catch (IOException e) {
			System.out.println("Mutagen: File was not read.");
		} catch (ClassNotFoundException e) {
			System.out.println("Mutagen Error Fatal Serialization.");
		}
		if (MUTAGEN_DEBUG) {
			System.out.println("Mutagen: Tracking " + situationMap.size() + " optimal situations.");
			System.out.println("Mutagen: Initialization complete.");
		}
		return 0;
	}

	/** Gets input */
	public Key input() {
		return key;
	}

	/** The algorithm for making decisions. */
	public void processing() {
		
			// If the move queue is out of sync, this will print an error.
			// This print statement should NEVER occur.
			if (moveQueue.size() != FRAME_WAIT) {
				System.out.println("Mutagen Error Fatal Queue (" + moveQueue.size() + ")");
			}

			// Updates the situation map and queues
			updateSituationMap();

			// Beginning of the decision process
			if (commandCenter.getSkillFlag()) {
				key = commandCenter.getSkillKey();
				inputQueue.add(new Situation());
			} else {
				key.empty();
				commandCenter.skillCancel();
				mctsPrepare();
				rootNode = new Node(simulatorAheadFrameData, null, myActions, oppActions, gameData, playerNumber,
						commandCenter);
				rootNode.createNode();
				Action bestAction = rootNode.mcts();
				if (Mutagen.DEBUG_MODE) {
					rootNode.printNode(rootNode);
				}

				Action myAction = myCharacter.getAction();
				Action oppAction = oppCharacter.getAction();
				Situation myChoice;
				Situation optimal = getSituation();

				if ((myAction == Action.DOWN || myAction == Action.RISE || myAction == Action.CHANGE_DOWN)
						&& oppCharacter.getState() != State.AIR) {
					// TODO: Implement MCTS for block types on getup
					/*
					 * if (frameData.getDistanceX() <= 10 &&
					 * oppCharacter.getEnergy() +
					 * myMotion.get(Action.valueOf(Action.STAND_D_DB_BB.name()).
					 * ordinal()) .getAttackStartAddEnergy() < 0) {
					 * commandCenter.commandCall(Action.STAND_A.name()); } else
					 * {
					 */
					commandCenter.commandCall("1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1");
					// }
					myChoice = new Situation();
				} else if (optimal.getMyAction() != "NO ACTION"
						&& optimal.getRequiredEnergy() + myCharacter.getEnergy() >= 0 && name.equals("LUD")) {
					// Perform an optimal action as determined by the hashmap.
					commandCenter.commandCall(optimal.getMyAction());
					myChoice = new Situation(optimal.getMyAction(), oppAction.name(), oppCharacter.getHp(),
							myCharacter.getHp(), frameData.getDistanceX(),
							myMotion.get(Action.valueOf(optimal.getMyAction()).ordinal()).getAttackStartAddEnergy(),
							oppCharacter.getEnergy(), myCharacter.getState(), oppCharacter.getState(),
							myCharacter.getHitCount(), oppCharacter.getHitCount());
					if (MUTAGEN_DEBUG) {
						System.out.println("Mutagen: Using optimal move " + optimal.getMyAction());
					}
				} else {
					// converts stand and crouch guard actions to lasting many
					// frames
					// TODO: Need to make this some sort of global function so
					// that ALL blocks are converted
					if (bestAction.name() == "STAND_GUARD") {
						commandCenter.commandCall("4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4");
					} else if (bestAction.name() == "CROUCH_GUARD") {
						commandCenter.commandCall("1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1");
					} else {
						commandCenter.commandCall(bestAction.name());
					}

					// Ignores moves that are not guard-related or do not deal
					// damage.
					// TODO: Check this for correctness.
					if (myMotion.get(Action.valueOf(bestAction.name()).ordinal()).getAttackHitDamage() <= 0
							&& !bestAction.name().equals("STAND_GUARD") && !bestAction.name().equals("CROUCH_GUARD")) {
						myChoice = new Situation();
					}

					else {
						// Adds the move that was used to the situation map.
						myChoice = new Situation(bestAction.name(), oppAction.name(), oppCharacter.getHp(),
								myCharacter.getHp(), frameData.getDistanceX(),
								myMotion.get(Action.valueOf(bestAction.name()).ordinal()).getAttackStartAddEnergy(),
								oppCharacter.getEnergy(), myCharacter.getState(), oppCharacter.getState(),
								myCharacter.getHitCount(), oppCharacter.getHitCount());
					}

				}
				inputQueue.add(myChoice);
			}
		}
	

	/**
	 * Determine whether or not the AI can perform an action
	 *
	 * @return whether or not the AI can perform an action
	 */
	public boolean canProcessing() {
		System.out.println("CanProcessing");
		return !frameData.getEmptyFlag() && frameData.getRemainingTime() > 0;
	}

	/**
	 * Some preparation for MCTS Perform the process for obtaining FrameData
	 * with 14 frames ahead
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

		if (myCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionMyAir.length; i++) {
				if (Math.abs(myMotion.get(Action.valueOf(actionMyAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionMyAir[i]);
				}
			}
		} else {
			// Each of these states is used for a different distance.
			if (Math.abs(myMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
				myActions.add(spSkill);
			}
			if (frameData.getDistanceX() < closeConst) {
				for (int i = 0; i < actionGroundPersonal.length; i++) {
					// Stops Lud from using moves with extremely long lag when
					// very near the opponent
					if (Math.abs(myMotion.get(Action.valueOf(actionGroundPersonal[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						int startup = myMotion.get(Action.valueOf(actionGroundPersonal[i].name()).ordinal())
								.getAttackStartUp();
						int frames = myMotion.get(Action.valueOf(actionGroundPersonal[i].name()).ordinal()).frameNumber;
						int endlag = frames - startup;
						if (name.equals("LUD") && endlag < 45) {
							myActions.add(actionGroundPersonal[i]);
						} else if (!name.equals("LUD")) {
							myActions.add(actionGroundPersonal[i]);
						}
					}
				}
				if (myActions.size() == 0) {
					for (int i = 0; i < actionGroundPersonal.length; i++) {
						if (Math.abs(myMotion.get(Action.valueOf(actionGroundPersonal[i].name()).ordinal())
								.getAttackStartAddEnergy()) <= energy) {
							myActions.add(actionGroundPersonal[i]);
						}
					}
				}
			} else if (frameData.getDistanceX() < 95) {
				for (int i = 0; i < actionGroundMeleeMid.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionGroundMeleeMid[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionGroundMeleeMid[i]);
					}
				}
			} else if (frameData.getDistanceX() <= 500) {
				for (int i = 0; i < actionGroundNeutral.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionGroundNeutral[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionGroundNeutral[i]);
					}
				}
			} else if (frameData.getDistanceX() > 500) {
				for (int i = 0; i < actionGroundFar.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionGroundFar[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionGroundFar[i]);
					}
				}
			} else {
				for (int i = 0; i < actionGround.length; i++) {
					if (Math.abs(myMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionGround[i]);
					}
				}
			}
		}
		// Various specific rules

		// Prevents characters from moving backwards when cornered, or Lud from
		// moving backwards at all.
		if (myCharacter.getX() < STAGELEFT + CORNERLENIENCY || myCharacter.getX() > STAGERIGHT - CORNERLENIENCY
				|| name.equals("LUD")) {
			myActions.remove(Action.BACK_STEP);
			myActions.remove(Action.BACK_JUMP);
		}
		// Stops Zen from throwing projectiles when the opponent can slide under
		// them.
		if (name.equals("ZEN") && oppCharacter.getEnergy()
				+ myMotion.get(Action.valueOf(Action.STAND_D_DB_BB.name()).ordinal()).getAttackStartAddEnergy() > 0) {
			myActions.remove(Action.STAND_D_DF_FA);
			myActions.remove(Action.STAND_D_DF_FB);
			myActions.remove(Action.STAND_D_DF_FC);
		}
	}

	/** Predict the opponent's action using MCTS */
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

	/**
	 * Determines if the current situation matches any situations in the hashmap
	 * of optimal moves.
	 */
	private Situation getSituation() {
		Situation ret = new Situation();
		Situation situation = null;
		for (int i = -1; i <= 1; i++) {
			// for (int j = 0; j <= oppCharacter.getEnergy(); j++) {
			String oppState = stateToString(oppCharacter.getState());
			String myState = stateToString(myCharacter.getState());
			situation = situationMap.get((frameData.getDistanceX() + i) + myState + oppState + "_"
					+ myCharacter.getHitCount() + "_" + oppCharacter.getHitCount() + "_");
			// + (oppCharacter.getEnergy() - j));
			// }
		}
		if (situation != null) {
			return situation;
		}
		return ret;
	}

	/**
	 * Updates the map of situations by cycling through the queues. Implements
	 * LGR algorithm.
	 */
	private void updateSituationMap() {
		Situation polledInput = inputQueue.poll();
		// The input queue is polled first, and if the move that was input is
		// the move that the
		// character is performing, then that move is added to the movequeue.
		// Otherwise, it is
		// dropped. This is necessary because the AI is inputting moves
		// CONSTANTLY, and there
		// needed to be a way to check whether the input actually matched with
		// the output.
		// Hence the two separate queues.
		if (polledInput.getMyAction() != myCharacter.getAction().name()) {
			moveQueue.add(new Situation());
		} else {
			moveQueue.add(polledInput);
		}
		Situation polledMove = moveQueue.poll();
		// This is likely not a very efficient method for keys in the map, and
		// with more time
		// I would try to find a better solution.
		if (polledMove.getMyAction() != "NO ACTION") {
			if (polledMove.getoppHp() - oppCharacter.getHp() + myCharacter.getHp() - polledMove.getmyHp() > 0) {
				String key = null;
				String oppState = stateToString(polledMove.getOppState());
				String myState = stateToString(polledMove.getState());
				key = polledMove.getDistance() + myState + oppState + "_" + polledMove.getCombo() + "_"
						+ polledMove.getOppCombo() + "_";
				// + polledMove.getOppEnergy();
				situationMap.put(key, polledMove);
				if (MUTAGEN_DEBUG) {
					System.out.println("Put: " + key);
				}
			} else {
				situationMap.remove(key);
			}
		}
	}

	/** Converts states to strings. */
	private String stateToString(State state) {
		if (state == State.AIR) {
			return "AIR";
		} else {
			return "GROUND";
		}
	}

	public void roundEnd(int arg0, int arg1, int arg2) {
		commandCenter.skillCancel();
		key.empty();
	}

}
