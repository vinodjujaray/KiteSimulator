import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;



public class KiteShow {  
	static class KiteDrawPanel extends JPanel {
		public KiteShow ks;
		public void setKiteShow( KiteShow ks)
		{
			this.ks = ks;
		}

		public Point2D getPointOnLine(Point2D p, double length, double angle)
		{
			double x = p.getX() + length * Math.cos(angle);
			double y = p.getY() - length * Math.sin(angle);
			return new Point2D.Double(x,y);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			double locx = 400;
			double locy = 100;
			double scale = 20*100;  // 1cm = 40pixels


			//Draw Kite Spine
			g2d.draw( new Line2D.Double( 
				locx - ks.k.SpineLength/2*Math.cos(ks.FlyAngle)*scale, 
				locy + ks.k.SpineLength/2*Math.sin(ks.FlyAngle)*scale,
				locx + ks.k.SpineLength/2*Math.cos(ks.FlyAngle)*scale, 
				locy - ks.k.SpineLength/2*Math.sin(ks.FlyAngle)*scale));

			//Draw Kite Center of Spine
			g2d.fill ( new Ellipse2D.Double(locx-3, locy-3, 6, 6) );
			g2d.drawString("KS", (float)locx, (float)locy-8);
			
			//Draw Center of Mass
			Point2D p = new Point2D.Double ( locx, locy);  //This is center of spine
			Point2D cm = getPointOnLine(p, -ks.k.Cm2CsDistance *scale, ks.FlyAngle);  //Center of Mass
			g2d.fill(new Ellipse2D.Double( cm.getX() -3, cm.getY() -3, 6, 6));
			g2d.drawString("CM",(float) cm.getX(),(float) cm.getY()-8);

			//Draw Kite Center of Pressure
			Point2D  cp = getPointOnLine(cm, ks.k.Cm2CpDistance*scale, ks.FlyAngle);
			g2d.fill(new Ellipse2D.Double( cp.getX() -3, cp.getY() -3, 6, 6));
			g2d.drawString("CP", (float)cp.getX(), (float)cp.getY()-8);


			//Draw TiePoint  base
			Point2D ctb = getPointOnLine(cm, ks.p.Cm2TiePoint * scale, ks.FlyAngle);
			g2d.fill(new Ellipse2D.Double( ctb.getX() -3, ctb.getY() -3, 6, 6));
			g2d.drawString("TieBase", (float)ctb.getX(), (float)ctb.getY()-8);

			//Draw TiePoint  
			Point2D ctp = getPointOnLine(ctb, ks.p.PerpTiePoint* scale, ks.FlyAngle - Math.PI / 2);
			g2d.fill(new Ellipse2D.Double( ctp.getX() -3, ctp.getY() -3, 6, 6));
			g2d.drawString("TiePoint", (float)ctp.getX(), (float)ctp.getY()-8);

			//Draw LineOfAction
			Point2D  actionPoint = getPointOnLine(ctp,  0.17*scale, -ks.t.angle);
			g2d.draw(new Line2D.Double(ctp.getX(), ctp.getY(), actionPoint.getX(), actionPoint.getY()));

			//Draw Loa from Kite
			Point2D  loaKite = getPointOnLine(cm, ks.l.distance *scale, ks.FlyAngle);
			Point2D  loaEp   = getPointOnLine(loaKite, 0.05 * scale,  -ks.t.angle);
			g2d.draw(new Line2D.Double(loaKite.getX(), loaKite.getY(), loaEp.getX(), loaEp.getY()));

			//Draw Statistics
			String flyAngle = "FlyAngle: " + ks.ToDegrees(ks.FlyAngle) + " degrees";
			g2d.drawString(flyAngle, 20, 400);
			String stringAngle = "StringAngle= " + ks.ToDegrees(ks.t.angle) + " degrees";
			g2d.drawString(stringAngle, 20, 440);
			String stringTension = "String Tension: " + ks.t.abs() + "Kg";
			g2d.drawString(stringTension, 20, 420);
			String actualWindForce = "WindForce= " + ks.ActualWindForce + "Kg";
			g2d.drawString(actualWindForce, 20, 460);


		}
	}
	static class LineOfAction {
		public double distance;
		public double angle;
		public LineOfAction() {
		}
		
	}
	static class KiteParams {
		public double Weight;
		public double Cm2CpDistance;
		public double Cm2CsDistance;
		public double WindPressure;
		public double SpineLength;
		public KiteParams()  {
		}
	}
	static class StringTension {
		public double tx;
		public double ty;
		public double angle;
		public double abs() {
			return Math.sqrt( tx*tx + ty*ty );
		}
		public StringTension () {
		}
	}

