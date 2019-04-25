
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import aiinterface.CommandCenter;
import enumerate.State;
import simulator.Simulator;
import struct.AttackData;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.HitArea;
import struct.MotionData;

public class GameState {
	/* Paramaters */
	int features_number = 46;
	/* End Parameters */
	int distance;
	int distanceY;
	int energy;
	CommandCenter cc;
	CharacterData opp;
	CharacterData myChar;
	GameData gd;
	Simulator simulator;
	boolean isGameJustStarted;
	int xDifference;
	int enemySpeedX;
	int enemySpeedY;
	int projetile_number = 0;
	double myLastX = 0;
	double myLastY = 0;
	double opLastX = 0;
	double opLastY = 0;
	boolean closeProjetile = false;
	String oppmotion;
	boolean p;
	boolean player;
	public int comboChain = 0;
	String MySkillName, OppSkillName;
	double[] features;
	double[] previous_features;
	// ArrayList features = new ArrayList();
	public ActionBehaviours[] actionAir;
	public ActionBehaviours[] actionGround;
	public ActionBehaviours[] actionGroundBehaviour;
	public ActionBehaviours[] actionAirBehaviour;
	public ActionBehaviours spSkill;
	public FrameData fd;
	/** All actions that can be performed by the character */
	public LinkedList<ActionBehaviours> myActions;
	public LinkedList<ActionBehaviours> oppActions;
	/**
	 * All actions that can be performed by the character on the index of 0-39
	 */
	ArrayList<Integer> myIndexActions = new ArrayList();
	ArrayList<Integer> oppIndexActions = new ArrayList();

	private ActionBehaviours[] behaviourAir;
	private ActionBehaviours[] behaviourGlobal;
	private ActionBehaviours[] behaviourGroundOpAir;
	private ActionBehaviours[] behaviourGroundOpGround;

	public String charType = "ZEN";

	public GameState(GameData gd, CommandCenter cc, boolean player, String ct) {
		features = new double[features_number];
		previous_features = new double[features_number];
		this.player = player;

		this.gd = gd;
		this.cc = cc;
		// simulator = this.gd.getSimulator();
		charType = ct;
		if (charType.contains("ZEN")) {
			actionAir = new ActionBehaviours[] {};
			actionGround = new ActionBehaviours[] {};// 11
			behaviourAir = new ActionBehaviours[] {}; // 2
			behaviourGlobal = new ActionBehaviours[] { 
					ActionBehaviours.BEHV_MCTS,
					ActionBehaviours.BEHV_GIGA,ActionBehaviours.BEHV_FOO,ActionBehaviours.BEHV_MUTA};
			behaviourGroundOpAir = new ActionBehaviours[] { 
					ActionBehaviours.BEHV_ANTIAIR// 1
			};
			behaviourGroundOpGround = new ActionBehaviours[] { 
					ActionBehaviours.BEHV_KICKER
					};

		} else if (charType.contains("LUD")) {
			actionAir = new ActionBehaviours[] {}; // 4
			actionGround = new ActionBehaviours[] {};// 11
			behaviourAir = new ActionBehaviours[] {}; // 2
			behaviourGlobal = new ActionBehaviours[] {
					ActionBehaviours.BEHV_GIGA,
					};
			behaviourGroundOpAir = new ActionBehaviours[] {};
			behaviourGroundOpGround = new ActionBehaviours[] { };// 3
		}

		this.myActions = new LinkedList<ActionBehaviours>();
		this.oppActions = new LinkedList<ActionBehaviours>();
	}

	public void updateState(CommandCenter cc, FrameData frameData, boolean player) {
		cc.setFrameData(frameData, player);
		this.fd = frameData;
		this.distance = frameData.getDistanceX();
		this.distanceY = frameData.getDistanceY();
		this.energy = frameData.getCharacter(player).getEnergy();
		this.myChar = frameData.getCharacter(player);
		this.opp = frameData.getCharacter(!player);
		this.xDifference = this.myChar.getX() - this.opp.getX();
		this.enemySpeedX = frameData.getCharacter(!player).getSpeedX();
		this.enemySpeedY = frameData.getCharacter(!player).getSpeedY();

		this.OppSkillName = frameData.getCharacter(!player).getAction().name().toString();
		this.MySkillName = frameData.getCharacter(player).getAction().name().toString();

	}

	public void setFeatures(double[] feat) {
		features = feat;
		previous_features = features;
		features = feat;
	}

