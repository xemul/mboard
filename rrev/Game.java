import java.util.ArrayList;

class Disk
{
	public static final int EMPTY = 0;
	public static final int BLACK = -1;
	public static final int WHITE = 1;

	public static int opp(int c) { return -c; }
}

class Cell
{
	/*
	 * That's the board how it's seen by the cell we want to
	 * place disk onto. And the amount of whites and blacks
	 * on it.
	 */
	private int[][] f;
	public int ws, bs;

	/*
	 * This field is ... special. It shows whether the player that
	 * moves next (Game.turn) can place its disk on the current cell.
	 */
	public boolean can_place;

	public Cell()
	{
		this.f = new int[Game.size][Game.size];
		this.f[3][3] = Disk.WHITE;
		this.f[3][4] = Disk.BLACK;
		this.f[4][3] = Disk.BLACK;
		this.f[4][4] = Disk.WHITE;
		this.ws = this.bs = 2;
	}

	public int get(int r, int c) { return f[r][c]; }
	public void set(int r, int c, int col)
	{
		f[r][c] = col;
		if (col == Disk.WHITE)
			this.ws++;
		else
			this.bs++;
	}

	private int can_flip(int row, int dr, int col, int dc, int color)
	{
		int r;
		int c;
		int dist = 0;
		int op = Disk.opp(color);

		/*
		 * Check whether we have a disk of the same color in dr:dc
		 * direction starting from row:col and flip the disks in
		 * between.
		 */

		r = row + dr;
		c = col + dc;
		while (true) {
			if (!(Game.in(r) && Game.in(c)))
				return 0;

			int ncolor = f[r][c];
			if (ncolor == color)
				/* Us -- stop and check/start flipping */
				break;
			else if (ncolor != op)
				/* Empty -- no move possible */
				return 0;

			/* Opponent -- can flip one */
			r += dr;
			c += dc;
			dist++;
		}

		return dist;
	}

	private boolean try_to_flip(int row, int dr, int col, int dc, int color)
	{
		int r;
		int c;
		int dist;

		dist = can_flip(row, dr, col, dc, color);
		if (dist == 0)
			return false;

		/*
		 * OK, this cell "knows" that disk can be placed here.
		 * Place one, update cell's knowledge about the flipped
		 * disks and start propagating the info about new disk
		 * appearred to the adjacent cells.
		 */

		if (color == Disk.WHITE) {
			bs -= dist;
			ws += dist;
		} else {
			bs += dist;
			ws -= dist;
		}

		r = row + dr;
		c = col + dc;
		while (dist > 0) {
			f[r][c] = color;
			r += dr;
			c += dc;
			dist--;
		}

		return true;
	}

	public boolean try_to_place_disk(int row, int col, int color)
	{
		boolean placed = false;
		int[] deltas = { -1, 0, 1 };

		for (int dr: deltas)
			for (int dc: deltas) {
				if (dr == 0 && dc == 0)
					continue;
				if (try_to_flip(row, dr, col, dc, color))
					placed = true;
			}

		return placed;
	}

	public boolean can_place_disk(int row, int col, int color)
	{
		int[] deltas = { -1, 0, 1 };

		if (f[row][col] != 0)
			return false;

		for (int dr: deltas)
			for (int dc: deltas) {
				if (dr == 0 && dc == 0)
					continue;
				if (can_flip(row, dr, col, dc, color) != 0)
					return true;
			}

		return false;
	}
}

public class Game
{
	public static final int size = 8;
	public static boolean in(int v) { return (v >= 0 && v < size); }

	private Cell[][] board;
	public int turn;
	public int disks;
	private ArrayList<Flip> waves;

	public Game()
	{
		board = new Cell[size][size];
		for (int r = 0; r < size; r++)
			for (int c = 0; c < size; c++)
				board[r][c] = new Cell();

		board[3][2].can_place = true;
		board[2][3].can_place = true;
		board[4][5].can_place = true;
		board[5][4].can_place = true;

		turn = Disk.BLACK;
		waves = new ArrayList<Flip>();
		disks = 4;
	}

	private boolean touch(int r, int c, Flip f)
	{
		Cell cell = this.board[r][c];

		/*
		 * At this place there cannot be any disk, as in each cell's
		 * view disks may only appear as the result of information
		 * propagation OR turnover.
		 */
		if (cell.get(f.row, f.col) != Disk.EMPTY)
			throw new Error();

		/*
		 * Cell at r:c now knows that a disk ___was___ placed
		 * some time ago at f.row:f.col position. Update
		 * the cell's knowledge of the world %)
		 */

		cell.try_to_place_disk(f.row, f.col, f.color);

		/*
		 * XXX: It might have happened that the newly appeared
		 * disk cannot be placed here since the cell at r:c sees
		 * different state as compared to the f's one.
		 *
		 * That's an alien disk from the r:c point of view, it's
		 * not clear what to do about it :) One of the options is
		 * to declare this wave as invalid and stop running one,
		 * but for now we just update the cell with "there's a disk
		 * here" info and continue running the wave.
		 */

		cell.set(f.row, f.col, f.color);

		return true;
	}

