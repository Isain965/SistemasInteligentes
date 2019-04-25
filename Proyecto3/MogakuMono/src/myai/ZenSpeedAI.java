package myai;

/**
 * Zenã�®ã‚¹ãƒ�?ãƒ¼ãƒ‰å¯¾æˆ¦ç�?¨AI
 * ã�¨ã�«ã�‹ã��å‰�é€²ã�‚ã‚‹ã�®ã�¿
 * @author Eita Aoki
 */
import java.util.LinkedList;

import enumerate.Action;
import enumerate.State;
import myutil.Calclator;
import myutil.My;
import struct.GameData;

public class ZenSpeedAI extends MyAI {

	public ZenSpeedAI(GameData arg0, boolean player) {
		super(arg0, player);

		System.out.println("ZenSpeedAI");

	}

	// ä½¿ã�„ã�Ÿã�„Actionã�®å…¨é‡�
	void setRootActions() {
		this.actionGround = My.actionGround;
		this.actionAir = My.actionAir;

	}

	void setCalclator() {
		if (this.preAct == Action.FORWARD_WALK) {
			calc = new Calclator(simFrameData, gd, player, Action.FORWARD_WALK);
		} else {
			calc = new Calclator(simFrameData, gd, player, Calclator.NONACT);
		}
	}

	@Override
	public Action getPrepareAction() {

		Action tac = null;
		if (bestac != null) {
			if (my.getState() != State.AIR) {

				LinkedList<Action> moveActs = calc.getEnoughEnergyActions(false, Action.NEUTRAL, Action.STAND_D_DF_FA,
						Action.STAND_D_DF_FB);

				// if(calc.getMinHpScore(tac=Action.STAND_B,moveActs)>0){
				// return(tac);
				// }
				//
				// else
				// if(calc.getMinHpScore(tac=Action.STAND_D_DF_FC,moveActs)>0){
				// return(tac);
				// }
				// else
				// if(calc.getMinHpScore(tac=Action.STAND_D_DB_BB,moveActs)>0){
				// return(tac);
				// }
				// else
				// if(calc.getMinHpScore(tac=Action.STAND_F_D_DFA,moveActs)>0){
				// return(tac);
				// }
				if (calc.getHpScore(tac = Action.STAND_D_DF_FC) > 0) {// è¶…å¿…æ®º
					return (tac);
				}

				if (calc.getHpScore(tac = Action.CROUCH_B) > 0 || distance < 100) {// ä¸‹ã�’ã‚Š
																					// 10
					return (tac);

				}
				if (calc.getHpScore(tac = Action.STAND_D_DF_FB) > 0) {// speedæ³¢å‹•æ‹³
																		// 30
					return (tac);

				}
				if (calc.getHpScore(tac = Action.STAND_B) > 0) {// 10
					return (tac);

				}

				if (calc.getHpScore(tac = Action.STAND_D_DB_BB) > 0) {// 25
																		// stand
					return (tac);
				}

				if (calc.getHpScore(tac = Action.STAND_FB) > 0) {// å‰�ã�’ã‚Š 12
					return (tac);

				}
				if (calc.getHpScore(tac = Action.STAND_F_D_DFA) > 0) {// ã‚¢ãƒƒãƒ‘ãƒ¼ 10
					return (tac);
				}

				// if(distance>150){//10
				// return(null);
				//
				// }

				// if(distance>50)if(calc.getHpScore(tac=Action.STAND_D_DB_BA)>0){//çŒ¿è·³ã�³
				// 10
				// return(tac);
				//
				// }
				// if(distance<50){//10
				// return(Action.CROUCH_B);
				//
				// }
				return Action.FORWARD_WALK;
				// return(bestac);
				// return mctsprocessing(simFrameData);

			} else {
				if (calc.getHpScore(tac = Action.AIR_DB) > 0) {
					return (tac);

				}

				return Action.AIR_B;
				// return(bestac);
				// return mctsprocessing(simFrameData);

			}
		} else {

			if (my.getState() != State.AIR) {
				return Action.FORWARD_WALK;

			} else {
				return (Action.AIR_B);
			}
		}

	}

}
