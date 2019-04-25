import aiinterface.CommandCenter;


import java.io.IOException;
import java.util.ArrayList;

import aiinterface.AIInterface;
import struct.*;

public class AIProject implements AIInterface {

	Key inputKey;
	boolean player;
	FrameData frmdata;
	CommandCenter cc;
	
	ArrayList<MotionData> myMotion;

	ArrayList<MotionData> oppMotion;
	
	BFS bfs;
	
	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void getInformation(FrameData frmdata) {
		this.frmdata = frmdata;
		 cc.setFrameData(this.frmdata, player);

	}

	@Override
	public int initialize(GameData gameData,boolean player) {
		this.player = player;
		 this.inputKey = new Key();
		 try {
			setupBFS();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 cc = new CommandCenter();
		 frmdata = new FrameData();
		 
		 myMotion = gameData.getMotionData(this.player) ;
		 oppMotion = gameData.getMotionData(!this.player) ;
			
		 return 0;
	}
	
	void setupBFS() throws IOException {
		this.bfs = new BFS();
	}

	@Override
	public Key input() {
		return inputKey;
	}

	@Override
	public void processing() {
		if(!frmdata.getEmptyFlag() && frmdata.getRemainingFramesNumber()> 0) {
			
			float distanciaX = frmdata.getDistanceX();
			float distanciaY = frmdata.getDistanceY();

			if(cc.getSkillFlag()) {
				
				inputKey = cc.getSkillKey();
				
			} else {
				
				inputKey.empty();
				cc.skillCancel();
			
				if(distanciaX > 100) {
					cc.commandCall(bfs.getMovimiento());
				}
				
				if (distanciaX < 100) {
					cc.commandCall(bfs.getGolpe());
					cc.commandCall(bfs.getMovimiento());
				}
				if (distanciaY < 100) {
					cc.commandCall(bfs.getGolpe());
					cc.commandCall(bfs.getMovimiento());
				}
				
				if (distanciaY > 100) {
					cc.commandCall(bfs.getMovimiento());
				}

				cc.commandCall(bfs.getMovimiento());
				cc.commandCall(bfs.getGolpe());
			}
		}
		

	}

	@Override
	public void roundEnd(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

}
