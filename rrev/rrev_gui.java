import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*
 * GUI for experiments
 */

class GUI extends JFrame implements UI, MouseListener
{
	private final static int fw = 50;
	private final static int brd = 10;
	private final static int voff = 50;

	private int board_size;
	private int win_w;
	private int win_h;

	private Game game;
	private Label s_label;

	private int view_r;
	private int view_c;

	private boolean game_over;
	private int white_score;
	private int black_score;

	public void game_over(int white_score, int black_score)
	{
		game_over = true;
		this.white_score = white_score;
		this.black_score = black_score;
	}

	public GUI()
	{
		super("Relativistic reversi");

		game_over = false;
		board_size = 8;
		win_w = board_size * fw + 2 * brd;
		win_h = board_size * fw + 2 * brd + voff;

		view_r = 0;
		view_c = 0;

		game = new Game();

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
			if (view_r == row && view_c == col)
				this.game.run_waves(this);
			else {
				view_r = row;
				view_c = col;
			}
		} else if (!game_over) {
			/*
			 * Prevent from placing disk on a cell other than
			 * the view one to avoid accidents
			 */
			if (row == view_r && col == view_c)
				this.game.move(row, col, this);
		}

		/*
		 * FIXME -- this redraws the whole screen. Need to optimize.
		 */
		repaint();
	}

	private Paint color2paint(int col)
	{
		return col > 0 ? Color.white : Color.black;
	}

	public void paint(Graphics g)
	{
		Graphics2D g2d = (Graphics2D)g;
		Color ccol = Color.green.darker();
		Color vcol = Color.green;

		g2d.setBackground(Color.black);
		g2d.clearRect(0, voff, win_w, win_h - voff);

		/*
		 * Fill the board cells
		 */
		for (int c = 0; c < board_size; c++) {
			for (int r = 0; r < board_size; r++) {
				if (c == view_c && r == view_r)
					g2d.setPaint(vcol);
				else
					g2d.setPaint(ccol);

				Polygon p = new Polygon();
				p.addPoint(col2x(c) + 1,     row2y(r) + 1);
				p.addPoint(col2x(c + 1) - 1, row2y(r) + 1);
				p.addPoint(col2x(c + 1) - 1, row2y(r + 1) - 1);
				p.addPoint(col2x(c) + 1,     row2y(r + 1) - 1);
				g.fillPolygon(p);

				int dc = this.game.disk_at(view_r, view_c, r, c);
				if (dc != 0) {
					g2d.setPaint(color2paint(dc));
					g.fillOval(col2x(c) + 3, row2y(r) + 3, fw - 6, fw - 6);
				} else {
					if (this.game.can_move_to(r, c)) {
						g2d.setPaint(color2paint(this.game.turn));
						g.drawOval(col2x(c) + 6, row2y(r) + 6, fw - 12, fw - 12);
					}
				}
			}
		}

		/* Draw waves */
		ArrayList<Flip> waves = this.game.waves();
		/*
		 * Slightly pertrubate the waves within cells
		 * so that they do not overlap with each other
		 */
		int wave_off = 3;
		for (Flip wave: waves) {
			int dist = wave.dist - 1;

			if (dist <= 0)
				continue;

			g2d.setPaint(color2paint(wave.color));
			g.drawRect(col2x(wave.col - dist) + wave_off, row2y(wave.row - dist) + wave_off,
					dist * 2 * fw + fw - 2 * wave_off, dist * 2 * fw + fw - 2 * wave_off);

			wave_off += 2;
			if (wave_off >= fw - 2)
				wave_off = 0;
		}

		String s;

		if (!game_over)
			s = String.format("%s move, %d disks, %d waves",
						this.game.turn > 0 ? "White" : "Black",
						this.game.disks, waves.size());
		else {
			if (white_score > black_score)
				s = String.format("White wins (%d/%d)\n",
						white_score, black_score);
			else if (white_score < black_score)
				s = String.format("Black wins (%d/%d)\n",
						black_score, white_score);
			else
				s = String.format("Game draw\n");

		}

		s += String.format(" (%d/%d w/b here)\n",
				this.game.whites_at(view_r, view_c),
				this.game.blacks_at(view_r, view_c));

		s_label.setText(s);
	}
}

public class rrev_gui
{
	public static void main(String[] args)
	{
		GUI gui = new GUI();
	}
}