	private void run_wave(Flip f, ArrayList<Flip> n_waves)
	{
		int r, c;
		boolean more = false;

		/*
		 * Pick all cells at distance f.dist and add the disk there
		 */

		/* Upper */
		r = f.row - f.dist;
		if (r >= 0) {
			for (c = f.col - f.dist; c < f.col + f.dist; c++)
				if (in(c))
					more = touch(r, c, f);
		}

		/* Lower */
		r = f.row + f.dist;
		if (r < size) {
			for (c = f.col - f.dist + 1; c <= f.col + f.dist; c++)
				if (in(c))
					more = touch(r, c, f);
		}

		/* Left */
		c = f.col - f.dist;
		if (c >= 0) {
			for (r = f.row - f.dist + 1; r <= f.row + f.dist; r++)
				if (in(r))
					more = touch(r, c, f);
		}

		/* Right */
		c = f.col + f.dist;
		if (c < size) {
			for (r = f.row - f.dist; r < f.row + f.dist; r++)
				if (in(r))
					more = touch(r, c, f);
		}

		if (more) {
			/*
			 * We've done progress this time, maybe we'll be
			 * able to do it later. But maybe not, some nicer
			 * logic applies here.
			 */
			f.dist++;
			n_waves.add(f);
		}
	}

	public void run_waves(UI ui)
	{
		do_run_waves();

		/*
		 * Update the information about possibility for the
		 * current player to place disk.
		 */
		while (true) {
			if (update_cans(turn))
				/* Current player still can move */
				break;

			/*
			 * Current player cannot move. Maybe it will be able
			 * to if he runs the waves, but we cannot forsee the
			 * future, so try to give move back to the next player.
			 */

			int nturn = Disk.opp(turn);
			if (update_cans(nturn)) {
				/* Peer can move, switch */
				turn = nturn;
				break;
			}

			/*
			 * None of the players is able to move. Check
			 * whether we have waves running, advance them
			 * one step forward and try again.
			 */
			if (waves.size() != 0) {
				do_run_waves();
				continue;
			}

			/*
			 * Nobody can move, neither we have waves. Well,
			 * it's a game over!
			 */
			finish(ui);
			break;
		}
	}

	private void do_run_waves()
	{
		ArrayList<Flip> n_waves = new ArrayList<Flip>();

		for (Flip f: waves) {
			if (f.dist == 0) {
				f.dist++;
				n_waves.add(f);
				continue;
			}

			run_wave(f, n_waves);
		}

		waves = n_waves;
	}
	
	private void finish(UI ui)
	{
		int win_white = 0, win_black = 0;

		for (int r = 0; r < size; r++)
			for (int c = 0; c < size; c++) {
				Cell cell = this.board[r][c];

				if (cell.bs > cell.ws)
					win_black++;
				else if (cell.bs < cell.ws)
					win_white++;
			}

		ui.game_over(win_white, win_black);
	}

	private boolean update_cans(int turn)
	{
		boolean can = false;

		for (int r = 0; r < size; r++)
			for (int c = 0; c < size; c++) {
				Cell cell = this.board[r][c];
				cell.can_place = cell.can_place_disk(r, c, turn);
				if (cell.can_place)
					can = true;
			}

		return can;
	}

	public void move(int row, int col, UI ui)
	{
		Cell cell = this.board[row][col];

		if (cell.get(row, col) != Disk.EMPTY)
			return;

		if (cell.try_to_place_disk(row, col, turn)) {
			cell.set(row, col, turn);
			waves.add(new Flip(row, col, turn));
			disks++;

			if (disks == size * size) {
				/*
				 * All disks placed, need to run waves till
				 * the end and get the results stats
				 */

				while (waves.size() != 0)
					do_run_waves();

				finish(ui);
				return;
			}

			turn = Disk.opp(turn);

			/*
			 * Update any waves running to other cells.
			 */
			run_waves(ui);
		}
	}

	/* Check whether current player can put disk on row:col cell */
	public boolean can_move_to(int row, int col)
	{
		return this.board[row][col].can_place;
	}

	public int disk_at(int view_row, int view_col, int row, int col)
	{
		return this.board[view_row][view_col].get(row, col);
	}

	public int whites_at(int view_row, int view_col)
	{
		return this.board[view_row][view_col].ws;
	}

	public int blacks_at(int view_row, int view_col)
	{
		return this.board[view_row][view_col].bs;
	}

	public ArrayList<Flip> waves()
	{
		return new ArrayList<Flip>(this.waves);
	}
}
