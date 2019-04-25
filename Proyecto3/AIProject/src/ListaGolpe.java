import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ListaGolpe {
	
	public static ArrayList<Golpe> movimientosGolpe = new ArrayList<Golpe>();
	private BufferedReader br;
	
	public ListaGolpe() throws IOException{
		opciones();
	    
	}
	
	String ruta = "C:/Users/isain/eclipse-workspace/AIProject/src/Motion.csv";
	
	private void opciones() throws IOException{
		br = new BufferedReader(new FileReader(ruta));
		String line;
		while ((line = br.readLine()) != null) {
			String[] values = line.split(",");
			
			double val = Math.random()*10;
			
			if (Integer.parseInt(values[19]) != 0 && (val > 4)) { 
				ListaGolpe.movimientosGolpe.add(new Golpe(values[0], values[8], Integer.parseInt(values[20])));
			}
			
			else if (Integer.parseInt(values[19]) != 0){
				ListaGolpe.movimientosGolpe.add(new Golpe(values[0], values[8], Integer.parseInt(values[20])));
			}
		}
	}
}