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

	// player(true:味方,false:敵)�?�actを�?�るエ�?ルギーを�?�?��?��?�る�?�
	public boolean isEnoughEnergy(Action act, boolean player) {
		ArrayList<MotionData> mos = (player ? myMotion : oppMotion);
		CharacterData ch = (player ? this.motoFrame.getCharacter(this.player)
				: this.motoFrame.getCharacter(this.player));
		return (mos.get(act.ordinal()).getAttackStartAddEnergy() + ch.getEnergy() >= 0);

	}

	// player(true:味方,false:敵)�?�今�?�エ�?ルギー�?�撃�?�るactsを列挙
	public LinkedList<Action> getEnoughEnergyActions(boolean player, Action... acts) {
		LinkedList<Action> moveActs = new LinkedList<Action>();
		for (Action tac : acts) {
			if (isEnoughEnergy(tac, player))
				moveActs.add(tac);
		}

		return moveActs;
	}

	// 味方�?�myact�?敵�?�opactを�?��?�場�?��?�SIMULATE_LIMIT�?�盤�?�を計算�?�る。
	// 一度�?�も呼�?�出�?��?��?��?��?��?�れ�?�map�?�格�?�?��?��?��??�?��?��?二度目�?�安心�?��?�呼�?�出�?�る
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

	// 敵�?�波動拳打�?��?��??�?��?��??�?�myact�?��?�場�?��?�最�?Hpスコア
	public double getMinHpScoreIfHadouken(Action myact) {
		double min = 9999;
		for (Action opact : this.hadoukenActsGround) {
			double score = getHpScore(myact, opact);
			if (score < min)
				min = score;
		}
		return min;
	}

	// 敵�?�波動拳打�?��?��??�?��?��??�?�最�?Hpスコア�?�最大�?��?�るアクション
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

	// 自分�?�myactを�?敵�?�動�??�?�opAcs�?��?��?�れ�?�を�?��?��??�?�場�?��?�自分�?�最�?Hpスコア
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

	// HP差をスコア�?��?��?�アルファベータ�?�る。探索アクション数�?�よ�?��?��?�時間制�?�?�途中切り�?��?�方�?�よ�?��?�
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
