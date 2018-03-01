import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.team2485.AutoPath;
import org.team2485.AutoPath.Pair;



@SuppressWarnings("serial")
public class TestPanel extends JPanel implements KeyListener, MouseListener {

	private JFrame frame;
	private boolean usingBeziers;
	private BufferedImage robot, fieldBW, fieldColor;
	private int w, h;
	private int id = -1;
	private boolean isInLen;
	private boolean isRotate = true;
	private ArrayList<Point> spline;
	private boolean hidden;
	private File file = null;
	private JFileChooser fc = new JFileChooser();
	private boolean isColor = false;
	private double robotWidth;
	private boolean animating = false;
	private double dist = 0;
	private double speed = 100;
	private AutoPath path;

	public TestPanel() throws IOException {

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				dist += 0.005 * speed;
				if (animating) {
					frame.repaint();
				}
			}
		}, 0, 5);

		frame = new JFrame();

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		fieldBW = ImageIO.read(classLoader.getResourceAsStream("drawing.png"));
		fieldColor = ImageIO.read(classLoader.getResourceAsStream("drawing-color.png"));
		robot = ImageIO.read(classLoader.getResourceAsStream("robot.png"));
		isColor = false;
		usingBeziers = true;
		robotWidth = robot.getWidth();
		w = (isColor ? fieldColor : fieldBW).getWidth(null);
		h = (isColor ? fieldColor : fieldBW).getHeight(null);

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

		JMenuItem exportMenu = new JMenuItem("Export to clipboard");
		exportMenu.addActionListener((ActionEvent e) -> {
			export();
		});
		fileMenu.add(exportMenu);
		this.add(menu);





		frame.setVisible(true);
		frame.repaint();

		spline = new ArrayList<>();
		spline.add(new Point(100, 100));
		spline.add(new Point(300, 100));
		spline.add(new Point(300, 300));
		spline.get(1).dMax = 100;



		addMouseListener(this);
		frame.addKeyListener(this);

	}

	private void updatePath() {
		// create path
		Pair[] controlPoints = new Pair[spline.size()];
		double[] dists = new double[spline.size() - 2];
		for (int i = 0; i < spline.size(); i++) {
			if (i > 0 && i < spline.size() - 1) {
				dists[i - 1] = spline.get(i).dMax;
			}
			controlPoints[i] = new Pair(spline.get(i).x, spline.get(i).y);

		}

		path = AutoPath.getAutoPathForClothoidSpline(controlPoints, dists);

	}

	@Override
	protected void paintComponent(Graphics g) {
		g.drawImage((isColor ? fieldColor : fieldBW), 0, 0, null);

		for (int i = 0; i < spline.size(); i++) {

			if (i != id && hidden) {
				continue;
			}
			((Graphics2D) g).setStroke(new BasicStroke(1));
			RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			rh.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE));
			((Graphics2D) g).setRenderingHints(rh);

			double x = spline.get(i).x, y = spline.get(i).y;
			double angle = 0;
			if (usingBeziers) {
				angle = Math.toRadians(spline.get(i).angle);
			} else if (i == 0 ){
				angle = Math.atan2(spline.get(1).x - spline.get(0).x, spline.get(1).y - spline.get(0).y);
			} else if (i == spline.size() - 1) {
				angle = Math.atan2(spline.get(i).x - spline.get(i-1).x, spline.get(i).y - spline.get(i-1).y);
			}
			AffineTransform trans = new AffineTransform();
			trans.translate(robot.getWidth() / 2.0, robot.getHeight() / 2.0);
			trans.rotate(Math.PI - angle, robot.getWidth() / 2.0, robot.getHeight() / 2.0);
			AffineTransformOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_BILINEAR);
			BufferedImage dest = op.filter(robot, null);

			if (!animating && (usingBeziers || i == 0 || i == spline.size() - 1)) {
				Composite original = ((Graphics2D) g).getComposite();
				Composite translucent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (i == id ? 0.75f : 0.25f));
				((Graphics2D) g).setComposite(translucent);
				g.drawImage(dest, (int) Math.round(x - robot.getWidth()), (int)Math.round(y - robot.getHeight()), null);
				((Graphics2D) g).setComposite(original);
			} 

			if (id == i) {
				g.setColor(Color.BLACK);
			} else {
				g.setColor(Color.GRAY);
			}

			g.fillOval((int) (x - 5), (int) (y - 5), 10, 10);


			if (i > 0 && usingBeziers) {
				double len = spline.get(i).inLen;
				double x2 = x + len * Math.sin(angle), y2 = y + len * Math.cos(angle);
				g.setColor(Color.BLUE);
				g.drawLine((int) x, (int) y, (int) x2, (int) y2);
				g.fillOval((int) (x2 - 2), (int) (y2 - 2), 4, 4);
			}

			if (i < spline.size() - 1 && usingBeziers) {
				double len = spline.get(i).outLen;
				double x2 = x + len * Math.sin(angle), y2 = y + len * Math.cos(angle);
				g.setColor(Color.GREEN);
				g.drawLine((int) x, (int) y, (int) x2, (int) y2);
				g.fillOval((int) (x2 - 2), (int) (y2 - 2), 4, 4);
			}
		}

		double tempDist = dist;
		Path2D curve = new Path2D.Double();
		((Graphics2D) g).setStroke(new BasicStroke(2));
		Stroke str = new BasicStroke((float) robotWidth);

		if (usingBeziers) {
			g.setColor(Color.RED);

			for (int i = 0; i < spline.size() - 1; i++) {
				if (hidden && i != id && i + 1 != id) {
					continue;
				}

				CubicCurve2D temp = new CubicCurve2D.Double(spline.get(i).x, spline.get(i).y, 
						spline.get(i).getOut().x, spline.get(i).getOut().y, 
						spline.get(i + 1).getIn().x, spline.get(i + 1).getIn().y, 
						spline.get(i + 1).x, spline.get(i + 1).y);

				if (i > 0 && spline.get(i).outLen * spline.get(i).inLen > 0) { // turn around
					((Graphics2D) g).draw(curve);
					((Graphics2D) g).draw(str.createStrokedShape(curve));
					curve = new Path2D.Double();
				} 
				curve.append(temp, true);



				if(animating) {
					AutoPath.Pair[] p = AutoPath.getPointsForBezier(200, 
							new AutoPath.Pair(spline.get(i).x, spline.get(i).y),
							new AutoPath.Pair(spline.get(i).getOut().x, spline.get(i).getOut().y), 
							new AutoPath.Pair(spline.get(i + 1).getIn().x, spline.get(i + 1).getIn().y),
							new AutoPath.Pair(spline.get(i + 1).x, spline.get(i + 1).y));
					AutoPath path = new AutoPath(p);
					boolean inverted = spline.get(i).outLen < 0;
					if (tempDist < path.getPathLength() && tempDist >= 0) {
						AutoPath.Point point = path.getPointAtDist(tempDist);
						double x = point.x;
						double y = point.y;
						double angle = point.heading + (inverted ? Math.PI : 0);
						AffineTransform trans = new AffineTransform();
						trans.translate(robot.getWidth() * .5, robot.getHeight() *.5);
						trans.rotate(Math.PI - angle, robot.getWidth() *.5, robot.getHeight() *.5);
						AffineTransformOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_BILINEAR);
						BufferedImage dest = op.filter(robot, null);

						Composite original = ((Graphics2D) g).getComposite();
						((Graphics2D) g).setComposite(original);
						g.drawImage(dest, (int) x - robot.getWidth(), (int) y - robot.getHeight(), null);
					}
					tempDist -= path.getPathLength();
				}

			}
			((Graphics2D) g).draw(curve);
			((Graphics2D) g).draw(str.createStrokedShape(curve));
		} else {

			updatePath();

			((Graphics2D) g).setStroke(new BasicStroke(1));
			g.setColor(Color.RED);



			Pair[] pairs = path.getPairs();
			curve.moveTo(pairs[0].getX(), pairs[0].getY());
			for (int j = 1; j < pairs.length; j++) {
				curve.lineTo(pairs[j].getX(), pairs[j].getY());

			}
			((Graphics2D) g).draw(curve);
			((Graphics2D) g).draw(str.createStrokedShape(curve));

			if (animating) {
				if (tempDist < path.getPathLength() && tempDist >= 0) {
					AutoPath.Point point = path.getPointAtDist(tempDist);
					double x = point.x;
					double y = point.y;
					double angle = point.heading;
					AffineTransform trans = new AffineTransform();
					trans.translate(x - robot.getWidth() * .5, y - robot.getHeight() * .5);
					trans.rotate(Math.PI - angle, robot.getWidth() * .5, robot.getHeight() * .5);
					AffineTransformOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_BILINEAR);
					BufferedImage dest = op.filter(robot, null);

					Composite original = ((Graphics2D) g).getComposite();
					((Graphics2D) g).setComposite(original);
					g.drawImage(dest, 0, 0, null);
				}
				tempDist -= path.getPathLength();


			}




			//draw dashed lines

			double percent = 1 - spline.get(1).dMax / Math.hypot(spline.get(1).x - spline.get(0).x, 
					spline.get(1).y - spline.get(0).y);
			double xEnd = (1 - percent) * spline.get(0).x + percent * spline.get(1).x;
			double yEnd = (1 - percent) * spline.get(0).y + percent * spline.get(1).y;


			Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
			((Graphics2D) g).setStroke(dashed);
			g.setColor(Color.GREEN);
			((Graphics2D) g).drawLine((int) xEnd, (int) yEnd, (int) spline.get(1).x, (int) spline.get(1).y);

			for (int i = 1; i < spline.size() - 1; i++) {


				double thisX = spline.get(i).x, thisY = spline.get(i).y;
				double nextX = spline.get(i+1).x, nextY = spline.get(i+1).y;



				double percentStart = spline.get(i).dMax / Math.hypot(nextX - thisX, nextY - thisY);
				double percentEnd = (i == spline.size() - 1) ? 1 : 1 - spline.get(i + 1).dMax / Math.hypot(nextX - thisX, nextY - thisY);
				double xStart = (1 - percentStart) * thisX + percentStart * nextX;
				double yStart = (1 - percentStart) * thisY + percentStart * nextY;
				xEnd = (1 - percentEnd) * thisX + percentEnd * nextX;
				yEnd = (1 - percentEnd) * thisY + percentEnd * nextY;


				((Graphics2D) g).setStroke(dashed);
				g.setColor(Color.GREEN);

				if (percentStart > 0) {
					((Graphics2D) g).drawLine((int) thisX, (int) thisY, (int) xStart, (int) yStart);
				} 
				if (percentEnd < 1) {
					((Graphics2D) g).drawLine((int) xEnd, (int) yEnd, (int) nextX, (int) nextY);
				}


			}

		}





		if (tempDist > 0) {
			animating = false;
		}


	}

	private static void drawPairs(Pair[] pairs, Graphics2D g2d) {
		int[] x = new int[pairs.length], y = new int[pairs.length];
		for (int i = 0; i < pairs.length; i++) {
			x[i] = (int) pairs[i].getX();
			y[i] = (int) pairs[i].getY();
		}
		g2d.drawPolyline(x, y, pairs.length);
	}

	public static void main(String[] args) {
		try {
			new TestPanel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class Point {
		public double x, y, angle, outLen, inLen, dMax;
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
			this.angle = 0;
			this.outLen = 50;
			this.inLen = -50;
			this.dMax = 0;
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
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_U) {
			isRotate = !isRotate;
		} else if (keyCode == KeyEvent.VK_E) {
			isColor = !isColor;
		} else if (keyCode == KeyEvent.VK_A) {
			animating = true;
			dist = 0;
		} else if (keyCode == KeyEvent.VK_B) {
			usingBeziers = !usingBeziers;

		}

		if (id < 0) {
			repaint();
			return;
		}

		if (keyCode == KeyEvent.VK_UP) {
			if (isRotate) {
				if (usingBeziers) {
					if (isInLen) {
						spline.get(id).inLen--;
					} else {
						spline.get(id).outLen++;
					}
				} else {
					spline.get(id).dMax++;
				}
			} else {
				spline.get(id).y--;
			}
		} else if (keyCode == KeyEvent.VK_DOWN) {
			if (isRotate) {
				if (usingBeziers) {
					if (isInLen) {
						spline.get(id).inLen++;
					} else {
						spline.get(id).outLen--;
					} 
				} else {
					spline.get(id).dMax--;
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
		} else if (keyCode == KeyEvent.VK_TAB) {
			id++; 
			if (id >= spline.size()) {
				id -= spline.size();
			}
		}  else if (keyCode == KeyEvent.VK_ESCAPE) {
			id = -1;
		}

		repaint();

	}

	private String output(double x, double y) {
		return "new Pair(" + Math.round(x) / 2.0 + ", " + Math.round(y) / 2.0 + "),\n";

	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger()) {
			JPopupMenu menu = new JPopupMenu();
			JMenuItem menuItem = new JMenuItem(id < 0 ? "New" : "Delete");
			menuItem.addActionListener((ActionEvent ev) -> {
				if (id < 0) {
					newPoint(e.getX(), e.getY());
				} else { 
					delete(id);
				}

			});
			menu.add(menuItem);
			menu.show(e.getComponent(), e.getX(), e.getY());
		} else if (id < 0) {
			for (int i = 0; i < spline.size(); i++) {
				if (Math.hypot(spline.get(i).x - e.getX(), spline.get(i).y - e.getY()) < robotWidth / 2) {
					id = i;
					if (id == 0) {
						isInLen = false;
					} else if (id == spline.size() - 1) {
						isInLen = true;
					}
					break;
				}
			}
		} else {
			spline.get(id).x = e.getX();
			spline.get(id).y = e.getY();
		}
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			System.out.println("thn");
			JPopupMenu menu = new JPopupMenu();
			JMenuItem menuItem = new JMenuItem(id < 0 ? "New" : "Delete");
			menuItem.addActionListener((ActionEvent ev) -> {
				if (id < 0) {
					newPoint(e.getX(), e.getY());
				} else { 
					delete(id);
				}

			});
			menu.add(menuItem);
			menu.show(e.getComponent(), e.getX(), e.getY());
		} 


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
			String bezier = reader.readLine();
			usingBeziers = bezier.startsWith("B");
			String color = reader.readLine();
			isColor = color.startsWith("C");
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				String[] coords = s.split(", ");
				Point p = new Point(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
				p.inLen = Double.parseDouble(coords[2]);
				p.outLen = Double.parseDouble(coords[3]);
				p.angle = Double.parseDouble(coords[4]);
				p.dMax = Double.parseDouble(coords[5]);
				spline.add(p);
			}
			reader.close();
		}
		repaint();
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
			b.write(usingBeziers ? "Bezier\n" : "Clothoid\n");
			b.write(isColor ? "Color\n" : "BW\n");
			for (int i = 0; i < spline.size(); i++) {
				b.write(spline.get(i).x + ", " + spline.get(i).y + ", " +  spline.get(i).inLen + ", " + 
						spline.get(i).outLen + ", "  + spline.get(i).angle + ", " + spline.get(i).dMax  + "\n");
			}
			b.close();
		}
	}

	private void export() {
		String s = "";

		if (usingBeziers) {
			for (int i = 0; i < spline.size(); i++) {
				double x = spline.get(0).x, 
						y = spline.get(0).y;
				if (i > 0) 
					s += output(spline.get(i).getIn().x - x, y - spline.get(i).getIn().y);
				s += output(spline.get(i).x - x, y - spline.get(i).y);
				if (i < spline.size() - 1)
					s += output(spline.get(i).getOut().x - x,  y - spline.get(i).getOut().y);


			} 
		} else {
			s += "Pair[] controlPoints = {";
			for (int i = 0; i < spline.size(); i++) {
				s += output(spline.get(i).x - spline.get(0).x, spline.get(i).y - spline.get(0).y);
			}
			s += "};\n";
			s += "double[] dists = {";
			for (int i = 0; i < spline.size() - 2; i++) {
				s += spline.get(i + 1).dMax + ",";
			}
			s += "};\n";
			s += "path = AutoPath.getAutoPathForClothoidSpline(controlPoints, dists);";
		}
		System.out.println(s);

		StringSelection selection = new StringSelection(s);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);


	}

	private void newPoint(int x, int y) {
		spline.add(new Point(x, y));
		repaint();
	}

	private void delete(int id) {
		spline.remove(id);
		this.id = -1;
		repaint();
	}


	private void viewControls() {
		JOptionPane.showMessageDialog(null, "Click on image of robot to select it\n" + 
				"Click anywhere with robot selected to move it\n" + 
				"Escape - Deselect\n" + 
				"Tab - select next point\n" +
				"Right click - Add / Remove point\n" + 
				"Horizontal Arrow Keys - Rotate or Translate\n" + 
				"Vertical Arrow Keys - Move control points or translate\n" + 
				"U - switch whether rotating or translating\n" + 
				"Space - switch whether adjusting in control point or out control point\n" + 
				"B - switch beziers vs clothoids\n" + 
				"E - switch color of field\n" + 
				"A - animate path\n" +
				"H - hide robot images (only show path)\n"
				);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}
}

