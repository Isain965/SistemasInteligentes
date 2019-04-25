import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ListaMov {
	
	public static ArrayList<Movimiento> movimientosMov = new ArrayList<Movimiento>();
	private BufferedReader br;;
	
	public ListaMov() throws IOException{
		opciones();
	    
	}
	
	String ruta = "C:/Users/isain/eclipse-workspace/AIProject/src/Motion.csv";
	
	private void opciones() throws IOException{
		br = new BufferedReader(new FileReader(ruta));
		String line;
		String[] anterior = null;
		int i = 0;
		while ((line = br.readLine()) != null) {
			String[] values = line.split(",");
			
			if (i == 0) {
				anterior = values;
			}
			
			double val = Math.random()*10;
			
			
			if (Integer.parseInt(values[19]) == 0 && Integer.parseInt(values[39]) > -1 && val < 5 ) { 
				ListaMov.movimientosMov.add(new Movimiento(values[0], values[8], Double.parseDouble(values[39]),
						Double.parseDouble(values[40]), 0 , 0)); 
			} else {
				ListaMov.movimientosMov.add(new Movimiento(anterior[0], anterior[8], Double.parseDouble(anterior[39]),
						Double.parseDouble(anterior[40]), 0 , 0)); 
			}
			
			anterior = values;
		}
	}
}