	public double[] getParameters(FrameData frames, double reward) {

		CharacterData myChar = frames.getCharacter(p);
		CharacterData opp = frames.getCharacter(!p);
		double[] new_features = new double[features.length];
		boolean myChar_isFront = myChar.isFront();
		boolean opp_isFront = opp.isFront();
		projetile_number = 0;
		closeProjetile = false;
		int v = 0;
		int my_projetile_number = 0;
		int op_projetile_number = 0;
		boolean closeProjetile = false;
		Deque<AttackData> atks = frames.getProjectiles();

		String OppSkillName = frames.getCharacter(!player).getAction().name().toString();

		if (!opp.getState().equals(State.AIR) && !opp.getState().equals(State.DOWN)) {
			new_features[v] = 1;

		}
		v++;
		if (opp.getState().equals(State.AIR)) {
			new_features[v] = 1;

		}
		v++;
		if (opp.getState().equals(State.DOWN)) {
			new_features[v] = 1;

		}
		v++;

		double myX = myChar.getX() + 120;
		double myY = myChar.getY();
		double oppX = opp.getX() + 120;
		double oppY = opp.getY();
		// fix do bug da posicao errada:
		myX += 80;
		oppX += 80;
		if (myChar_isFront) {
			myX -= 80;
		}
		if (opp_isFront) {
			oppX -= 80;
		}
		if (myX > 880) {
			myX = 880;
		}
		if (oppX > 880) {
			oppX = 880;
		}
		if (myX < 0) {
			myX = 0;
		}
		if (oppX < 0) {
			oppX = 0;
		}
		// 0 campo agora varia de 0 a 880
		// fim fix

		// Saber se esta voltado pro canto
		boolean diff = Math.abs(myX - 440) < Math.abs(oppX - 440);

		if (diff) {
			new_features[v] = 1;

		}
		v++;
		if (!diff) {
			new_features[v] = 1;

		}
		v++;

		// Fixbug: para tratar a posicao de maneira simetrica, devemos ajustar
		// a posicao x para o meio do framebox. A pos x normalmente
		// vem do vertice do canto para onde o char esta voltado.
		if (myChar_isFront) {
			myX -= 20;
		} else {
			myX += 20;
		}

		// ta nos cantos
		if (myX <= 180) {
			new_features[v] = 1;

		}
		v++;
		if (myX >= 700) {
			new_features[v] = 1;

		}
		v++;
		// ta nas secoes do meio
		if (myX > 180 && myX < 700) {
			new_features[v] = 1;

		}
		v++;
		if ((myX >= 180 && myX < 260) || (myX >= 620 && myX < 700)) {
			new_features[v] = 1;

		}
		v++;
		if ((myX >= 260 && myX < 360) || (myX >= 420 && myX < 620)) {
			new_features[v] = 1;

		}
		v++;
		if (myX >= 360 && myX < 420) {
			new_features[v] = 1;

		}
		v++;

		// Reverter fixBug
		if (myChar_isFront) {
			myX += 20;
		} else {
			myX -= 20;
		}

		// op ta em cima
		if (oppY >= 300 && oppY < 350) {
			new_features[v] = 1;
		}
		v++;
		if (oppY >= 250 && oppY < 300) {
			new_features[v] = 1;
		}
		v++;
		if (oppY >= 200 && oppY < 250) {
			new_features[v] = 1;
		}
		v++;
		if (oppY >= 150 && oppY < 200) {
			new_features[v] = 1;
		}
		v++;
		if (oppY >= 50 && oppY < 150) {
			new_features[v] = 1;
		}
		v++;
		if (oppY < 50) {
			new_features[v] = 1;
		}
		v++;
		double distanceFromEnemy = Math.abs(Math.abs(myX) - Math.abs(oppX));
		double distanceYFromEnemy = Math.abs(Math.abs(myY) - Math.abs(oppY));
		if (charType.contains("ZEN")) {
			// Grab - 3A - 2A -5A -5B - 2B
			if (distanceFromEnemy <= 64) {
				new_features[v] = 1;

			}
			v++;
			// 5A - 2A
			if (distanceFromEnemy > 64 && distanceFromEnemy <= 84) {
				new_features[v] = 1;

			}
			v++;

			// 5B - 2B - 6B
			if (distanceFromEnemy > 84 && distanceFromEnemy <= 100) {
				new_features[v] = 1;

			}
			v++;

			// 6B
			if (distanceFromEnemy > 100 && distanceFromEnemy <= 190) {
				new_features[v] = 1;

			}
			v++;
			// Safe from pyshical atks
			if (distanceFromEnemy > 190 && distanceFromEnemy < 310) {
				new_features[v] = 1;

			}
			v++;

			if (distanceFromEnemy > 310) {
				new_features[v] = 1;

			}
			v++;
		} else if (charType.contains("LUD")) {
			// Grab - 3A - 2A -5A -5B - 2B
			if (distanceFromEnemy > 80 && distanceFromEnemy <= 95) {
				new_features[v] = 1;

			}
			v++;
			// 5A - 2A
			if (distanceFromEnemy > 95 && distanceFromEnemy <= 115) {
				new_features[v] = 1;

			}
			v++;

			// 6B
			if (distanceFromEnemy > 115 && distanceFromEnemy <= 150) {
				new_features[v] = 1;

			}
			v++;
			// Safe from pyshical atks
			if (distanceFromEnemy > 150 && distanceFromEnemy <= 360) {
				new_features[v] = 1;

			}
			v++;

			if (distanceFromEnemy > 360) {
				new_features[v] = 1;

			}
			v++;
		}

		// End - Nocoes de posicao no plano 2d

		// Begin - Nocoes sobre pulos

		// pulos subindo

		new_features = pulos(new_features, myX, myY, myLastX, myLastY, myChar_isFront, v);
		v += 10;// qtd de feat em pulos
		new_features = pulos(new_features, oppX, oppY, opLastX, opLastY, opp_isFront, v);
		v += 10;
		// End - Nocoes sobre pulos

		// Begin - Nocoes sobre acoes do oponente
		for (AttackData a : atks) {
			// System.out.println("PROJECTILES: "+a.isPlayerNumber()+"
			// "+projetile_number);
			if (a.isPlayerNumber()) {
				my_projetile_number++;
			} else {
				op_projetile_number++;
			}
			HitArea ha = a.getCurrentHitArea();

			if (myChar_isFront) {
				if ((ha.getLeft() < (myX + 100)) && (ha.getLeft() > (myX))) {
					closeProjetile = true;
				}
			} else {
				if ((ha.getLeft() > (myX - 100)) && (ha.getLeft() < (myX))) {
					closeProjetile = true;
				}
			}
		}

		if (closeProjetile) {
			new_features[v] = 1;

		}
		v++;

		if (my_projetile_number > 1) {
			new_features[v] = 1;

		}
		v++;

		if (op_projetile_number > 1) {
			new_features[v] = 1;

		}
		v++;

		// End - Nocoes sobre acoes do oponente

		myLastX = myX;
		myLastY = myY;
		opLastX = oppX;
		opLastY = oppY;

		return new_features;
	}


