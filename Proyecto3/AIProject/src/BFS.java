import java.io.IOException;
import java.util.PriorityQueue;

public class BFS {
	
	ListaGolpe listaGolpe;
	ListaMov listaMov;
	
	PriorityQueue<Movimiento> pqMovimiento;
	PriorityQueue<Golpe> pqGolpe;
	
	SeleccGolpes comparadorGolpe;
	SeleccMov comparadorMovimiento;
	
	BFS() throws IOException {
		this.listaGolpe = new ListaGolpe();
		this.listaMov = new ListaMov();
		
		this.comparadorGolpe = new SeleccGolpes();
		this.comparadorMovimiento = new SeleccMov();
		
		this.pqGolpe = new PriorityQueue<Golpe>(10,comparadorGolpe::compare);
		this.pqMovimiento = new PriorityQueue<Movimiento>(10,comparadorMovimiento::compare);;
	}
	
	String getMovimiento() {
		if(pqMovimiento.size() == 0) {
			for (Movimiento choice : ListaMov.movimientosMov) {
				pqMovimiento.add(choice);
			}
		}
		return pqMovimiento.remove().nombreMovimiento;
	}
	
	String getGolpe() {
		if(pqGolpe.size() == 0) {
			for (Golpe choice : ListaGolpe.movimientosGolpe) {
				pqGolpe.add(choice);
			}
		}
		return pqGolpe.remove().nombreGolpe;		
	}
}

