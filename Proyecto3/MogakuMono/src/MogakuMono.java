import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import foo.FooAI;
import muta.Mutagen;
import myai.LudNormalAI;
import myai.LudSpeedAI;
import myai.MyAI;
import myai.ZenNormalAI;
import myai.ZenSpeedAI;
import myenumurate.CharaName;
import myenumurate.GameMode;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.MotionData;

public class MogakuMono implements AIInterface {

	/* Paramaters */
	double epsilon = 0.0;
	double decay = 0.95;
	double gamma = 0.95;
	double alpha = 0.2;
	double lambda = 0.1;
	/* end Parameters */
	int zen_action_weigths_number = 6;
	int lud_action_weigths_number = 1;
	int action_weigths_number = 0;
	boolean use_Q_Function = true;
	boolean use_expReplay = false;
	boolean debug = false;
	boolean debug2 = false;

	String zen_weigthP1 = "C:/Users/isain/eclipse-workspace/MogakuMono/aiData/MogakuMono/zen_weigthsP1.ser";
	String zen_weigthP2 = "C:/Users/isain/eclipse-workspace/MogakuMono/aiData/MogakuMono/zen_weigthsP2.ser";

	String statsPath = "C:/Users/isain/eclipse-workspace/MogakuMono/aiData/MogakuMono/stats.ser";

	private Key inputKey;
	private boolean player;
	private FrameData frameData;
	private CommandCenter cc;
	private Simulator simulator;
	private GameData gd;
	MotionData my_current_Motion = new MotionData();

	GameState state;
	Agent ag;
	double[] features;
	ActionVal current_action = new ActionVal(0, 0, 0);
	ActionBehaviours choosenAct = ActionBehaviours.NEUTRAL;

	double lastOppHp = 0;
	double lastMyCharHp = 0;
	boolean flagSave = false;
	boolean executingCommand = false;
	int partidas = 0;
	int count = 0;
	Stats stats  = new Stats(0, 0);
	int rounds = 0;
	boolean finishedRound = false;

	int lud_combo_state = 0;
	int lud_combo_count = 0;
	
	int negative_chain = 0;
	int shielding = 0;

	/* MCTS Variables */
	private FrameData simulatorAheadFrameData;

	private LinkedList<Action> myActions;

	private LinkedList<Action> oppActions;

	private CharacterData myCharacter;

	private CharacterData oppCharacter;

	private static int FRAME_AHEAD = 14;

	private ArrayList<MotionData> myMotion;

	private ArrayList<MotionData> oppMotion;

	private Action[] actionAir;

	private Action[] actionGround;

	Action[] opActionAir;
	Action[] opActionGround;
	private Node rootNode;
	public int simulate_time = 60;
	boolean in_behaviour = false;
	int combo_state = 0;
	int combo_level = 0;
	String tmpcharname = null;
	
	/*Gigathunder*/
	private boolean firstflag = true;
	private GameMode gamemode;
	private CharaName charname;
	MyAI ai;
	
	/*Mutagen*/
	Mutagen muta = new Mutagen();
	
	/*FooAi*/
	FooAI foo = new FooAI();
	
	/* MCTS Variables - END */

	@Override
	public void close() {
		// TODO Auto-generated method stub

		System.out.println("saved");
	}

	void firstgetInformation(){
		//Gigathunder

		int myhp = frameData.getCharacter(player).getHp();
		int ophp = frameData.getCharacter(!player).getHp();
		if (myhp < 1000 && ophp < 1000)
			this.gamemode = GameMode.NORMAL;
		else if (myhp > 1000 && ophp < 1000)
			this.gamemode = GameMode.SPEED;
		else
			this.gamemode = GameMode.LUDTRAIN;
	
		if (this.charname == CharaName.ZEN && this.gamemode == GameMode.NORMAL)
			ai = new ZenNormalAI(gd, player);
		else if (this.charname == CharaName.ZEN && this.gamemode == GameMode.SPEED)
			ai = new ZenSpeedAI(gd, player);
		else if (this.charname == CharaName.OTHER && this.gamemode == GameMode.NORMAL)
			ai = new LudNormalAI(gd, player);
		else
			ai = new LudSpeedAI(gd, player);
	}
	
