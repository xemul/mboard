import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*
 * GUI for experiments
 */

class GUI extends JFrame implements UI, MouseListener
{
	private final static int fw = 40;
	private final static int brd = 10;
	private final static int voff = 50;

	private int board_size;
	private int win_w;
	private int win_h;

	private Game game;
	private Label s_label;

	private int lr, lc; /* Last click */
	private int move;

	ArrayList<Score> wins;

	public GUI()
	{
		super("Wave renju");

		board_size = Game.size;
		win_w = board_size * fw + 2 * brd;
		win_h = board_size * fw + 2 * brd + voff;

		game = new Game();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(win_w, win_h);
		addMouseListener(this);

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		s_label = new Label("");
		c.add(s_label, BorderLayout.PAGE_START);

		lr = lc = -1;
		wins = null;

		setVisible(true);
	}

	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void invalid_move() {}
	public void game_over(ArrayList<Score> wins)
	{
		this.wins = new ArrayList<Score>(wins);
	}

	private int col2x(int col) { return brd + col * fw; }
	private int x2col(int x) { return (x - brd) / fw; }
	private int row2y(int row) { return voff + brd + row * fw; }
	private int y2row(int y) { return (y - voff - brd) / fw; }

	public void mouseClicked(MouseEvent e)
	{
		int row = y2row(e.getY());
		int col = x2col(e.getX());
		int but = e.getButton();

		if (but == MouseEvent.BUTTON1) {
			if (lr == row && lc == col) {
				lr = -1;
				lc = -1;
			} else {
				lr = row;
				lc = col;
			}
		} else if (wins == null) {
			if (lr != -1) {
				this.game.move(lr, lc, row, col, this);
				lr = -1;
				lc = -1;
			}
		}

		/*
		 * FIXME -- this redraws the whole screen. Need to optimize.
		 */
		repaint();
	}

	static Color wave2col(int wa)
	{
		if (wa > 5)
			wa = 5;
		else if (wa < -5)
			wa = -5;

		return new Color((float).0, (float).0, (float)(0.5 + wa * 0.1));
	}

	public void paint(Graphics g)
	{
		Graphics2D g2d = (Graphics2D)g;

		g2d.setBackground(Color.black);
		g2d.clearRect(0, voff, win_w, win_h - voff);

		/*
		 * Fill the board cells
		 */
		for (int c = 0; c < board_size; c++) {
			for (int r = 0; r < board_size; r++) {
				int wa = game.wave_at(r, c);

				g2d.setPaint(wave2col(wa));

				Polygon p = new Polygon();
				p.addPoint(col2x(c) + 1,     row2y(r) + 1);
				p.addPoint(col2x(c + 1) - 1, row2y(r) + 1);
				p.addPoint(col2x(c + 1) - 1, row2y(r + 1) - 1);
				p.addPoint(col2x(c) + 1,     row2y(r + 1) - 1);
				g.fillPolygon(p);
			}
		}

		String s = String.format("");

		if (wins == null) {
			if (game.turn > 0)
				s += String.format("Bright turn");
			else
				s += String.format("Dark turn");
		} else {
			boolean bw = false, dw = false;

			g2d.setPaint(Color.green.darker());
			Stroke bst = g2d.getStroke();
			g2d.setStroke(new BasicStroke(2));

			for (Score ws: wins) {
				g.drawLine(col2x(ws.start_c) + fw / 2, row2y(ws.start_r) + fw / 2,
						col2x(ws.c) + fw / 2, row2y(ws.r) + fw / 2);

				if (ws.v > 0)
					bw = true;
				else
					dw = true;
			}

			if (bw && dw)
				s += String.format("Game draw");
			else if (bw)
				s += String.format("Bright win");
			else
				s += String.format("Dark win");

			g2d.setStroke(bst);
		}

		if (lr != -1) {
			g2d.setPaint(Color.black);
			g.drawOval(col2x(lc) + 5, row2y(lr) + 5, fw - 10, fw - 10);
			s += String.format(" %d", game.wave_at(lr, lc));
		}

		s_label.setText(s);
	}
}

public class wg_gui
{
	public static void main(String[] args)
	{
		GUI gui = new GUI();
	}
}
