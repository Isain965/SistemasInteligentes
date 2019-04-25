package myutil;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import enumerate.Action;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.MotionData;

public class Calclator {
	public final static int SIMULATE_LIMIT = 60;
	public final static Action NONACT = Action.NEUTRAL;
	public FrameData nonActionFrame;
	FrameData motoFrame;
	HashMap<List, FrameData> map;
	GameData gd;
	Simulator simlator;
	boolean player;
	private ArrayList<MotionData> myMotion;
	private ArrayList<MotionData> oppMotion;
	LinkedList<Action> hadoukenActsAir;
	LinkedList<Action> hadoukenActsGround;

	public Calclator(FrameData motoFrame, GameData gd, boolean player, Action preAct) {
		this.motoFrame = motoFrame;
		this.gd = gd;
		this.simlator = gd.getSimulator();
		this.player = player;
		this.myMotion = gd.getMotionData(player);
		this.oppMotion = gd.getMotionData(!player);

		this.hadoukenActsAir = new LinkedList<Action>();
		this.hadoukenActsGround = new LinkedList<Action>();
		for (Action ac : My.actionAir) {
			MotionData mo = myMotion.get(ac.ordinal());
			if (mo.getAttackSpeedX() != 0 || mo.getAttackSpeedY() != 0)
				hadoukenActsAir.add(ac);
		}
		for (Action ac : My.actionGround) {
			MotionData mo = myMotion.get(ac.ordinal());
			if (mo.getAttackSpeedX() != 0 || mo.getAttackSpeedY() != 0)
				hadoukenActsGround.add(ac);
		}

		map = new HashMap<List, FrameData>();
		this.nonActionFrame = getFrame(preAct, NONACT);

	}

	// player(true:å‘³æ–¹,false:æ•µ)ã?Œactã‚’ã?™ã‚‹ã‚¨ãƒ?ãƒ«ã‚®ãƒ¼ã‚’æŒ?ã?£ã?¦ã?„ã‚‹ã?‹
	public boolean isEnoughEnergy(Action act, boolean player) {
		ArrayList<MotionData> mos = (player ? myMotion : oppMotion);
		CharacterData ch = (player ? this.motoFrame.getCharacter(this.player)
				: this.motoFrame.getCharacter(this.player));
		return (mos.get(act.ordinal()).getAttackStartAddEnergy() + ch.getEnergy() >= 0);

	}

	// player(true:å‘³æ–¹,false:æ•µ)ã?Œä»Šã?®ã‚¨ãƒ?ãƒ«ã‚®ãƒ¼ã?§æ’ƒã?¦ã‚‹actsã‚’åˆ—æŒ™
	public LinkedList<Action> getEnoughEnergyActions(boolean player, Action... acts) {
		LinkedList<Action> moveActs = new LinkedList<Action>();
		for (Action tac : acts) {
			if (isEnoughEnergy(tac, player))
				moveActs.add(tac);
		}

		return moveActs;
	}

	// å‘³æ–¹ã?Œmyactã€?æ•µã?Œopactã‚’ã?—ã?Ÿå ´å?ˆã?®SIMULATE_LIMITã?®ç›¤é?¢ã‚’è¨ˆç®—ã?™ã‚‹ã€‚
	// ä¸€åº¦ã?§ã‚‚å‘¼ã?³å‡ºã?—ã?Ÿã?“ã?¨ã?Œã?‚ã‚Œã?°mapã?«æ ¼ç´?ã?—ã?¦ã?Šã??ã?®ã?§ã€?äºŒåº¦ç›®ã?¯å®‰å¿ƒã?—ã?¦å‘¼ã?³å‡ºã?›ã‚‹
	public FrameData getFrame(Action myact, Action opact) {
		Action tmyact, topact;
		if (isEnoughEnergy(myact, true)) {
			tmyact = myact;
		} else {
			tmyact = NONACT;
		}
		if (isEnoughEnergy(opact, false)) {
			topact = opact;
		} else {
			topact = NONACT;
		}
		List<Action> key = new ArrayList<Action>();
		key.add(tmyact);
		key.add(topact);

		if (!map.containsKey(key)) {
			Deque<Action> mAction = new LinkedList<Action>();
			mAction.add(tmyact);
			Deque<Action> opAction = new LinkedList<Action>();
			opAction.add(topact);

			FrameData value = this.simlator.simulate(motoFrame, player, mAction, opAction, SIMULATE_LIMIT);
			map.put(key, value);
		}

		return map.get(key);
	}

