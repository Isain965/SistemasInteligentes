import java.util.Comparator;

public class SeleccGolpes implements Comparator<Golpe>{

	@Override
	public int compare(Golpe golpeA, Golpe golpeB) {
		// TODO Auto-generated method stub
		
		if (golpeA.danioGolpe > golpeB.danioGolpe) {
			return golpeA.danioGolpe;
		} else {
			return golpeB.danioGolpe;
		}
	}
	
}