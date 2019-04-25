package myai;

/**
 * Zenã?®é€šå¸¸å¯¾æˆ¦ç”¨AI
 * ã‚¹ãƒ©ã‚¤ãƒ‡ã‚£ãƒ³ã‚°ã?§æ•µã‚’è§’ã?«ã?Šã?„ã‚„ã‚Šã€?ä¸‹ã?’ã‚Šã?§ãƒ?ãƒ¡ã‚‹
 * @author Eita Aoki
 */
import java.util.LinkedList;

import enumerate.Action;
import enumerate.State;
import myutil.My;
import struct.GameData;

public class ZenNormalAI extends MyAI {

	public ZenNormalAI(GameData arg0, boolean player) {
		super(arg0, player);
		System.out.println("ZenNormalAI");

	}

	// ä½¿ã?„ã?Ÿã?„Actionã?®å…¨é‡?
	void setRootActions() {
		this.actionGround = My.actionGroundNonEnergy;
		this.actionAir = My.actionAirNonEnergy;

	}

	@Override
	public Action getPrepareAction() {

		Action tac = null;
		if (bestac != null) {
			if (my.getState() != State.AIR) {
				LinkedList<Action> moveActs = calc.getEnoughEnergyActions(false, Action.NEUTRAL, Action.STAND_D_DF_FA,
						Action.STAND_D_DF_FB);

				if (calc.getMinHpScore(tac = Action.STAND_B, moveActs) > 0) {
					return (tac);
				} else if (calc.getMinHpScore(tac = Action.CROUCH_FB, moveActs) > 0) {
					return (tac);
				} else if (calc.getMinHpScore(tac = Action.STAND_D_DF_FC, moveActs) > 0) {
					return (tac);
				} else if (calc.getMinHpScore(tac = Action.STAND_D_DB_BB, moveActs) > 0) {
					return (tac);
				} else if (calc.getMinHpScore(tac = Action.STAND_F_D_DFA, moveActs) > 0) {
					return (tac);
				}
				// if(calc.getHpScore(tac=Action.STAND_B)>0){
				// return(tac);
				// }
				// else if(calc.getHpScore(tac=Action.CROUCH_FB)>0){
				// return(tac);
				// }
				// else if(calc.getHpScore(tac=Action.STAND_D_DF_FC)>0){
				// return(tac);
				// }
				// else if(calc.getHpScore(tac=Action.STAND_D_DB_BB)>0){
				// return(tac);
				// }
				// else if(calc.getHpScore(tac=Action.STAND_F_D_DFA)>0){
				// return(tac);
				// }
				else {

					return (bestac);
					// return mctsprocessing(simFrameData);

				}
			} else {

				return (bestac);
				// return mctsprocessing(simFrameData);

			}
		} else {

			if (my.getState() != State.AIR) {

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

			} else {
				return (Action.AIR_GUARD);
			}
		}

	}

}
