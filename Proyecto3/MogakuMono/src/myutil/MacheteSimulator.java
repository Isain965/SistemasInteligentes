package myutil;

import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;

public class MacheteSimulator {
	public static Action getAction(FrameData fd, GameData gd, boolean player) {
		Simulator simulator = gd.getSimulator();
		FrameData frameData = simulator.simulate(fd, player, null, null, 17);

		CommandCenter cc = new CommandCenter();
		cc.setFrameData(frameData, player);
		int distance = frameData.getDistanceX();
		int energy = frameData.getCharacter(player).getEnergy();
		CharacterData my = frameData.getCharacter(player);
		CharacterData opp = frameData.getCharacter(!player);
		int xDifference = my.getX() - opp.getX();

		// Following is the brain of the reflex agent. It determines distance to
		// the enemy
		// and the energy of our agent and then it performs an action
		if ((opp.getEnergy() >= 150) && ((my.getHp() - opp.getHp()) <= 120))
			return Action.FOR_JUMP;
		// if the opp has 300 of energy, it is dangerous, so better jump!!
		// if the health difference is high we are dominating so we are fearless
		// :)
		else if (!my.getState().equals(State.AIR) && !my.getState().equals(State.DOWN)) { // if
																							// not
																							// in
																							// air
			if ((distance > 150)) {
				return Action.FOR_JUMP; // If its too far, then jump to get
										// closer fast
			} else if (energy >= 150)
				return Action.STAND_D_DF_FC; // High energy projectile
			else if ((distance > 100) && (energy >= 50))
				return Action.STAND_D_DB_BB; // Perform a slide kick
			else if (opp.getState().equals(State.AIR)) // if enemy on Air
				return Action.STAND_F_D_DFA; // Perform a big punch
			else if (distance > 100)
				return Action.DASH;// Perform a quick dash to get closer
			else
				return Action.STAND_B; // Perform a kick in all other cases,
										// introduces randomness
		} else if ((distance <= 150) && (my.getState().equals(State.AIR) || my.getState().equals(State.DOWN))
				&& (((gd.getStageWidth() - my.getX()) >= 200) || (xDifference > 0))
				&& ((my.getX() >= 200) || xDifference < 0)) { // Conditions to
																// handle game
																// corners
			if (energy >= 5)
				return Action.AIR_DB; // Perform air down kick when in air
			else
				return Action.AIR_B; // Perform a kick in all other cases,
										// introduces randomness
		} else {
			if (my.getState() == State.AIR)
				return Action.AIR_B;
			else
				return Action.STAND_B; // Perform a kick in all other cases,
										// introduces randomness
		}

	}
}