	static class KiteTiePoint {
		public double Cm2TiePoint;
		public double PerpTiePoint;
		public KiteTiePoint() { }
	}
	public double ComputeTorque(KiteParams k, KiteTiePoint p, double angle)
	{
		double torque = (p.Cm2TiePoint -k.Cm2CpDistance)*k.WindPressure*Math.sin(angle) 
			- k.Weight*( p.Cm2TiePoint*Math.cos(angle) + p.PerpTiePoint*Math.sin(angle));
		return torque;
	}

	public StringTension ComputeTension(KiteParams k, double angle) 
	{
		StringTension t = new StringTension();
		t.tx = k.WindPressure *Math.sin(angle) *Math.sin(angle);
		t.ty = k.WindPressure *Math.sin(angle) *Math.cos(angle)  -k.Weight;
		t.angle = Math.atan2(t.ty, t.tx);
		return t;
	}
	public LineOfAction  ComputeLineOfAction(KiteParams k, StringTension t, double angle)
	{
		LineOfAction l = new LineOfAction();
		l.angle = angle + t.angle;
		double tabs = t.abs();
		l.distance = ActualWindForce * k.Cm2CpDistance /tabs /Math.sin(l.angle);
		return l;
	}

	public KiteParams k;
	public KiteTiePoint p;
	public StringTension t;
	public LineOfAction l;
	public double FlyAngle;
	public double ActualWindForce;

	public void ComputeFlyParams( ) {
		double angle = 1;
		double preAngle = 90;
		double testAngle = (angle + preAngle)/2;

		double torque = ComputeTorque(k, p, ToRadians(testAngle) );

		int maxIters =  1500;

		
		while ( maxIters > 0 && fabs (torque) > 0.000000000001 )
		{ 
			if ( torque > 0 ) {
				preAngle =  testAngle;
			} else {
				angle   = testAngle;
			}
			testAngle = (angle+preAngle)/2;
			torque = ComputeTorque(k, p, ToRadians(testAngle));
			System.out.println("Angle= " + testAngle + " Torque= " + torque);
			maxIters --;
		}
		FlyAngle = ToRadians(testAngle);
		System.out.println("Found Angle= " + testAngle + " Torque= " + torque);
		ActualWindForce = k.WindPressure * Math.sin(FlyAngle);

		t = ComputeTension(k, FlyAngle);
		l = ComputeLineOfAction(k, t, FlyAngle);

		System.out.println("LoaDist: "+ l.distance);
	}
	

		
	public KiteShow()  
    	{  
		k = new KiteParams();
		k.Weight = 0.010 ;    //10 grams
		k.WindPressure =  0.600;  // 1 poind Kite
		k.Cm2CpDistance = 0.02;   // Centr of mass to center of pressure distance 
		k.Cm2CsDistance = -0.01;  // Center of mass to center of spine distance
		k.SpineLength   = 0.3;    // Length of Kite

		p = new KiteTiePoint();
		p.Cm2TiePoint = 0.03;     // Tie point base distance from CM
		p.PerpTiePoint = 0.12;    // Tie point location perp distance from kite
		t = new StringTension();
		l = new LineOfAction();

		ComputeFlyParams();


		//DrawKite(k, t, l, angle);

		/*
		Frame f = new Frame();  
		Button btn=new Button("Hello World");  
		btn.setBounds(80, 80, 100, 50);  
		f.add(btn);         //adding a new Button.  
		f.setSize(300, 250);        //setting size.  
		f.setTitle("JavaTPoint");  //setting title.  
		f.setLayout(null);   //set default layout for frame.  
		f.setVisible(true);           //set frame visibility true.  
		*/
	}  

	public static double ToDegrees(double ang) {
		return ang * 180 / Math.acos(-1);
	}

	public static double ToRadians(double ang) {
		return ang * Math.acos(-1) / 180;
	}

	public static double fabs(double d) {
		if (d >0 ) return d;
		else return -d;
	}

	
	public static void main(String[] args) {  
		KiteShow ks = new KiteShow();
		JFrame frame = new JFrame("Kite Flying Program");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024, 800);
		KiteDrawPanel panel = new KiteDrawPanel();
		panel.setKiteShow(ks);
		frame.add(panel);
		frame.setVisible(true);
	}  
}  


