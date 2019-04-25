import java.io.Serializable;
import java.util.ArrayList;

public class Stats implements Serializable {
	public double e;
	public int rounds;
	public ArrayList arr = new ArrayList();
	public int score_counter = 0;
	public double score_accumuler = 0;
	public Stats(double e, int rounds) {
		this.e = e;
		this.rounds = rounds;
	}
	public void setScore(double score){
		score_counter++;
		score_accumuler += score;
		if(score_counter%10 == 0){
			arr.add(score_accumuler/10);
			System.out.println("Score(10 matches: "+score_accumuler/10);
			score_accumuler = 0;
		}
	}
	
}
