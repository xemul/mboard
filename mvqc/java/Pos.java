import java.util.ArrayList;

public class Pos
{
	public final static int M_UL = 1;
	public final static int M_UR = 2;
	public final static int M_DL = 3;
	public final static int M_DR = 4;
	public final static int[] all_moves = { M_UL, M_UR, M_DL, M_DR };
	public final static int[] down_moves = { M_DL, M_DR };
	public final static int[] up_moves = { M_UL, M_UR };

	public int l, n;

	public Pos(int l, int n) { this.l = l; this.n = n; }
	public String s() { return String.format("%d.%d", l, n); }

	public Pos move(int direction, int board_size)
	{
		int nl;

		if (direction == Pos.M_UR || direction == Pos.M_UL) {
			if (l == 0)
				return null;
			nl = l - 1;
		} else {
			if (l == board_size - 1)
				return null;
			nl = l + 1;
		}

		if (l % 2 == 0) {
			/*
			 * Even line, left move is nr == nr,
			 * right move is +1 and possible for all but last cell
			 */
			if (direction == Pos.M_UL || direction == Pos.M_DL)
				return new Pos(nl, n);
			else if (n < board_size / 2 - 1)
				return new Pos(nl, n + 1);
			else
				return null;
		} else {
			/*
			 * Odd line, right move is nr == nr,
			 * lefot move is -1 and possible for all but 0th cell
			 */
			if (direction == Pos.M_UR || direction == Pos.M_DR)
				return new Pos(nl, n);
			else if (n > 0)
				return new Pos(nl, n - 1);
			else
				return null;
		}
	}

	public ArrayList<Pos> fly_move(int direction, int board_size)
	{
		/* Generate as many moves in one direction as possible */
		ArrayList<Pos> posl = new ArrayList<Pos>();

		Pos pos = this;
		while (true) {
			Pos npos = pos.move(direction, board_size);
			if (npos == null)
				break;

			posl.add(npos);
			pos = npos;
		}

		return posl;
	}
}


