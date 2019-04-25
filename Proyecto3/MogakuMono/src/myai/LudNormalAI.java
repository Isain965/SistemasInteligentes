package myai;

import java.util.LinkedList;

import enumerate.Action;
import struct.GameData;

public class LudNormalAI extends MyAI {

	public LudNormalAI(GameData arg0, boolean player) {
		super(arg0, player);
		System.out.println("LudNormalAI");

	}

	@Override
	Action getPrepareAction() {

		if (HittingMyActions.size() > 0)
			return this.mctsprocessing(simFrameData, HittingMyActions);
		else {
			if (my.getHp() - opp.getHp() > 100) {
				LinkedList<Action> moveActs = calc.getEnoughEnergyActions(true, Action.BACK_STEP, Action.JUMP,
						Action.FOR_JUMP, Action.FORWARD_WALK, Action.BACK_JUMP);

				return (calc.getMinMaxIfHadouken(moveActs));
			} else {
				LinkedList<Action> moveActs = (distance < 300)
						? calc.getEnoughEnergyActions(true, Action.FOR_JUMP, Action.FORWARD_WALK, Action.JUMP,
								Action.BACK_JUMP, Action.BACK_STEP)
						: calc.getEnoughEnergyActions(true, Action.FORWARD_WALK, Action.FOR_JUMP, Action.JUMP,
								Action.BACK_JUMP, Action.BACK_STEP);

				return (calc.getMinMaxIfHadouken(moveActs));
			}
		}

	}

}
