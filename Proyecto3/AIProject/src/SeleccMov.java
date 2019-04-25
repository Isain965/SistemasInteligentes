import java.util.Comparator;

public class SeleccMov implements Comparator<Movimiento>{

	@Override
	public int compare(Movimiento opB, Movimiento opA) {

		return (int) (((opA.distanciaX / opA.distanciaY) / (opB.distanciaX / opB.distanciaY)) * 0.001f);
	}		
}