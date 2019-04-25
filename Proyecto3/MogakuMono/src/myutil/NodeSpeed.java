package myutil;

import java.util.LinkedList;

import aiinterface.CommandCenter;
import enumerate.Action;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;

/**
 * MCTSã?§åˆ©ç”¨ã?™ã‚‹Node
 *
 * @author Taichi Miyazaki
 */
public class NodeSpeed extends Node {

	public NodeSpeed(long ucttime, FrameData frameData, NodeSpeed parent, LinkedList<Action> myActions,
			LinkedList<Action> oppActions, GameData gameData, boolean playerNumber, CommandCenter commandCenter,
			LinkedList<Action> selectedMyActions) {
		super(ucttime, frameData, parent, myActions, oppActions, gameData, playerNumber, commandCenter,
				selectedMyActions);
	}

	public NodeSpeed(long ucttime, FrameData frameData, NodeSpeed parent, LinkedList<Action> myActions,
			LinkedList<Action> oppActions, GameData gameData, boolean playerNumber, CommandCenter commandCenter) {
		super(ucttime, frameData, parent, myActions, oppActions, gameData, playerNumber, commandCenter);
	}

	@Override
	public int getScore(FrameData fd) {
		CharacterData mychar, opchar;
		mychar = fd.getCharacter(playerNumber);
		opchar = fd.getCharacter(!playerNumber);
		int mygap = mychar.getHp() - myOriginalHp;
		int opgap = opchar.getHp() - oppOriginalHp;

		return -opgap * 2 + mygap;
	}
}
