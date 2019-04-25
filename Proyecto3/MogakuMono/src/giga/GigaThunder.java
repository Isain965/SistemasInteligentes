package giga;


import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import enumerate.Action;
import myai.LudNormalAI;
import myai.LudSpeedAI;
import myai.MyAI;
import myai.ZenNormalAI;
import myai.ZenSpeedAI;
import myenumurate.CharaName;
import myenumurate.GameMode;
import struct.FrameData;
import struct.GameData;
import struct.Key;

public class GigaThunder implements AIInterface {

	private Key inputKey;
	private boolean player;
	private FrameData frameData;
	private CommandCenter cc;
	private GameData gd;

	private boolean firstflag = true;
	private GameMode gamemode;
	private CharaName charname;

	MyAI ai;

	@Override
	public void close() {
		// Nothing to do here
	}

	public void firstgetInformation() {
		System.out.println("call firstgetInformation!");
		CommandCenter c = new CommandCenter();
		c.setFrameData(frameData, player);

		int myhp = frameData.getCharacter(player).getHp();
		int ophp = frameData.getCharacter(!player).getHp();
		if (myhp < 1000 && ophp < 1000)
			this.gamemode = GameMode.NORMAL;
		else if (myhp > 1000 && ophp < 1000)
			this.gamemode = GameMode.SPEED;
		else
			this.gamemode = GameMode.LUDTRAIN;

		if (this.charname == CharaName.ZEN && this.gamemode == GameMode.NORMAL)
			ai = new ZenNormalAI(gd, player);
		else if (this.charname == CharaName.ZEN && this.gamemode == GameMode.SPEED)
			ai = new ZenSpeedAI(gd, player);
		else if (this.charname == CharaName.OTHER && this.gamemode == GameMode.NORMAL)
			ai = new LudNormalAI(gd, player);
		else
			ai = new LudSpeedAI(gd, player);
	}

	@Override
	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		if (frameData.getFramesNumber() >= 0) {
			if (this.firstflag) {
				this.firstflag = false;
				firstgetInformation();
			} // ãƒ¢ãƒ¼ãƒ‰åˆ¤å®šã�—ã�Ÿã‚Šã€�åˆ¤å®šå¾Œã�®ãƒ•ãƒ©ã‚°è¨­å®šã‚’ã�™ã‚‹

		}
		if (ai != null) {
			ai.getInformation(frameData);
		}

	}

	@Override
	public int initialize(GameData arg0, boolean player) {
		System.out.println("test 0603");
		// Initialize the global variables at the start of the round
		inputKey = new Key();
		this.player = player;
		frameData = new FrameData();
		cc = new CommandCenter();
		gd = arg0;

		String tmpcharname = this.gd.getCharacterName(this.player);
		if (tmpcharname.equals("ZEN"))
			charname = CharaName.ZEN;
		else
			charname = CharaName.OTHER;

		return 0;
	}

	@Override
	public Key input() {
		// The input is set up to the global variable inputKey
		// which is modified in the processing part
		return inputKey;
	}

	public final static int SIMULATE_LIMIT = 60;

	long start;
	long maxtime;

	@Override
	public void processing() {
		// First we check whether we are at the end of the round
		start = System.nanoTime();

		if (!frameData.getEmptyFlag() && frameData.getRemainingTime() > 0) {
			// System.out.println("frame:"+frameData.getFrameNumber());
			// Simulate the delay and look ahead 2 frames. The simulator class
			// exists already in FightingICE

			if (cc.getSkillFlag()) {
				// If there is a previous "command" still in execution, then
				// keep doing it
				inputKey = cc.getSkillKey();
			} else {

				inputKey.empty();
				cc.setFrameData(frameData, player);
				cc.skillCancel();
				Action ac = ai.getDoAction();
				String commandstr = (ac.name());
				System.out.println("my HP " + frameData.getCharacter(player).getHp() + " opp HP " + frameData.getCharacter(!player).getHp() + " dis " + frameData.getDistanceX() + " act " + commandstr);
				cc.commandCall(commandstr);
			}
			if (frameData.getFramesNumber() >= 0) {
				long ttime = System.nanoTime() - start;
				if (ttime > maxtime)
					maxtime = ttime;
				if (frameData.getFramesNumber() % 10 == 0) {
					double timerate = (double) maxtime / (double) (165 * 100000);
					maxtime = 0;

				}
			}

		}

	}

	@Override
	public void roundEnd(int arg0, int arg1, int arg2) {
		inputKey.empty();
		cc.skillCancel();
	}

}
