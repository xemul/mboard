import java.util.ArrayList;

public class Game
{
	public final static int size = 15;
	public int[][] board;
	public int turn;

	public Game()
	{
		board = new int[Game.size][Game.size];
		turn = 1;
	}

	public int wave_at(int row, int col)
	{
		return board[row][col];
	}

	private void update_score(Score s, int r, int c, ArrayList<Score> wins)
	{
		int v = board[r][c];

		if (v == 0 || (v > 0 && s.v < 0) || (v < 0 && s.v > 0)) {
			/* End of sequence. */
			stop_score(s, r, c, wins);

			s.v = 0;
			s.start_r = r;
			s.start_c = c;
		}

		s.v += v;
	}

	private void stop_score(Score s, int r, int c, ArrayList<Score> wins)
	{
		if (s.v >= 5 || s.v <= -5)
			wins.add(new Score(s, r, c));
	}

	public boolean check_win(UI ui)
	{
		int i, j;
		Score s0, s1, s2, s3;
		ArrayList<Score> wins = new ArrayList<Score>();

		/*
		 * Game over is when we have a sequence of squares with
		 * the total "power" of 5 in either direction.
		 */

		/*
		 * First loop -- horizontal and vertical lines
		 */
		for (i = 0; i < size; i++) {
			s0 = new Score(i, 0);
			s1 = new Score(0, i);

			for (j = 0; j < size; j++) {
				update_score(s0, i, j, wins);
				update_score(s1, j, i, wins);
			}

			stop_score(s0, i, size - 1, wins);
			stop_score(s1, size - 1 , i, wins);
		}

		/*
		 * Second loop -- diagonals. All 4 types
		 */

		for (i = 0; i < size; i++) {
			s0 = new Score(i, 0);
			s1 = new Score(size - 1 - i, 0);
			s2 = new Score(size - 1, size - 1 - i);
			s3 = new Score(0, size - 1 - i);

			for (j = 0; j <= i; j++) {
				update_score(s0, i - j, j, wins);
				update_score(s1, size - 1 - (i - j), j, wins);

				/* Don't double central diagonals */
				if (i != size - 1) {
					update_score(s2, size - 1 - j, size - 1 - (i - j), wins);
					update_score(s3, j, size - 1 - (i - j), wins);
				}
			}

			stop_score(s0, 0, i, wins);
			stop_score(s1, size - 1, i, wins);

			if (i != size - 1) {
				stop_score(s2, size - 1 - i, size - 1, wins);
				stop_score(s3, i, size - 1, wins);
			}
		}

		if (wins.size() == 0)
			return false;

		ui.game_over(wins);
		return true;
	}

	/*
	 * Places a wave on the board. The br:bc pair is row and column
	 * of the wave beginning (maximum), the er:ec is row and column
	 * of the wave ending (minimum).
	 */

	public void move(int br, int bc, int er, int ec, UI ui)
	{
		int r_inc = br - er;
		int c_inc = bc - ec;
		int r, c, v;
		boolean flip = false;

		if (!(r_inc == 0 || c_inc == 0 || r_inc == c_inc || r_inc == -c_inc)) {
			ui.invalid_move();
			return;
		}

		board[br][bc] += turn;

		/*
		 * Now run the wave. In the simpler version we have discrete
		 * one -- only +1's and -1's. In continuous version we can
		 * add pure floats to the cells %)
		 */
		v = -turn;
		r = br;
		c = bc;

		while (true) {
			r += r_inc;
			c += c_inc;

			if (!(r >= 0 && r < size && c >= 0 && c < size)) {
				if (flip)
					break;

				v = -turn;
				flip = true;
				r = br;
				c = bc;
				r_inc = -r_inc;
				c_inc = -c_inc;
				continue;
			}

			board[r][c] += v;
			v = -v;
		}

		if (check_win(ui))
			return;

		turn = -turn;
	}
}
