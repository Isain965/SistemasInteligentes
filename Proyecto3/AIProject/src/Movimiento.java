
public class Movimiento {
		public String nombreMovimiento;
		public String estadoMovimiento;
		public double distanciaX;
		public double distanciaY;
		
		public Movimiento(String nombreMovimiento, String estadoMovimiento, double speedX,
				double speedY, double distanciaX, double distanciaY) {
			this.nombreMovimiento = nombreMovimiento;
			this.estadoMovimiento = estadoMovimiento;
			this.distanciaX = distanciaX;
			this.distanciaY = distanciaY;
			
		}
		
		void setDistanciaX(double distanciaX) {
			this.distanciaX = distanciaX;
		}
		
		void setDistanceY(double distanceY) {
			this.distanciaY = distanceY;
		}
		
	}