	public FrameData getMyFrame(Action myact) {
		return getFrame(myact, NONACT);
	}

	public double getHpScore(Action myact) {
		return getHpScore(myact, NONACT);
	}

	public double getHpScore(Action myact, Action opact) {
		FrameData fd = getFrame(myact, opact);
		double gapMyHp = fd.getCharacter(player).getHp() - nonActionFrame.getCharacter(player).getHp();
		double gapOpHp = fd.getCharacter(!player).getHp() - nonActionFrame.getCharacter(!player).getHp();

		return gapMyHp - gapOpHp;
	}

	// æ•µã?Œæ³¢å‹•æ‹³æ‰“ã?£ã?¦ã??ã?Ÿã?¨ã??ã?«myactã?—ã?Ÿå ´å?ˆã?®æœ€å°?Hpã‚¹ã‚³ã‚¢
	public double getMinHpScoreIfHadouken(Action myact) {
		double min = 9999;
		for (Action opact : this.hadoukenActsGround) {
			double score = getHpScore(myact, opact);
			if (score < min)
				min = score;
		}
		return min;
	}

	// æ•µã?Œæ³¢å‹•æ‹³æ‰“ã?£ã?¦ã??ã?Ÿã?¨ã??ã?«æœ€å°?Hpã‚¹ã‚³ã‚¢ã?Œæœ€å¤§ã?«ã?ªã‚‹ã‚¢ã‚¯ã‚·ãƒ§ãƒ³
	public Action getMinMaxIfHadouken(List<Action> acs) {
		double max = -9999;
		Action maxact = Action.FORWARD_WALK;
		for (Action myact : acs) {
			double score = getMinHpScoreIfHadouken(myact);
			if (score > max) {
				max = score;
				maxact = myact;
			}
		}
		return maxact;
	}

	// è‡ªåˆ†ã?Œmyactã‚’ã€?æ•µã?®å‹•ã??ã?ŒopAcsã?®ã?„ã?šã‚Œã?‹ã‚’ã?—ã?¦ã??ã?Ÿå ´å?ˆã?®è‡ªåˆ†ã?®æœ€å°?Hpã‚¹ã‚³ã‚¢
	public double getMinHpScore(Action myact, List<Action> opAcs) {

		double min = 9999;

		for (Action opact : opAcs) {
			double score = getHpScore(myact, opact);
			if (score < min) {
				min = score;
			}
		}

		return min;
	}

	// HPå·®ã‚’ã‚¹ã‚³ã‚¢ã?¨ã?—ã?¦ã‚¢ãƒ«ãƒ•ã‚¡ãƒ™ãƒ¼ã‚¿ã?™ã‚‹ã€‚æŽ¢ç´¢ã‚¢ã‚¯ã‚·ãƒ§ãƒ³æ•°ã?«ã‚ˆã?£ã?¦ã?¯æ™‚é–“åˆ¶é™?ã?§é€”ä¸­åˆ‡ã‚Šã?—ã?Ÿæ–¹ã?Œã‚ˆã?•ã?’
	public Action getMinMaxHp(List<Action> myAcs, List<Action> opAcs) {

		double alpha = -9999;
		Action maxact = Action.FORWARD_WALK;
		for (Action myact : myAcs) {
			double min = 9999;

			for (Action opact : opAcs) {
				double score = getHpScore(myact, opact);
				if (score < min) {
					min = score;
					if (min < alpha)
						break;
				}
			}
			if (min > alpha) {
				alpha = min;
				maxact = myact;
			}
		}
		return maxact;
	}

}