	@Override
	public void getInformation(FrameData frameData) {
		// TODO Auto-generated method stub
		this.frameData = frameData;
		this.state.fd = frameData;
		this.cc.setFrameData(this.frameData, player);

		this.state.myChar = frameData.getCharacter(player);
		
		//Gigathunder
		if (frameData.getFramesNumber() >= 0) {
			if (this.firstflag) {
				this.firstflag = false;
				firstgetInformation();
			}
		}
		if (ai != null) {
			ai.getInformation(frameData);
		}
		
		//Mutagen
		muta.getInformation(frameData);
		
		//fooAi
		foo.getInformation(frameData);
	}

	@Override
	public int initialize(GameData arg0, boolean player) {
		// TODO Auto-generated method stub
		System.out.println("Inicializou");
		inputKey = new Key();
		this.player = player;
		frameData = new FrameData();
		cc = new CommandCenter();
		gd = arg0;
		simulator = gd.getSimulator();
		String weigthP1 = null;
		String weigthP2 = null;

		tmpcharname = this.gd.getCharacterName(this.player);
		
		//gigathunder
		if (tmpcharname.equals("ZEN"))
			charname = CharaName.ZEN;
		else
			charname = CharaName.OTHER;

		//Mutagen
		muta.initialize(gd, player, frameData, cc);
		
		//FooAi
		foo.initialize(gd, player, frameData, cc);
		
		if (tmpcharname.equals("ZEN")) {
			state = new GameState(gd, cc, player, "ZEN");
			weigthP1 = zen_weigthP1;
			weigthP2 = zen_weigthP2;
			action_weigths_number = zen_action_weigths_number;
		}

		state.isGameJustStarted = true;
		state.xDifference = -300;
		state.p = player;

		ag = new Agent(state, epsilon, gamma, alpha, lambda, state.features_number, use_Q_Function, player,
				use_expReplay, debug);

		myMotion = arg0.getMotionData(player);
		oppMotion = arg0.getMotionData(!player);
		this.myActions = new LinkedList<Action>();
		this.oppActions = new LinkedList<Action>();

		ObjectInputStream in;

		ObjectInputStream in3;

		try {
			if (this.player) {
				in = new ObjectInputStream(new FileInputStream(weigthP1.toString()));
			} else {
				in = new ObjectInputStream(new FileInputStream(weigthP2.toString()));
			}
			ag.actions_weigths = (ArrayList) in.readObject();
			System.out.println("Multiple Weights for each action: ");
			for (int i = 0; i < ag.actions_weigths.size(); i++) {
				double[] d = (double[]) ag.actions_weigths.get(i);
				System.out.println(Arrays.toString(d));
			}
			System.out.println("Weights Correctly loaded!!");
			in.close();
		} catch (IOException e) {
			System.out.println("Weights Not Found");
			// TODO Auto-generated catch block
			e.printStackTrace();
			ArrayList mult_weights = new ArrayList();
			for (int i = 0; i < action_weigths_number; i++) {
				double[] feat = new double[state.features_number];
				mult_weights.add(feat);
			}
			ag.setMultipleWeights(mult_weights);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			in3 = new ObjectInputStream(new FileInputStream(statsPath));
			stats = (Stats) in3.readObject();
			System.out.println("**************************************");
			System.out.println(stats.toString());
			System.out.println("**************************************");

			ag.e = stats.e;
			rounds = stats.rounds;
			System.out.println("----- Number of Rounds so far: "+rounds+"-----");
			in3.close();
		} catch (FileNotFoundException e) {
			stats = new Stats(this.epsilon,0);
			e.printStackTrace();
		} catch (IOException e) {
			stats = new Stats(this.epsilon,0);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (use_expReplay) {
			loadExpReplay();
		}
		
		System.out.println("My initial hp: " + lastMyCharHp + "Opp initial hp: " + lastOppHp);
		return 0;
	}

	@Override
	public Key input() {
		// TODO Auto-generated method stub
		return inputKey;
	}

	public void executeAttackZen(ActionBehaviours act) {
		String attack_name = act.name();
		Action selectedAction = Action.NEUTRAL;
		if (in_behaviour == false) {
			if (attack_name.contains("BEHV")) {
				if (attack_name.contains("CAUTELOUS")) {
					actionAir = new Action[] { Action.AIR_GUARD };
					actionGround = new Action[] { Action.DASH, Action.NEUTRAL, Action.STAND_A, Action.CROUCH_B,
							Action.THROW_A, Action.STAND_B, Action.CROUCH_A };

					opActionAir = new Action[] { Action.AIR_B, Action.AIR_DB, Action.AIR_FB };
					opActionGround = new Action[] { Action.STAND, Action.DASH, Action.STAND_A, Action.CROUCH_B,
							Action.STAND_B };

					simulate_time = 60;

				} else if (attack_name.contains("KICKER")) {
					actionAir = new Action[] { Action.AIR_GUARD };
					actionGround = new Action[] { Action.STAND, Action.DASH, Action.FORWARD_WALK, Action.CROUCH_A,
							Action.CROUCH_B, Action.CROUCH_FB, Action.STAND_D_DB_BB };

					opActionAir = new Action[] { Action.AIR_B, Action.AIR_DB, Action.AIR_FB };
					opActionGround = new Action[] { Action.STAND, Action.DASH, Action.CROUCH_FB };

					simulate_time = 60;
				} else if (attack_name.contains("ESCAPER")) {
					actionAir = new Action[] { Action.AIR_GUARD };
					actionGround = new Action[] { Action.BACK_STEP, Action.JUMP, Action.NEUTRAL, Action.BACK_JUMP,
							Action.FOR_JUMP };

					opActionAir = new Action[] { Action.AIR_B, Action.AIR_DB, Action.AIR_FB };
					opActionGround = new Action[] { Action.STAND_A, Action.STAND_B, Action.STAND_FA, Action.STAND_FB,
							Action.CROUCH_FB };

				} else if (attack_name.contains("ATTACKER")) {
					actionAir = new Action[] { Action.AIR_GUARD };
					actionGround = new Action[] { Action.DASH, Action.NEUTRAL, Action.STAND_A, Action.AIR_FA,
							Action.THROW_A, Action.STAND_B, Action.CROUCH_A };

					opActionAir = new Action[] { Action.AIR_B, Action.AIR_DB, Action.AIR_FB };
					opActionGround = new Action[] { Action.DASH, Action.STAND };

					simulate_time = 60;
				} else if (attack_name.contains("GRABBER")) {
					actionAir = new Action[] { Action.AIR };
					actionGround = new Action[] { Action.FORWARD_WALK, Action.DASH, Action.STAND_A, Action.THROW_A };

					opActionAir = new Action[] { Action.AIR };
					opActionGround = new Action[] { Action.STAND, Action.DASH, Action.STAND_A };

					simulate_time = 20;
					FRAME_AHEAD = 14;

				} else if (attack_name.contains("ANTIAIR")) {
					actionAir = new Action[] { Action.AIR_GUARD };
					actionGround = new Action[] { Action.FORWARD_WALK, Action.CROUCH_FA, Action.STAND_FB };

					opActionAir = new Action[] { Action.NEUTRAL };
					opActionGround = new Action[] { Action.NEUTRAL };

					simulate_time = 20;
				}

				// STOMPER: Engloba ForStomper e JumpStomper
				else if (attack_name.contains("STOMPER")) {
					actionAir = new Action[] { Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_FB, Action.AIR_DB,
							Action.AIR_B };
					actionGround = new Action[] { Action.NEUTRAL, Action.THROW_A };

					opActionAir = new Action[] { Action.AIR };
					opActionGround = new Action[] { Action.NEUTRAL };

					simulate_time = 30;
					FRAME_AHEAD = 14;
				}

				else if (attack_name.contains("AIRDOMINATOR")) {
					actionAir = new Action[] { Action.AIR_A, Action.AIR_B, Action.AIR_FA, Action.AIR_FB };
					actionGround = new Action[] { Action.NEUTRAL };

					opActionAir = new Action[] { Action.AIR };
					opActionGround = new Action[] { Action.NEUTRAL };

					simulate_time = 30;
					FRAME_AHEAD = 14;
				} else if (attack_name.contains("MCTS")) {
					actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA,
							Action.AIR_DB, Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB,
							Action.AIR_D_DF_FA, Action.AIR_D_DF_FB, Action.AIR_F_D_DFA, Action.AIR_F_D_DFB,
							Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
					actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK,
							Action.DASH, Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD,
							Action.CROUCH_GUARD, Action.THROW_A, Action.THROW_B, Action.STAND_A, Action.STAND_B,
							Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB, Action.CROUCH_FA,
							Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB, Action.STAND_F_D_DFA,
							Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
					opActionAir = actionAir;
					opActionGround = actionGround;
					simulate_time = 60;
					FRAME_AHEAD = 14;
				}

				// MCTS Prepare
				simulatorAheadFrameData = simulator.simulate(frameData, player, null, null, FRAME_AHEAD);
				myCharacter = simulatorAheadFrameData.getCharacter(player);
				oppCharacter = simulatorAheadFrameData.getCharacter(!player);
				setMyAction();
				setOppAction();
				// MCTS Prepare - End

				rootNode = new Node(simulatorAheadFrameData, null, myActions, oppActions, gd, player, cc,
						simulate_time);
				rootNode.createNode();

				selectedAction = rootNode.mcts();
				if (attack_name.contains("FOR_STOMPER") && selectedAction.name().equals(Action.NEUTRAL.name())) {
					cc.commandCall(ActionBehaviours.FOR_JUMP.name());
				} else if (attack_name.contains("UP_STOMPER") && selectedAction.name().equals(Action.NEUTRAL.name())) {
					cc.commandCall(ActionBehaviours.JUMP.name());
				} else if (selectedAction.name().contains("THROW_A")) {
					attack_name = "EXP_INF";
					choosenAct = ActionBehaviours.EXP_INF;
				} else {

					cc.commandCall(selectedAction.name());
				}

				my_current_Motion = gd.getMotionData(player).get(selectedAction.ordinal());

				// Condicion donde el personaje esta fuera de peligro 
				// por lo que permanecera parado
			} else if (!attack_name.contains("EXP")) {
				if (!attack_name.contains(ActionBehaviours.NEUTRAL.name())) {
					cc.commandCall(attack_name);
					my_current_Motion = gd.getMotionData(player).get(act.ordinal());
				}
			}
		}

		if (attack_name.contains("EXP_INF")
				&& (frameData.getCharacter(player).getX() <= -50 || frameData.getCharacter(player).getX() >= 600)
				&& frameData.getDistanceX() <= 150) {
			in_behaviour = true;
			CharacterData myChar = this.frameData.getCharacter(player);
			CharacterData opp = this.frameData.getCharacter(!player);
			double distanceFromEnemy = frameData.getDistanceX();
			String op_state = frameData.getCharacter(!player).getAction().name().toString();
			Action a = Action.STAND;
			if (distanceFromEnemy <= 80 && !op_state.contains("THROW_SUFFER") && myChar.getY() == 335) {
				a = Action.THROW_A;
				combo_state++;
				combo_level = 1;
			} else if (opp.getY() < 100 && opp.getSpeedY() < 0 && distanceFromEnemy > 60) {
				a = Action.DASH;
			} else if (opp.getY() > 150 && distanceFromEnemy <= 80) {
				a = Action.STAND_B;
			}
			cc.commandCall(a.name());
			my_current_Motion = gd.getMotionData(player).get(a.ordinal());

			/* Condicoes de parada */
			if ((!frameData.getCharacter(player).isFront() && frameData.getCharacter(player).getX() > -50)
					|| (frameData.getCharacter(player).isFront() && frameData.getCharacter(player).getX() < 600)) {
				in_behaviour = false;
				combo_state = 0;
			}
			if (combo_state >= 50
					&& (frameData.getCharacter(player).getX() > -50 && frameData.getCharacter(player).getX() < 600)) {
				in_behaviour = false;
				combo_state = 0;

			} else if (frameData.getCharacter(player).getX() > -50 && frameData.getCharacter(player).getX() < 600) {
				// System.out.println("BREAK INF3");
				in_behaviour = false;
				combo_state = 0;
			}
		} else if (attack_name.contains("EXP_INF")) {
			cc.commandCall("THROW_A");
			in_behaviour = false;
			combo_state = 0;
		}

	}

	public void executeAttackLud(ActionBehaviours act) {
		String attack_name = act.name();
		Action selectedAction = Action.NEUTRAL;
		if (in_behaviour == false) {
			lud_combo_count = 0;
			lud_combo_state = 0;
			if (attack_name.contains("BEHV")) {
				if (attack_name.contains("ATTACKER")) {
					actionAir = new Action[] { Action.AIR_GUARD };
					actionGround = new Action[] { Action.DASH, Action.STAND, Action.STAND_A, Action.THROW_A,
							Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB };

					opActionAir = new Action[] { Action.AIR_B, Action.AIR_DB, Action.AIR_FB };
					opActionGround = new Action[] { Action.DASH, Action.STAND, Action.STAND_A };

					simulate_time = 60;
				} else if (attack_name.contains("GRABBER")) {
					actionAir = new Action[] { Action.AIR };
					actionGround = new Action[] { Action.FORWARD_WALK, Action.DASH, Action.STAND_A, Action.THROW_A };

					opActionAir = new Action[] { Action.AIR };
					opActionGround = new Action[] { Action.STAND, Action.STAND_A };

					simulate_time = 60;
					FRAME_AHEAD = 14;
				} else if (attack_name.contains("STOMPER")) {
					actionAir = new Action[] { Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_FB, Action.AIR_DB,
							Action.AIR_B, Action.AIR_A, Action.AIR_FA };
					actionGround = new Action[] { Action.STAND, Action.THROW_A, Action.STAND_A };

					opActionAir = new Action[] { Action.AIR };
					opActionGround = new Action[] { Action.STAND, Action.STAND_A };

					simulate_time = 30;
					FRAME_AHEAD = 14;
				} else if (attack_name.contains("MCTS")) {
					actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA,
							Action.AIR_DB, Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB,
							Action.AIR_D_DF_FA, Action.AIR_D_DF_FB, Action.AIR_F_D_DFA, Action.AIR_F_D_DFB,
							Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
					actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK,
							Action.DASH, Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD,
							Action.CROUCH_GUARD, Action.THROW_A, Action.THROW_B, Action.STAND_A, Action.STAND_B,
							Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB, Action.CROUCH_FA,
							Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB, Action.STAND_F_D_DFA,
							Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
					opActionAir = actionAir;
					opActionGround = actionGround;
					simulate_time = 60;
					FRAME_AHEAD = 14;
				}

				// MCTS Prepare
				simulatorAheadFrameData = simulator.simulate(frameData, player, null, null, FRAME_AHEAD);
				myCharacter = simulatorAheadFrameData.getCharacter(player);
				oppCharacter = simulatorAheadFrameData.getCharacter(!player);
				setMyAction();
				setOppAction();
				// MCTS Prepare - End

				rootNode = new Node(simulatorAheadFrameData, null, myActions, oppActions, gd, player, cc,
						simulate_time);
				rootNode.createNode();
				// System.out.println(cc.getMyCharacter().y);
				// System.out.println(cc.getMyCharacter().state);
				selectedAction = rootNode.mcts();
				if (attack_name.contains("FOR_STOMPER") && selectedAction.name().equals(Action.NEUTRAL.name())) {
					cc.commandCall(ActionBehaviours.FOR_JUMP.name());
				} else if (attack_name.contains("UP_STOMPER") && selectedAction.name().equals(Action.NEUTRAL.name())) {
					cc.commandCall(ActionBehaviours.JUMP.name());
				} else if (selectedAction.name().contains("THROW_A")) {
					attack_name = "EXP_INF";
					choosenAct = ActionBehaviours.EXP_INF;
				} else {

					cc.commandCall(selectedAction.name());
				}

				my_current_Motion = gd.getMotionData(player).get(selectedAction.ordinal());

				/* Condicoes de Parada */
				// ESCAPER nao tem condicao pois sai quando estiver fora de
				// perigo(o mcts vai retornar neutro nesse caso)
				/* Condicoes de Parada - END */

			} else if (!attack_name.contains("EXP")) {
				// verificar se e um behaviour que deve ser skipado, ou uma acao
				// a ser executada
				// no 1 caso, nao salvamos o motion pois sera do neutral, e n da
				// ult acao do behaviour
				if (!attack_name.contains(ActionBehaviours.NEUTRAL.name())) {
					cc.commandCall(attack_name);
					my_current_Motion = gd.getMotionData(player).get(act.ordinal());
				} // n salvamos o motion do neutral
			}
		}

		if (attack_name.contains("EXP_INF") && frameData.getDistanceX() <= 100) {
			// expert behaviour
			// System.out.println("INSIDE EXP INF");
			in_behaviour = true;
			CharacterData myChar = this.frameData.getCharacter(player);
			CharacterData opp = this.frameData.getCharacter(!player);
			double distanceFromEnemy = frameData.getDistanceX();
			String op_state = frameData.getCharacter(!player).getAction().name().toString();
			Action a = Action.STAND;
			if (distanceFromEnemy <= 95 && !op_state.contains("THROW_SUFFER") && myChar.getY() == 335
					&& myChar.getEnergy() < 70) {
				a = Action.THROW_A;
				lud_combo_state = 1;
			} else if (distanceFromEnemy <= 95 && !op_state.contains("THROW_SUFFER") && myChar.getY() == 335
					&& myChar.getEnergy() >= 70) {
				a = Action.STAND_F_D_DFB;
				lud_combo_state = 0;
				in_behaviour = false;
				lud_combo_count = 0;
			} else if (op_state.contains("THROW_SUFFER") && lud_combo_state <= 2 && myChar.getY() == 335
					&& distanceFromEnemy < 20) {
				a = Action.BACK_JUMP;
				lud_combo_state++;
			} else if (op_state.contains("THROW_SUFFER") && opp.getY() < 335 && lud_combo_state >= 3
					&& lud_combo_state <= 10) {
				a = Action.STAND_B;
				lud_combo_state++;
				// System.out.println("STATE: "+lud_combo_state);
			} else if (myChar.getY() >= 0 && myChar.getY() < 200 && myChar.getSpeedY() > 4 && lud_combo_state >= 9) {
				a = Action.STAND_A;
				lud_combo_state = 0;
			}
			cc.commandCall(a.name());

			/* Condicoes de Parada */
			lud_combo_count++;
			// System.out.println(lud_combo_count);
			if (lud_combo_count > 200) {

				in_behaviour = false;
				lud_combo_count = 0;
				lud_combo_state = 0;
			}

			my_current_Motion = gd.getMotionData(player).get(a.ordinal());

		} else if (attack_name.contains("EXP_INF")) {
			cc.commandCall("THROW_A");
			in_behaviour = false;
			combo_state = 0;
			lud_combo_count = 0;
			lud_combo_state = 0;
		}

	}
	
	public void executeGigaThunder(){
		inputKey.empty();
		cc.setFrameData(frameData, player);
		cc.skillCancel();
		Action ac = ai.getDoAction();
		String commandstr = (ac.name());
		cc.commandCall(commandstr);
	}
	
	public void executeMutagen(){
		muta.processing();
	}
	
	public void executeFoo(){
		foo.processing();
	}
	
	
	public boolean expertFilter(){
			
		if(shielding>50){
			if(frameData.getCharacter(player).isFront() && frameData.getCharacter(player).getY() == 335){
				if (tmpcharname.equals("ZEN")){
					inputKey.U = true;
					inputKey.R = true;
				}else{
					inputKey.B = true;
				}
			}
			if(!frameData.getCharacter(player).isFront() && frameData.getCharacter(player).getY() == 335){
				if (tmpcharname.equals("ZEN")){
					inputKey.U = true;
					inputKey.L = true;
				}else{
					
					inputKey.B = true;
				}
			}
			shielding = 0;
			negative_chain = 0;
			
			return true;
		}
		 if(negative_chain>2){
			if(frameData.getCharacter(player).isFront() && frameData.getCharacter(player).getY() == 335){
				if (tmpcharname.equals("ZEN")){
					inputKey.L = true;
					inputKey.D = true;
				}else{
					if (frameData.getCharacter(player).getX() < -60 || frameData.getCharacter(player).getX()> 700) {
						inputKey.D = true;
					}
					inputKey.L = true;
				}
			}
			if(!frameData.getCharacter(player).isFront() && frameData.getCharacter(player).getY() == 335){
				if (tmpcharname.equals("ZEN")){
					inputKey.R = true;
					inputKey.D = true;
				}else{
					if (frameData.getCharacter(player).getX() < -60 || frameData.getCharacter(player).getX()> 700) {
						inputKey.D = true;
					}
					inputKey.R = true;
				}
			}
			shielding++;
			return true;
		}
		return false;
	}
	@Override
	public void processing() {
		inputKey.A = false;
		inputKey.B = false;
		inputKey.U = false;
		inputKey.L = false;
		inputKey.R = false;
		inputKey.D = false;
		// First we check whether we are at the end of the round
		if (!frameData.getEmptyFlag() && frameData.getRemainingTime() > 0) {

			state.updateState(cc, frameData, player);
			count++;
			if (cc.getSkillFlag()) {
				// If there is a previous "command" still in execution, then
				// keep doing it
				inputKey = cc.getSkillKey();
			} else {
				flagSave = true;
				// We empty the keys and cancel skill just in case
				inputKey.empty();
				cc.skillCancel();

				if (count >= my_current_Motion.getFrameNumber() && in_behaviour == false) {
					executingCommand = false;
					count = 0;
					// recebe recompensa
					double reward = (Math.abs(lastOppHp - state.opp.getHp()) - Math.abs(lastMyCharHp - state.myChar.getHp()));
					System.out.println("REWARD: "+reward);
					lastOppHp = state.opp.getHp();
					lastMyCharHp = state.myChar.getHp();
					
					if(reward<0){
						System.out.println("Negative chain incremented");
						System.out.println(negative_chain);
						negative_chain++;
					}else if(reward>0){
						System.out.println("negative chain reset");
						negative_chain = 0;
					}

					// IMPORTANTE: seleciona as possiveis acoes do estado daqui
					// a 14 frames
					FrameData frameDataAdv = simulator.simulate(frameData, this.player, null, null, 17);
					state.setPossibleActions(frameDataAdv);
					System.out.println("possible Actions: "+state.myActions);
					// atualiza pesos/aprendizado e recebe prox acao
					ActionVal next_act = ag.update(frameData, reward, current_action.act_weight);
					current_action = next_act;
					choosenAct = state.myActions.get(current_action.act_index);

					System.out.println("Choosen act: "+choosenAct.name());
					}
				if(!expertFilter()){
					if(choosenAct.name().equals(ActionBehaviours.BEHV_GIGA.name())){
						executeGigaThunder();
					}
					else if(choosenAct.name().equals(ActionBehaviours.BEHV_FOO.name())){
						executeFoo();
					}
					else if(choosenAct.name().equals(ActionBehaviours.BEHV_MUTA.name())){
						executeMutagen();
					}
					else{
						if (tmpcharname.equals("ZEN")) {
							executeAttackZen(choosenAct);
						} else if (tmpcharname.equals("LUD")) {
							executeAttackLud(choosenAct);
						} else {
							executeAttackZen(choosenAct);
						}
					}
				}

			}
		} else {
			if (flagSave) {
				rounds++;
				stats.rounds = rounds;
				flagSave = false;
				ag.e = ag.e * decay;
				stats.e = stats.e * decay;
				System.out.println("----- Number of Rounds so far: " + rounds + "-----");
				System.out.println("Epsilon: " + stats.e);
				System.out.println(lastMyCharHp);
				System.out.println(lastOppHp);
				stats.setScore(Math.abs(lastOppHp) - Math.abs(lastMyCharHp));
				partidas++;

			}
			state.isGameJustStarted = true;

		}
		if (frameData.getRemainingTime() == 1 && finishedRound == false) {
			 try {
			 save();
			 } catch (IOException e) {
			// // TODO Auto-generated catch block
			 e.printStackTrace();
			 }
			finishedRound = true;
		} else if (frameData.getRemainingTime() > 1) {
			finishedRound = false;
		}
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
			for (int i = 0; i < opActionAir.length; i++) {
				if (Math.abs(oppMotion.get(Action.valueOf(opActionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(opActionAir[i]);
				}
			}
		} else {

			for (int i = 0; i < opActionGround.length; i++) {
				if (Math.abs(oppMotion.get(Action.valueOf(opActionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(opActionGround[i]);
				}
			}
		}
	}

	public void loadExpReplay() {

		ObjectInputStream in;
		try {
			if (this.player) {
				in = new ObjectInputStream(new FileInputStream("Replays/batchP1.ser"));
			} else {
				in = new ObjectInputStream(new FileInputStream("Replays/batchP2.ser"));
			}
			TransitionStorage ts = (TransitionStorage) in.readObject();
			for (int i = 0; i < ts.size; i++) {
				double[] arr = (double[]) ts.features.get(i);
				int act = 0;
				double target = 0;
				double[] dm = new double[arr.length];
				act = (int) ts.acts.get(i);
				target = (double) ts.targets.get(i);
				Transition t = new Transition(dm, target, act);
				ag.storage.add(t);
			}

		} catch (IOException | ClassNotFoundException e) {

			e.printStackTrace();
		}

	}

	void save() throws IOException{
		ObjectOutputStream out;
		ObjectOutputStream out1;
		tmpcharname = this.gd.getCharacterName(this.player);
		System.out.println("*-*-*-*-*-*-*-*-*-*-*");
		System.out.println(Arrays.toString(ag.state.features));
		System.out.println("*-*-*-*-*-*-*-*-*-*-*");
		if (tmpcharname.equals("ZEN")) {
			try {
				if(this.player){
					out = new ObjectOutputStream(
					    new FileOutputStream(zen_weigthP1.toString())
					);
				}else{
					out = new ObjectOutputStream(
						    new FileOutputStream(zen_weigthP2.toString())
						);
				}
				out.writeObject(ag.actions_weigths);
				
				out.flush();
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		try{		
			out1= new ObjectOutputStream(
				    new FileOutputStream(statsPath)
				);
			out1.writeObject(stats);
			out1.flush();
			out1.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
 	
 	}
	
	@Override
	public void roundEnd(int arg0, int arg1, int arg2) {
		cc.skillCancel();
		inputKey.empty();
	}

}
