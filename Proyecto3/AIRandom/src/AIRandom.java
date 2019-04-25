import aiinterface.CommandCenter;
import enumerate.State;
import aiinterface.AIInterface;
import struct.*;

public class AIRandom implements AIInterface {

	Key inputKey;
	boolean playernum;
	FrameData frmdata;
	CommandCenter cc;
	private CharacterData myCharacter;
	public void close() {

	}

	
	public void getInformation(FrameData frmdata) {
		 this.frmdata = frmdata;
		 cc.setFrameData(this.frmdata, playernum);
		 
		myCharacter = frmdata.getCharacter(playernum);
	}

	
	public int initialize(GameData gameData,boolean playernum) {
		 this.playernum = playernum;
		 this.inputKey = new Key();
		 cc = new CommandCenter();
		 frmdata = new FrameData();
		 return 0;
	}

	
	public Key input() {
		// returns Key
		return inputKey;
	}
	public void processing() {
		if(!frmdata.getEmptyFlag() && frmdata.getRemainingFramesNumber()> 0) {
			if(cc.getSkillFlag()) {
				inputKey = cc.getSkillKey();
			} else {
				inputKey.empty();
				cc.skillCancel();
				// create new variable "dist" , because "framedata.getDistanceX() cannot use in FTG version 4."
				float dist = Math.abs((frmdata.getCharacter(true).getLeft()+frmdata.getCharacter(true).getRight())/2
						- (frmdata.getCharacter(false).getLeft()+frmdata.getCharacter(false).getRight())/2);
				if(dist < 100) {
					if(myCharacter.getEnergy() >= 30) {
						cc.commandCall("STAND_D_DF_FB");	
					}else {
						cc.commandCall("CROUCH_FA");
						cc.commandCall("STAND_FA");						
					}
				} else if (dist > 100) {
					cc.commandCall("FORWARD_WALK");
				}
			}
		}
		
	}

	
	public void roundEnd(int p1Hp, int p2Hp, int frames){

	}
}