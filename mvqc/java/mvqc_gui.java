import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*
 * GUI for experiments
 */

class GUI extends JFrame implements UI, MouseListener
{
	public void no_checker(Pos pos) { }
	public void measure_cell(Pos pos) { }
	public void measure_situation() { }
	public void make_move(Pos pos) { }
	public void cell_stuck(Pos pos) { }
	public void moved_to(Pos npos) { }
	public void capture_at(Pos npos, Pos nnpos) { }
	public void ai_move(Pos pos) { ai_last_move = pos; }
	public void log(String str) { }

	public void game_over(int turn, Prob p)
	{
		loser = turn;
		lp = p;
	}

	private final static int fw = 50;
	private final static int brd = 10;
	private final static int voff = 50;

	private int board_size;
	private int win_w;
	private int win_h;

	private Game game;
	private Label s_label;
	private int loser;
	private Prob lp;
	private Pos ai_last_move;

	private boolean ai;

	public GUI(Rules r, String init, boolean ai)
	{
		super("Quantum Checkers");

		this.ai = ai;

		board_size = r.board_size;
		win_w = board_size * fw + 2 * brd;
		win_h = board_size * fw + 2 * brd + voff;
		loser = Ck.CNONE;
		game = new Game(r, init, this);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(win_w, win_h);
		addMouseListener(this);

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		s_label = new Label("");
		c.add(s_label, BorderLayout.PAGE_START);

		setVisible(true);
	}

	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void mouseClicked(MouseEvent e)
	{
		if (loser != Ck.CNONE)
			return;

		int x = e.getX();
		int y = e.getY();

		int line = (y - voff - brd) / fw;
		int col = (x - brd) / fw;

		this.game.move(line, col);

		if (this.ai && (this.game.turn == Ck.CTOP))
			this.game.move_ai();

		/*
		 * FIXME -- this redraws the whole screen. Need to optimize.
		 */
		repaint();
	}

	private void draw_checker(Graphics g, int l, int n, Cell c)
	{
		Graphics2D g2d = (Graphics2D)g;
		int x, y, angle;
		Prob p;

		/*
		 * Turn board cell number into graphics coordinates
		 */
		n *= 2;
		if (l % 2 == 0)
			n++;
		x = brd + n * fw;
		y = voff + brd + l * fw;

		p = c.c_prob[Ck.CBOT];
		if (!p.is_zero()) {
			angle = p.mul_and_get(360);
			g2d.setPaint(Color.white);
			g.fillArc(x + 3, y + 3, fw - 6, fw - 6, 30, angle);
		}

		p = c.c_prob[Ck.CTOP];
		if (!p.is_zero()) {
			angle = p.mul_and_get(360);
			g2d.setPaint(Color.black);
			g.fillArc(x + 3, y + 3, fw - 6, fw - 6, 210, angle);
		}
	}

	public void paint(Graphics g)
	{
		Graphics2D g2d = (Graphics2D)g;
		Color w = Color.yellow.darker();
		Color b = Color.orange.darker();

		g2d.setBackground(Color.black);
		g2d.clearRect(0, voff, win_w, win_h - voff);

		/*
		 * Fill the board cells
		 */
		for (int i = 0; i < board_size; i++) {
			for (int j = 0; j < board_size; j++) {
				if ((i + j) % 2 == 0)
					g2d.setPaint(w);
				else
					g2d.setPaint(b);

				Polygon p = new Polygon();
				p.addPoint(brd + i * fw + 1,       voff + brd + j * fw + 1);
				p.addPoint(brd + (i + 1) * fw - 1, voff + brd + j * fw + 1);
				p.addPoint(brd + (i + 1) * fw - 1, voff + brd + (j + 1) * fw - 1);
				p.addPoint(brd + i * fw + 1,       voff + brd + (j + 1) * fw - 1);
				g.fillPolygon(p);
			}
		}

		/*
		 * Now draw the checkers
		 */

		for (int l = 0; l < board_size; l++) {
			for (int n = 0; n < board_size/2; n++) {
				Pos p = new Pos(l, n);
				Cell c = this.game.superposition(p);
				draw_checker(g, l, n, c);
			}
		}

		if (loser == Ck.CNONE)
			/* Show who's move it is */
			g2d.setPaint(Color.green);
		else
			/* Otherwise -- mark loser with read line */
			g2d.setPaint(Color.red);

		g2d.setStroke(new BasicStroke(3));
		if (this.game.turn == Ck.CBOT)
			g.drawLine(brd, win_h - brd - 1, win_w - brd, win_h - brd - 1);
		else
			g.drawLine(brd, voff + brd - 1, win_w - brd, voff + brd - 1);

		/* And stats */
		if (loser == Ck.CNONE) {
			String s = game.stats();
			if (ai_last_move != null) {
				s += " AI: ";
				s += ai_last_move.s();
			}

			s_label.setText(s);
		} else {
			String s = String.format("%s has lost", loser == Ck.CBOT ? "White" : "Black");
			if (!lp.is_one())
				s += String.format(" (probably)");

			s_label.setText(s);
		}
	}
}

public class mvqc_gui
{
	public static void main(String[] args)
	{
		if (args.length < 1) {
			System.out.println("Specify rules set rus|eng");
			return;
		}

		Rules r;
		String init = null;
		boolean ai = false;

		if (args[0].equals("rus"))
			r = Rules.make_rus_rules();
		else if (args[0].equals("eng"))
			r = Rules.make_eng_rules();
		else {
			System.out.println("Unknown rules");
			return;
		}

		if (args.length > 1 && args[1].equals("ai"))
			ai = true;

		GUI gui = new GUI(r, init, ai);
	}
}
