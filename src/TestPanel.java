import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.team2485.AutoPath;
import org.team2485.AutoPath.Pair;

@SuppressWarnings("serial")
public class TestPanel extends JPanel implements KeyListener, MouseListener {
	
	private JFrame frame;
	private BufferedImage robot, fieldB, fieldR;
	private int w, h;
	private int id = -1;
	private boolean isInLen;
	private boolean isRotate = true;
	private ArrayList<Point> spline;
	private boolean hidden;
	private static ArrayList<Pair> pairs = new ArrayList<>();
	private File file = null;
	private JFileChooser fc = new JFileChooser();
	private boolean isRed = false;

	public TestPanel() throws IOException {

		frame = new JFrame();
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		fieldB = ImageIO.read(classLoader.getResourceAsStream("fieldB.png"));
		fieldR = ImageIO.read(classLoader.getResourceAsStream("fieldR.png"));
		robot = ImageIO.read(classLoader.getResourceAsStream("robot.png"));
		isRed = false;

		w = (isRed ? fieldR : fieldB).getWidth(null);
		h = (isRed ? fieldR : fieldB).getHeight(null);

		this.setSize(w, h);
		frame.add(this);
		
		frame.setSize(w, h);

		System.setProperty("apple.laf.useScreenMenuBar", "true");
		JMenuBar menu = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menu.add(fileMenu);
		
		JMenu helpMenu = new JMenu("Help");
		menu.add(helpMenu);

		JMenuItem controlsMenu = new JMenuItem("Controls");
		controlsMenu.addActionListener((ActionEvent e) -> {
				viewControls();
		});
		helpMenu.add(controlsMenu);
		
		JMenuItem openMenu = new JMenuItem("Open");
		openMenu.addActionListener((ActionEvent e) -> {
			try {
				open();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		fileMenu.add(openMenu);

		
		JMenuItem saveMenu = new JMenuItem("Save");
		saveMenu.addActionListener((ActionEvent e) -> {
			try {
				save(false);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		fileMenu.add(saveMenu);
		
		JMenuItem saveAsMenu = new JMenuItem("Save As");
		saveAsMenu.addActionListener((ActionEvent e) -> {
			try {
				save(true);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		fileMenu.add(saveAsMenu);
		
		this.add(menu);


		

		frame.setVisible(true);
		frame.repaint();

		spline = new ArrayList<>();
		spline.add(new Point(140 + 27, 500));

		addMouseListener(this);
		frame.addKeyListener(this);

	}

	@Override
	protected void paintComponent(Graphics g) {
		g.drawImage((isRed ? fieldR : fieldB), 0, 0, null);

		for (int i = 0; i < spline.size(); i++) {
			
			if (i != id && hidden) {
				continue;
			}

			double x = spline.get(i).x, y = spline.get(i).y;
			double angle = Math.toRadians(spline.get(i).angle);
			AffineTransform trans = new AffineTransform();
			trans.translate(robot.getWidth() / 2, robot.getHeight() / 2);
			trans.rotate(Math.PI - angle, robot.getWidth() / 2, robot.getHeight() / 2);
			AffineTransformOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_BILINEAR);
			BufferedImage dest = op.filter(robot, null);
			
			Composite original = ((Graphics2D) g).getComposite();
			Composite translucent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
			((Graphics2D) g).setComposite(translucent);
			g.drawImage(dest, (int) x - robot.getHeight(), (int) y - robot.getHeight(), null);
			((Graphics2D) g).setComposite(original);


			if (i == id) {
				g.setColor(Color.BLUE);
			} else {
				g.setColor(Color.PINK);
			}
			g.fillOval((int) (x - 5), (int) (y - 5), 10, 10);


			if (i > 0) {
				double len = spline.get(i).inLen;
				double x2 = x + len * Math.sin(angle), y2 = y + len * Math.cos(angle);
				g.setColor(Color.RED);
				g.drawLine((int) x, (int) y, (int) x2, (int) y2);
				g.fillOval((int) (x2 - 2), (int) (y2 - 2), 4, 4);
			}

			if (i < spline.size() - 1) {
				double len = spline.get(i).outLen;
				double x2 = x + len * Math.sin(angle), y2 = y + len * Math.cos(angle);
				g.setColor(Color.GREEN);
				g.drawLine((int) x, (int) y, (int) x2, (int) y2);
				g.fillOval((int) (x2 - 2), (int) (y2 - 2), 4, 4);
			}
		}

		g.setColor(Color.MAGENTA);
		for (int i = 0; i < spline.size() - 1; i++) {
			if (hidden && i != id && i + 1 != id) {
				continue;
			}
			AutoPath.Pair[] p = AutoPath.getPointsForBezier(200, 
					new AutoPath.Pair(spline.get(i).x, spline.get(i).y),
					new AutoPath.Pair(spline.get(i).getOut().x, spline.get(i).getOut().y), 
					new AutoPath.Pair(spline.get(i + 1).getIn().x, spline.get(i + 1).getIn().y),
					new AutoPath.Pair(spline.get(i + 1).x, spline.get(i + 1).y));

			int[] x = new int[p.length], y = new int[p.length];
			for (int j = 0; j < p.length; j++) {
				x[j] = (int) p[j].getX();
				y[j] = (int) p[j].getY();
			}
			g.drawPolyline(x, y, p.length);
		}

	}

	public static void main(String[] args) {
		try {
			new TestPanel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	private class Point {
		public double x, y, angle, outLen, inLen;
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
			this.angle = 0;
			this.outLen = 50;
			this.inLen = -50;
		}
		public Point getOut() {
			return new Point(x + outLen * Math.sin(Math.toRadians(angle)), y + outLen * Math.cos(Math.toRadians(angle)));
		}
		public Point getIn() {
			return new Point(x + inLen * Math.sin(Math.toRadians(angle)), y + inLen * Math.cos(Math.toRadians(angle)));
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		Point p;
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_U) {
			isRotate = !isRotate;
		} else if (keyCode == KeyEvent.VK_P) {
			
			for (int i = 0; i < spline.size(); i++) {
				System.out.println("p = new Point(" + spline.get(i).x + ", " + spline.get(i).y + ");");
				System.out.println("p.inLen = " + spline.get(i).inLen + ";");
				System.out.println("p.outLen = " + spline.get(i).outLen + ";");
				System.out.println("p.angle = " + spline.get(i).angle + ";");
				System.out.println("spline.add(p);");
			}
		} else if (keyCode == KeyEvent.VK_L) {
			loadFromPairs();
		} else if (keyCode == KeyEvent.VK_M) {
			while (spline.size() > 1) {
				spline.remove(1);
			}
			p = new Point(169.0, 469.0);
			p.inLen = -50.0;
			p.outLen = 150.0;
			p.angle = 180.0;
			spline.add(p);
			p = new Point(309.0, 239.0);
			p.inLen = 60;
			p.outLen = 180;
			p.angle = -56;
			spline.add(p);
			p = new Point(242.0, 422.0);
			p.inLen = 106;
			p.outLen = 50.0;
			p.angle = -63;
			spline.add(p);
		} else if (keyCode == KeyEvent.VK_E) {
			isRed = !isRed;
		}
		
		if (id < 0) {
			repaint();
			return;
		}

		if (keyCode == KeyEvent.VK_UP) {
			if (isRotate) {
				if (isInLen) {
					spline.get(id).inLen--;
				} else {
					spline.get(id).outLen++;
				}
			} else {
				spline.get(id).y--;
			}
		} else if (keyCode == KeyEvent.VK_DOWN) {
			if (isRotate) {

				if (isInLen) {
					spline.get(id).inLen++;
				} else {
					spline.get(id).outLen--;
				}
			} else {
				spline.get(id).y++;
			}
			
		} else if (keyCode == KeyEvent.VK_LEFT) {
			if (isRotate) {
				spline.get(id).angle++;
			} else {
				spline.get(id).x--;
			}
		} else if (keyCode == KeyEvent.VK_RIGHT) {
			if (isRotate) {
				spline.get(id).angle--;
			} else {
				spline.get(id).x++;
			}
		} else if (keyCode == KeyEvent.VK_H) {
			hidden = !hidden;
		} else if (keyCode == KeyEvent.VK_SPACE) {
			isInLen = !isInLen;
		} else if (keyCode == KeyEvent.VK_F) { // flip
			spline.get(id).inLen *= -1;
			spline.get(id).outLen *= -1;
			spline.get(id).angle += 180;

		} else if (keyCode == KeyEvent.VK_X) {
			spline.remove(id);
			id = -1;
		} else if (keyCode == KeyEvent.VK_ESCAPE) {
			id = -1;
		}

		repaint();

	}

	private String output(double x, double y) {
		return "new Pair(" + Math.round(2 * x) / 2.0 + ", " + Math.round(2 * y) / 2.0 + "),\n";
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (id < 0) {
			for (int i = 0; i < spline.size(); i++) {
				if (Math.hypot(spline.get(i).x - e.getX(), spline.get(i).y - e.getY()) < 5) {
					id = i;
					if (id == 0) {
						isInLen = false;
					} else if (id == spline.size() - 1) {
						isInLen = true;
					}
					break;
				}
			}
			if (id < 0) {
				int x = e.getX();
				int y = e.getY();
				spline.add(new Point(x, y));
			}
		} else {
			spline.get(id).x = e.getX();
			spline.get(id).y = e.getY();
		}

		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	private void open() throws IOException {
		int returnVal = fc.showOpenDialog(frame);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fc.getSelectedFile();
			while (spline.size() > 0) {
				spline.remove(0);
			}
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String side = reader.readLine();
			isRed = side.startsWith("R");
			String first = reader.readLine();
			String[] firstCoords = first.split(", ");
			spline.add(new Point(Double.parseDouble(firstCoords[0]), Double.parseDouble(firstCoords[1])));
			pairs = new ArrayList<>();
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				s = s.split("\\(")[1];
				s = s.split("\\)")[0];
				String[] coords = s.split(", ");
				pairs.add(new Pair(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
			}
			loadFromPairs();
			reader.close();
		}
	}
	
	private void save(boolean saveAs) throws IOException {
		if (file == null || saveAs) {
			int ret = fc.showSaveDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
				if (!file.exists()) {
					file.createNewFile();
				}
			}
		} 
		
		if (file != null) {
			BufferedWriter b = new BufferedWriter(new FileWriter(file));
 			b.write(isRed ? "R\n" : "B\n");
			double x = spline.get(0).x, 
					y = spline.get(0).y;
			b.write(x + ", " + y + "\n");

			for (int i = 0; i < spline.size(); i++) {
				if (i > 0) 
					b.write(output((spline.get(i).getIn().x - x) / 2, (y - spline.get(i).getIn().y) / 2));
				b.write(output((spline.get(i).x - x) / 2, (y - spline.get(i).y) / 2));
				if (i < spline.size() - 1)
					b.write(output((spline.get(i).getOut().x - x) / 2,  (y - spline.get(i).getOut().y) / 2));

			}
			b.close();
		}
	}
	
	private void loadFromPairs() {
		while (spline.size() > 1) {
			spline.remove(1);
		}
		for (int i = 0; i <= pairs.size() / 3; i++) {
			if (i == 0) {
				
				double dx = pairs.get(1).getX() - pairs.get(0).getX();
				double dy = - pairs.get(1).getY() + pairs.get(0).getY();
				spline.get(0).angle = Math.toDegrees(Math.atan2(dx, dy));
				spline.get(0).outLen = 2 *  Math.sqrt(dx * dx + dy * dy);
				
			} else {
				
				spline.add(new Point((pairs.get(3 * i).getX() - pairs.get(0).getX()) * 2 + spline.get(0).x, 
						(pairs.get(0).getY() - pairs.get(3 * i).getY()) * 2 + spline.get(0).y));
				
				double dx1 = pairs.get(3 * i - 1).getX() - pairs.get(3 * i).getX(),
						dy1 = - pairs.get(3 * i - 1).getY() + pairs.get(3 * i).getY();
				spline.get(i).angle = Math.toDegrees(Math.atan2(dx1, dy1));
				spline.get(i).inLen = 2 * Math.sqrt(dx1 * dx1 + dy1 * dy1);
				
				if (i < pairs.size() / 3) {
					double dx2 = pairs.get(3 * i + 1).getX() - pairs.get(3 * i).getX(),
							dy2 = - pairs.get(3 * i + 1).getY() + pairs.get(3 * i).getY();
					spline.get(i).outLen = 4 * (dx1 * dx2 + dy1 * dy2) / spline.get(i).inLen;
				}
				
			}
		}
		repaint();
	}

	private void viewControls() {
		JOptionPane.showMessageDialog(null, "Click on point when none selected - Select point\n" + 
											"Click anywhere else when no point selected - Create new point\n" + 
											"Click anywhere with point selected - Move selected point\n" + 
											"Escape - Deselect Point\n" + 
											"X - Remove selected point\n" + 
											"Horizontal Arrow Keys - Rotate or Translate\n" + 
											"Vertical Arrow Keys - Move control points or translate\n" + 
											"U - switch whether rotating or translating\n" + 
											"Space - switch whether adjusting in control point or out control point\n" + 
											"F - flip orientation of robot\n" + 
											"E - switch side of field\n"
										);
	}
}