	public void pulosDebug(double mX, double mY, double mLx, double mLy, boolean myChar_isFront, int v,
			double[] weights, boolean debug_weigths) {
		if (mY - mLy > 0) {
			// System.out.println("falling" + mY + mLy);
			// cima
			if (mX - mLx == 0) {
				System.out.println("fall");
				if (debug_weigths)
					System.out.println("Weight: " + weights[v]);
			}
			v++;

			// pra frente
			if (myChar_isFront) {
				if (mX - mLx > 0) {
					System.out.println("front fall");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			if (!myChar_isFront) {
				if (mX - mLx < 0) {
					System.out.println("front fall");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			v++;
			// pra tras
			if (myChar_isFront) {
				if (mX - mLx < 0) {
					System.out.println("back fall");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			if (!myChar_isFront) {
				if (mX - mLx > 0) {
					System.out.println("back fall");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			v++;
			v += 5;
			// pulo
		} else if (mY - mLy < 0) {
			v += 5;
			// System.out.println("jumping");
			// para cima
			if (mX - mLx == 0) {
				System.out.println("jump");
				if (debug_weigths)
					System.out.println("Weight: " + weights[v]);
			}
			v++;

			// pra frente
			if (myChar_isFront) {
				if (mX - mLx > 0) {
					System.out.println("front jump");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			if (!myChar_isFront) {
				if (mX - mLx < 0) {
					System.out.println("front jump");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			v++;
			// pra tras
			if (myChar_isFront) {
				if (mX - mLx < 0) {
					System.out.println("back jump");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			if (!myChar_isFront) {
				if (mX - mLx > 0) {
					System.out.println("back jump");
					if (debug_weigths)
						System.out.println("Weight: " + weights[v]);
				}
			}
			v++;
		}
	}

	public double[] pulos(double[] new_features, double mX, double mY, double mLx, double mLy, boolean myChar_isFront,
			int v) {
		if (mY - mLy > 0) {
			// System.out.println("falling" + mY + mLy);
			// cima
			if (mX - mLx == 0) {
				new_features[v] = 1;
			}
			v++;

			// pra frente
			if (myChar_isFront) {
				if (mX - mLx > 0) {
					new_features[v] = 1;
				}
			}
			v++;
			if (!myChar_isFront) {
				if (mX - mLx < 0) {
					new_features[v] = 1;
				}
			}
			v++;
			// pra tras
			if (myChar_isFront) {
				if (mX - mLx < 0) {
					new_features[v] = 1;
				}
			}
			v++;
			if (!myChar_isFront) {
				if (mX - mLx > 0) {
					new_features[v] = 1;
				}
			}
			v++;

			v += 5;// pular as features do mY-mLy <0 que nao ocorrem
			// pulo
		} else if (mY - mLy < 0) {
			v += 5;// pular as features do mY-mLy >0 que nao ocorrem
			// System.out.println("jumping");
			// para cima
			if (mX - mLx == 0) {
				new_features[v] = 1;
			}
			v++;

			// pra frente
			if (myChar_isFront) {
				if (mX - mLx > 0) {
					new_features[v] = 1;
				}
			}
			v++;
			if (!myChar_isFront) {
				if (mX - mLx < 0) {
					new_features[v] = 1;
				}
			}
			v++;
			// pra tras
			if (myChar_isFront) {
				if (mX - mLx < 0) {
					new_features[v] = 1;
				}
			}
			v++;
			if (!myChar_isFront) {
				if (mX - mLx > 0) {
					new_features[v] = 1;
				}
			}
			v++;
		}
		return new_features;
	}

	// URGENTE:(Feito)
	// Lembrar que esta setado pro motion do p1, entao a ia tem q ser o player
	// 1!
	// Lembrar de variar pros 2 players, pois no torneio pode ser qualquer um
	// dos dois!!!
	/**
	 * Coloca as possiveis acoes no myActions na ordem: 0-14 acoes no Ar. 15-39
	 * acoes em Stand
	 */
	public void setPossibleActions(FrameData frames) {
		myActions.clear();
		myIndexActions.clear();
		int energy = this.myChar.getEnergy();
		ArrayList<MotionData> VM = gd.getMotionData(!p);

		myChar = frames.getCharacter(player);
		if (this.myChar.getState() == State.AIR) { // no ar
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(VM.get(ActionBehaviours.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionAir[i]);
					myIndexActions.add(i);
				}
			}
			for (int i = 0; i < behaviourAir.length; i++) {
				myActions.add(behaviourAir[i]);
				myIndexActions.add(i + actionAir.length);
			}

		} else {
			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(VM.get(ActionBehaviours.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionGround[i]);
					myIndexActions.add(i + actionAir.length + behaviourAir.length);
				}
			}
	
			if (charType.equals("LUD")) {
				for (int i = 0; i < behaviourGroundOpGround.length; i++) {
					if (energy >= 2 && frames.getDistanceX() < 200) {
						myActions.add(behaviourGroundOpGround[i]);
						myIndexActions.add(i + actionAir.length + behaviourAir.length + actionGround.length
								+ behaviourGroundOpAir.length);
					}
				}
			} else {
				for (int i = 0; i < behaviourGroundOpGround.length; i++) {
					myActions.add(behaviourGroundOpGround[i]);
					myIndexActions.add(i + actionAir.length + behaviourAir.length + actionGround.length
							+ behaviourGroundOpAir.length);
				}
			}
		}

		// Behaviours globais, que funcionam independente da nossa posicao e do
		// oponente
		if (charType.equals("LUD")) {

			for (int i = 0; i < behaviourGlobal.length; i++) {
				ActionBehaviours ab = behaviourGlobal[i];
				if ((ab.name().equals(ActionBehaviours.BEHV_FOR_STOMPER.name())
						|| ab.name().equals(ActionBehaviours.BEHV_UP_STOMPER.name())) && energy < 2) {
					// dont put it
				} else {
					myActions.add(ab);
					myIndexActions.add(i + actionAir.length + behaviourAir.length + actionGround.length
							+ behaviourGroundOpAir.length + behaviourGroundOpGround.length);
				}
			}
		} else {// ZEN
			for (int i = 0; i < behaviourGlobal.length; i++) {
				myActions.add(behaviourGlobal[i]);
				myIndexActions.add(i + actionAir.length + behaviourAir.length + actionGround.length
						+ behaviourGroundOpAir.length + behaviourGroundOpGround.length);

			}
		}
	}

	public void setOppPossibleActions() {
		oppActions.clear();
		oppIndexActions.clear();
		int energy = this.opp.getEnergy();
		ArrayList<MotionData> VM = gd.getMotionData(!p);

		if (this.opp.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(VM.get(ActionBehaviours.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionAir[i]);
					oppIndexActions.add(i);
				}
			}
		} else {

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(VM.get(ActionBehaviours.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionGround[i]);
					oppIndexActions.add(i + 15);
				}
			}
			if (Math.abs(
					VM.get(ActionBehaviours.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
				oppActions.add(spSkill);
				oppIndexActions.add(39);
			}
		}
	}
}
