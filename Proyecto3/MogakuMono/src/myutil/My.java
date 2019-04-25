package myutil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import enumerate.Action;
import enumerate.State;
import struct.MotionData;

enum GameState {
	AIR, Ground, 
}

public class My {

	public static final boolean[] isAirtable = new boolean[] { true, false, false, false, false };
	
	public static final Action[] actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA,
			Action.AIR_DB, Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA,
			Action.AIR_D_DF_FB, Action.AIR_F_D_DFA, Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
	public static final Action[] actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP,
			Action.FORWARD_WALK, Action.DASH, Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD,
			Action.CROUCH_GUARD, Action.THROW_A, Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A,
			Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA,
			Action.STAND_D_DF_FB, Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB,
			Action.STAND_D_DF_FC };

	public static final Action[] actionAirNonEnergy = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B,
			Action.AIR_DB, Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA, };
	public static final Action[] actionGroundNonEnergy = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP,
			Action.FORWARD_WALK, Action.DASH, Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD,
			Action.CROUCH_GUARD, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
			Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_F_D_DFA, Action.STAND_D_DB_BB };

	/**** ZEN ***/

	public static final Action[] ZenFinalActionAir = // 20 3.3759618E7
			new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DB, Action.AIR_FA, Action.AIR_FB,
					Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA, Action.AIR_DA, Action.AIR_F_D_DFB };
	public static final Action[] ZenFinalActionGround = new Action[] { Action.BACK_STEP, Action.DASH, Action.JUMP,
			Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.STAND_D_DB_BA,
			Action.CROUCH_A, Action.CROUCH_B, Action.FORWARD_WALK, Action.THROW_A, Action.CROUCH_FB,
			Action.STAND_F_D_DFA, Action.STAND_D_DB_BB, };

	public static final Action[] ZenFinalActionHame = new Action[] { Action.FORWARD_WALK, Action.STAND,
			Action.CROUCH_FB, Action.STAND_B, Action.STAND_D_DF_FC };

	public static final Action[] ZenFinalActionNear = new Action[] { Action.FORWARD_WALK, Action.DASH,
			Action.STAND_D_DB_BB, Action.STAND_B, Action.STAND_D_DF_FC };

	public static final Action[] ZenFinalActionFar = new Action[] { Action.FORWARD_WALK, Action.DASH,
			Action.STAND_D_DB_BB, Action.FOR_JUMP, Action.STAND_D_DF_FC };

	public static final Action[] ZenMustActionAir = new Action[] {};

	public static final Action[] ZenMustActionGround = new Action[] { Action.DASH, Action.FOR_JUMP,
			Action.STAND_D_DB_BA, Action.CROUCH_FB, Action.STAND_D_DF_FC };

	public static final Action[] ZenMustActionHame = new Action[] { Action.STAND_D_DB_BA, Action.CROUCH_FB,
			Action.STAND_D_DF_FC };
	public static final Action[] ZenMustActionFar = new Action[] { Action.FOR_JUMP, Action.STAND_D_DB_BA,
			Action.STAND_D_DF_FC };


	public static void ls(String path) {
		File dir = new File(path);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			System.out.println(file.getName());
		}
	}

	static Action[] getGaAction(Action[] MustAction, boolean isAir) {
		Action[] allAction;
		if (isAir)
			allAction = actionAir;
		else
			allAction = actionGround;
		ArrayList<Action> ac = new ArrayList<Action>(Arrays.asList(allAction.clone()));
		for (int i = ac.size() - 1; i >= 0; i--) {

			for (int j = 0; j < MustAction.length; j++) {
				if (MustAction[j] == ac.get(i)) {
					ac.remove(i);
				}
			}

		}

		return ac.toArray(new Action[0]);
	}

	static ArrayList<Action> getUseAction(Vector<MotionData> motions, Action[] allaction) {
		ArrayList<Action> ac = new ArrayList<Action>();
		for (int i = 0; i < allaction.length; i++) {
			MotionData motion = motions.elementAt(Action.valueOf(allaction[i].name()).ordinal());
			if (motion.getAttackStartAddEnergy() >= 0 ||
					(motion.isAttackDownProp() && motion.speedX > 0) ||
					((motion.attackSpeedX != 0) && (motion.state == State.STAND)) ||
					(motion.attackType == 4)// æŠ•ã?’
			)
				ac.add(allaction[i]);

		}
		return ac;
	}

}
