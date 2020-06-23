import java.util.ArrayList;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

interface UI
{
	void no_checker(Pos pos);
	void measure_cell(Pos pos);
	void make_move(Pos pos);
	void moved_to(Pos npos);
	void capture_at(Pos npos, Pos nnpos);
	void cell_stuck(Pos pos);

	void log(String str);
}

class Ck
{
	/*
	 * Checkers colors, KING bit and helper routines.
	 * XXX Makes sence to make checker a class, rather than int?
	 */
	final static int CNONE = 0;
	final static int WHITE = 1;
	final static int BLACK = 2;
	final static int KING = 0x8;
	final static int WHITE_K = (Ck.WHITE | Ck.KING);
	final static int BLACK_K = (Ck.BLACK | Ck.KING);

	public static int color(int ck)
	{
		return ck & ~Ck.KING;
	}

	public static boolean is_king(int ck)
	{
		return ((ck & Ck.KING) != 0 ? true : false);
	}

	public static int kingify(int ck)
	{
		return ck | Ck.KING;
	}

	public static int oppc(int ck)
	{
		if (ck == Ck.WHITE)
			return Ck.BLACK;
		if (ck == Ck.BLACK)
			return Ck.WHITE;
		throw new Error();
	}
}

class Pos
{
	final static int M_UL = 1;
	final static int M_UR = 2;
	final static int M_DL = 3;
	final static int M_DR = 4;

	public int l, n;

	public Pos(int l, int n) { this.l = l; this.n = n; }
	public String s() { return String.format("%d.%d", this.l, this.n); }

	public Pos move(int direction)
	{
		int nl;

		if (direction == Pos.M_UR || direction == Pos.M_UL) {
			if (this.l == 0)
				return null;
			nl = this.l - 1;
		} else {
			if (this.l == 7)
				return null;
			nl = this.l + 1;
		}

		if (this.l % 2 == 0) {
			/*
			 * Even line, left move is nr == nr,
			 * right move is +1 and possible for 0, 1 and 2
			 */
			if (direction == Pos.M_UL || direction == Pos.M_DL)
				return new Pos(nl, this.n);
			else if (this.n < 3)
				return new Pos(nl, this.n + 1);
			else
				return null;
		} else {
			/*
			 * Odd line, right move is nr == nr,
			 * lefot move is -1 and possible for 1, 2 and 3
			 */
			if (direction == Pos.M_UR || direction == Pos.M_DR)
				return new Pos(nl, this.n);
			else if (this.n > 0)
				return new Pos(nl, this.n - 1);
			else
				return null;
		}
	}

	public ArrayList<Pos> move_king(int direction)
	{
		/* Generate as many moves in one direction as possible */
		ArrayList<Pos> posl = new ArrayList<Pos>();

		Pos pos = this;
		while (true) {
			Pos npos = pos.move(direction);
			if (npos == null)
				break;

			posl.add(npos);
			pos = npos;
		}

		return posl;
	}
}

class Prob
{
	private int num, den;

	public Prob(int num, int den) { this.num = num; this.den = den; }

	private static int gcd(int x, int y)
	{
		while (y != 0) {
			int i = x;
			x = y;
			y = i % y;
		}
		return x;
	}

	private void set_and_reduce(int nnum, int nden)
	{
		int red = Prob.gcd(nnum, nden);

		if (red != 1) {
			nnum /= red;
			nden /= red;
		}

		this.num = nnum;
		this.den = nden;
	}

	public void add(Prob prob)
	{
		int nnum = this.num * prob.den + prob.num * this.den;
		int nden = this.den * prob.den;
		this.set_and_reduce(nnum, nden);
	}

	public void mul(Prob prob)
	{
		int nnum = this.num * prob.num;
		int nden = this.den * prob.den;
		this.set_and_reduce(nnum, nden);
	}

	public int[] raw()
	{
		return new int[] { this.num, this.den };
	}

	public boolean is_zero()
	{
		if (this.num == 0) {
			if (this.den != 1)
				throw new Error();
			return true;
		}

		return false;
	}

	public boolean is_one()
	{
		if (this.num == 1 && this.den == 1)
			return true;
		if (this.num == this.den)
			throw new Error();
		return false;
	}

	public String s() { return String.format("%d/%d", this.num, this.den); }
	public String sx()
	{
		if (this.num < this.den)
			return this.s();

		int integ = this.num / this.den;
		int nnum = this.num - integ * this.den;

		if (nnum == 0)
			return String.format("%d", integ);
		else
			return String.format("%d %d/%d", integ, nnum, this.den);
	}
}

class Cell
{
	public Prob[] c_prob;
	private int result;

	public Cell()
	{
		/*
		 * Each cell can have checkers of both colors, so
		 * we need the probability of both
		 */

		this.c_prob = new Prob[3]; // 0 will be empty
		this.c_prob[Ck.WHITE] = new Prob(0, 1);
		this.c_prob[Ck.BLACK] = new Prob(0, 1);
		this.result = -1;
	}

	public void update(int ck, Prob prob)
	{
		/*
		 * XXX One doesn't tell kings from checkers even if
		 * making a cell measurement. Instead, both of them
		 * form a superposition and move each in its own
		 * sub board
		 */

		this.c_prob[Ck.color(ck)].add(prob);
	}

	public void measure(UI ui, Random rand)
	{
		/*
		 * We have c_prob[$color] probability of the $color checker
		 * there and need to roll the dice to decide what really is
		 * XXX: there are two things we can measure -- what is in the
		 * cell or is there the checker of the game.turn color there.
		 * In the former case we may end up in opponent's checker
		 * disappear after the measure. In the latter -- we will
		 * happen into different set of boards.
		 */

		int[] wp = this.c_prob[Ck.WHITE].raw();
		int[] bp = this.c_prob[Ck.BLACK].raw();

		// XXX -- is this sane?

		int w = wp[0] * bp[1];
		int b = bp[0] * wp[1];
		int t = bp[1] * wp[1];

		ui.log(String.format(" ? 0 .. %d .. %d .. %d", w, w + b, t));

		if (w + b > t)
			throw new Error();

		int r = rand.nextInt(t);
		if (r < w)
			this.result = Ck.WHITE;
		else if (r < w + b)
			this.result = Ck.BLACK;
		else
			this.result = Ck.CNONE;
	}

	public boolean match(int ck)
	{
		if (this.result == -1)
			throw new Error();
		return this.result == Ck.color(ck);
	}
}

class Board
{
	private int[][] f;
	private Board parent;
	private ArrayList<Board> kids;
	private int[] n_checkers;

	public Board()
	{
		this.f = new int[][] {
			{ Ck.BLACK, Ck.BLACK, Ck.BLACK, Ck.BLACK },
			{ Ck.BLACK, Ck.BLACK, Ck.BLACK, Ck.BLACK },
			{ Ck.BLACK, Ck.BLACK, Ck.BLACK, Ck.BLACK },

			{ Ck.CNONE, Ck.CNONE, Ck.CNONE, Ck.CNONE },
			{ Ck.CNONE, Ck.CNONE, Ck.CNONE, Ck.CNONE },

			{ Ck.WHITE, Ck.WHITE, Ck.WHITE, Ck.WHITE },
			{ Ck.WHITE, Ck.WHITE, Ck.WHITE, Ck.WHITE },
			{ Ck.WHITE, Ck.WHITE, Ck.WHITE, Ck.WHITE }
		};

		this.parent = null;
		this.kids = new ArrayList<Board>();
		n_checkers = new int[3];
		n_checkers[Ck.WHITE] = n_checkers[Ck.BLACK] = 12;
	}

	public boolean empty(int col)
	{
		return n_checkers[col] == 0;
	}

	public Prob prob()
	{
		/*
		 * Probability of the exact board is -- what's the
		 * chance we get to this board given on each moment
		 * in the past we had equal changes of taking any
		 * possible move
		 */

		Prob p = new Prob(1, 1);
		Board par = this.parent;
		while (par != null) {
			p.mul(new Prob(1, par.kids.size()));
			par = par.parent;
		}
		return p;
	}

	public Board fork()
	{
		Board ret = new Board();

		for (int l = 0; l < 8; l++) {
			for (int n = 0; n < 4; n++) {
				ret.f[l][n] = this.f[l][n];
			}
		}

		/* XXX actually, we can borrow parent's array */
		ret.n_checkers = new int[3];
		ret.n_checkers[Ck.WHITE] = n_checkers[Ck.WHITE];
		ret.n_checkers[Ck.BLACK] = n_checkers[Ck.BLACK];

		ret.parent = this;
		this.kids.add(ret);

		return ret;
	}

	private void drop_kid(Board kid)
	{
		this.kids.remove(kid);
		if (this.kids.isEmpty())
			this.drop();
	}

	public void drop()
	{
		if (!this.kids.isEmpty())
			throw new Error();

		Board par = this.parent;
		if (par != null) {
			this.parent = null;
			par.drop_kid(this);
		} else
			throw new Error();
	}

	public int get(Pos pos)  { return this.f[pos.l][pos.n]; }
	public int getc(Pos pos) { return Ck.color(this.get(pos)); }
	public void set(Pos pos, int ck) { this.f[pos.l][pos.n] = ck; }

	public void move_at(int turn, Pos pos,
			ArrayList<Board> new_mvb,
			ArrayList<Board> stuck_mvb,
			ArrayList<Board> othc_mv, UI ui)
	{
		/*
		 * Here we're on the "real" board w/o any quantum effects.
		 *
		 * Checker at line.nr is of 'turn' color, this should
		 * have been checked by the caller.
		 *
		 * XXX Checkers rule -- if you can capture, you must do it.
		 * But this code does a valid capture/move by a given checker,
		 * it does _not_ check whether we make a move, while some
		 * other checker has chance ot capture.
		 * However, once we do it we'll get into a catch -- what if
		 * on one bard a move is possible, but on another it's blocked
		 * due to some other one can do a capture?
		 * This issue is related to the one -- what to do in the
		 * situation when we could move on some boards and couldn't
		 * on anothers?
		 * So for now we relax this basic checkers rule -- player can
		 * move with any checker, but if this particulat checker can
		 * capture -- it _must_ do it.
		 */

		int ck = this.get(pos);
		if (Ck.color(ck) != turn)
			throw new Error();

		/* First -- try to capture something */
		ui.log(String.format("Try capture from %s", pos.s()));
		if (this.try_to_capture(ck, pos, new_mvb, ui))
			return;

		/*
		 * Second -- check whether any other checker can
		 * capture. If it can -- we can't move further.
		 */
		ui.log(String.format("Check for move possibility"));
		if (!this.check_can_move(ck, pos, ui)) {
			othc_mv.add(this);
			return;
		}

		/* Finally -- try to move regularly */
		ui.log(String.format("Try move from %s", pos.s()));
		if (this.try_to_move(ck, pos, new_mvb, ui))
			return;

		/* No moves possible at this board.pos pair */
		ui.log("Board stuck");
		stuck_mvb.add(this);
	}

	/* Cell after peer is not available for move */
	private Pos landing(Pos pos, int dir)
	{
		Pos apos = pos.move(dir);
		if (apos == null || this.get(apos) != Ck.CNONE)
			return null;
		else
			return apos;
	}

	private boolean try_to_capture_at(int ck, Pos pos, Pos npos, int dir, ArrayList<Board> new_mvb, UI ui)
	{
		Pos nnpos = landing(npos, dir);
		if (nnpos == null)
			return false;

		int peer_c = Ck.oppc(Ck.color(ck));

		/*
		 * OK, we can move from @pos, capture the peer at @npos
		 * and move to @nnpos
		 *
		 * XXX This may not be the final capture, but this new board will
		 * be in the tree. And since we get board probabilities using
		 * this tree, the probability of the L-th captures will be 2^(-L),
		 * i.e. the longer the capture path is the lower probability to
		 * find the checker there we'll have.
		 *
		 * We can do it the other way -- not fork this board, but copy one
		 * and graft into tree below when doing new_mvb.append. This will
		 * generate all capture paths with 1/N probability each.
		 */

		if (Ck.is_king(ck)) {
			ArrayList<Pos> nnposl = npos.move_king(dir);
			/*
			 * A king can land on any cell after the @npos, but
			 * once we meet non empty cell (next one is free) we
			 * should stop
			 */
			for (Pos nnpos2 : nnposl) {
				if (this.get(nnpos2) != Ck.CNONE)
					break;

				ui.capture_at(npos, nnpos2);

				Board nb = this.fork();
				nb.set(pos, Ck.CNONE);
				nb.set(npos, Ck.CNONE);
				nb.n_checkers[peer_c] -= 1;

				if (!nb.try_to_capture(ck, nnpos2, new_mvb, ui)) {
					nb.set(nnpos2, ck);
					new_mvb.add(nb);
					ui.moved_to(nnpos2);
				}
			}
		} else {
			ui.capture_at(npos, nnpos);
			Board nb = this.fork();
			nb.set(pos, Ck.CNONE);
			nb.set(npos, Ck.CNONE);
			nb.n_checkers[peer_c] -= 1;

			int king_line = (ck == Ck.WHITE ? 0 : 7);
			if (nnpos.l == king_line)
				ck = Ck.kingify(ck);

			if (!nb.try_to_capture(ck, nnpos, new_mvb, ui)) {
				nb.set(nnpos, ck);
				new_mvb.add(nb);
				ui.moved_to(nnpos);
			}
		}

		return true;
	}

	private boolean try_to_capture(int ck, Pos pos, ArrayList<Board> new_mvb, UI ui)
	{
		boolean didit = false;
		int col = Ck.color(ck);
		int[] dirs = { Pos.M_UL, Pos.M_UR, Pos.M_DL, Pos.M_DR };

		for (int dir : dirs) {
			if (Ck.is_king(ck)) {
				ArrayList<Pos> nposl = pos.move_king(dir);

				for (Pos npos : nposl) {
					int ncol = this.getc(npos);

					if (ncol == Ck.CNONE)
						continue;
					if (ncol == col)
						break;

					if (this.try_to_capture_at(ck, pos, npos, dir, new_mvb, ui))
						didit = true;
				}
			} else {
				int peer = Ck.oppc(col);
				Pos npos = pos.move(dir);

				if (npos != null && this.getc(npos) == peer)
					if (this.try_to_capture_at(ck, pos, npos, dir, new_mvb, ui))
						didit = true;
			}
		}

		return didit;
	}

	private boolean check_can_capture(int ck, Pos pos)
	{
		int col = Ck.color(ck);
		int[] dirs = { Pos.M_UL, Pos.M_UR, Pos.M_DL, Pos.M_DR };

		for (int dir : dirs) {
			if (Ck.is_king(ck)) {
				ArrayList<Pos> nposl = pos.move_king(dir);

				for (Pos npos : nposl) {
					int ncol = this.getc(npos);

					if (ncol == Ck.CNONE)
						continue;
					if (ncol == col)
						break;

					if (landing(npos, dir) != null)
						return true;
				}
			} else {
				int peer = Ck.oppc(col);
				Pos npos = pos.move(dir);

				if (npos != null && this.getc(npos) == peer && landing(npos, dir) != null)
					return true;
			}
		}

		return false;
	}

	private boolean check_can_move(int ck, Pos pos, UI ui)
	{
		int col = Ck.color(ck);

		/*
		 * This checks whether any other checker has
		 * possibility to capture.
		 */

		for (int l = 0; l < 8; l++) {
			for (int n = 0; n < 4; n++) {
				Pos p = new Pos(l, n);
				int ock = get(p);

				if (Ck.color(ock) != col)
					continue;
				if (l == pos.l && n == pos.n)
					continue;

				if (check_can_capture(ock, p))
					return false;
			}
		}

		return true;
	}

	private boolean try_to_move(int ck, Pos pos, ArrayList<Board> new_mvb, UI ui)
	{
		boolean didit = false;

		if (Ck.is_king(ck)) {
			int[] dirs = { Pos.M_UR, Pos.M_UL, Pos.M_DR, Pos.M_DL };

			for (int dir: dirs) {
				ArrayList<Pos> nposl = pos.move_king(dir);
				for (Pos npos: nposl) {
					/*
					 * This is move. If we hit a checker we stop,
					 * even if we can capture one, since checks
					 * for capture has been already made
					 */
					if (this.get(npos) != Ck.CNONE)
						break;

					Board nb = this.fork();
					nb.set(pos, Ck.CNONE);
					nb.set(npos, ck);
					new_mvb.add(nb);
					ui.moved_to(npos);
					didit = true;
				}
			}

		} else {
			int[] dirs;
			int king_line;

			if (ck == Ck.WHITE) {
				dirs = new int[] { Pos.M_UR, Pos.M_UL };
				king_line = 0;
			} else {
				dirs = new int[] { Pos.M_DR, Pos.M_DL };
				king_line = 7;
			}

			for (int dir: dirs) {
				Pos npos = pos.move(dir);
				if (npos != null && this.get(npos) == Ck.CNONE) {
					Board nb = this.fork();
					nb.set(pos, Ck.CNONE);
					if (npos.l == king_line)
						ck = Ck.kingify(ck);
					nb.set(npos, ck);
					new_mvb.add(nb);
					ui.moved_to(npos);
					didit = true;
				}
			}
		}

		return didit;
	}
}

class Game
{
	public int turn;
	public ArrayList<Board> mvb;
	private Random rand;

	public Game()
	{
		this.turn = Ck.WHITE;
		this.mvb = new ArrayList<Board>();
		this.mvb.add(new Board());
		this.rand = new Random();
	}

	private void tune_multiverse(Pos pos, Cell cell)
	{
		ArrayList<Board> nmvb = new ArrayList<Board>();

		for (Board b: this.mvb) {
			int ck = b.get(pos);
			if (cell.match(ck))
				nmvb.add(b);
			else
				b.drop();
		}
		this.mvb = nmvb;
	}

	public Cell superposition(Pos pos)
	{
		Cell c = new Cell();
		for (Board b : this.mvb) {
			int ck = b.get(pos);
			if (ck != Ck.CNONE)
				c.update(ck, b.prob());
		}

		return c;
	}

	public String stats()
	{
		Prob wp = new Prob(0, 1);
		Prob bp = new Prob(0, 1);

		/* Probabilities to meet a checker */
		for (int l = 0; l < 8; l++) {
			for (int n = 0; n < 4; n++) {
				Cell c = superposition(new Pos(l, n));
				wp.add(c.c_prob[Ck.WHITE]);
				bp.add(c.c_prob[Ck.BLACK]);
			}
		}

		/* Probabilities to have no checkers %) */
		Prob wl = new Prob(0, 1);
		Prob bl = new Prob(0, 1);
		for (Board b: mvb) {
			if (b.empty(Ck.WHITE) || b.empty(Ck.BLACK)) {
				Prob p = b.prob();
				if (b.empty(Ck.WHITE))
					wl.add(p);
				if (b.empty(Ck.BLACK))
					bl.add(p);
			}
		}

		return String.format("mvb=%d w(p=%s e=%s) b(p=%s e=%s)",
				mvb.size(), wp.sx(), wl.sx(), bp.sx(), bl.sx());
	}

	public void move(int line, int nr, UI ui)
	{
		Pos pos = new Pos(line, nr);
		Cell cell = this.superposition(pos);
		Prob prob = cell.c_prob[this.turn];

		/* First -- check whether our checker can be in this cell at all */
		if (prob.is_zero()) {
			ui.no_checker(pos);
			return;
		}

		/* Next -- find out whether it's 100% there or not */
		if (!prob.is_one()) {
			/* Need to take the measurement and decide what's really there */
			ui.measure_cell(pos);
			cell.measure(ui, this.rand);

			/*
			 * After we know whether the checker is there or
			 * not -- update the multiverse to keep only those,
			 * that have this checker in this cell
			 */
			this.tune_multiverse(pos, cell);

			if (!cell.match(this.turn)) {
				/* XXX -- should be pass the move to peer? */
				ui.no_checker(pos);
				return;
			}
		}

		ui.make_move(pos);

		/* Try to move the checker on each board we have in the multiverse */
		ArrayList<Board> newmv = new ArrayList<Board>();
		ArrayList<Board> stkmv = new ArrayList<Board>();
		ArrayList<Board> othmv  = new ArrayList<Board>();

		for (Board b : this.mvb)
			b.move_at(this.turn, pos, newmv, stkmv, othmv, ui);

		/*
		 * XXX There are three possibilities of move attempt on
		 * the real board (FIXME -- one is not checked currently)
		 * 1 -- checker moved
		 * 2 -- checker locked (stuck)
		 * 3 -- checker cannot move (some other one should capture)
		 *
		 * In multiverse we can hit combinations
		 *
		 * move  lock   cant      result
		 *  +     -      -         move
		 *  -     +      -         try again
		 *  -     -      +         try again
		 *
		 *  -     +      +         try again
		 *
		 *  +     +      -         ?
		 *  +     -      +         ?
		 *  +     +      +         ?
		 *
		 * We can't do any definite decisions here and it's an
		 * open question what to do in this case :(
		 *
		 * Current decision is to make "move" be preferred over
		 * anything else and lock/cant boards are just dropped.
		 */

		if (newmv.isEmpty()) {
			/*
			 * We were unable to move at all. This is possible, so
			 * just ask the player to pick another cell.
			 *
			 * XXX Still, it may have happened that we've just made the
			 * cell measurement. This means, that the player just
			 * got some more information, but kept the turn :\ This is
			 * similar to the situation when we measured the cell and
			 * didn't find our checker in it.
			 */
			ui.cell_stuck(pos);
			return;
		}

		/*
		 * XXX Some boards get stuck. We should (probably) measure the
		 * probabilities of having non-stuck boards and either trim
		 * the stuck ones from the multiverse, or stuck the whole move
		 * (trimming the valid boards from it)
		 */

		for (Board b: stkmv)
			b.drop();
		for (Board b: othmv)
			b.drop();

		this.mvb = newmv;
		this.turn = Ck.oppc(this.turn);
	}
}

/*
 **************************
 * Sanity tests for moves *
 **************************
 */

class Nilui implements UI
{
	public void no_checker(Pos pos) { }
	public void measure_cell(Pos pos) { }
	public void make_move(Pos pos) { }
	public void cell_stuck(Pos pos) { }
	public void moved_to(Pos npos) { System.out.format("-> %s\n", npos.s()); }
	public void capture_at(Pos npos, Pos nnpos) { System.out.format("-(%s)>%s\n", npos.s(), nnpos.s()); }
	public void log(String str) { }
}

class Test
{
	static boolean chk_res(ArrayList<Board> n, int nn, ArrayList<Board>s, int ns)
	{
		if (n.size() != nn) {
			System.out.println("`- FAIL result boards");
			return false;
		}

		if (s.size() != ns) {
			System.out.println("`- FAIL stuck boards");
			return false;
		}

		return true;
	}

	static boolean pop_comb(ArrayList<Board> l, int[][] combs, int nc)
	{
		for (Board b: l) {
			boolean found = true;

			for (int c = 0; c < nc; c++) {
				if (b.get(new Pos(combs[c][0], combs[c][1])) != combs[c][2])
					found = false;
			}

			if (!found)
				continue;

			l.remove(b);
			return true;
		}

		System.out.println("`- FAIL comb not found");
		return false;
	}

	static boolean one_move(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one quantum move");

		b.move_at(Ck.WHITE, new Pos(5, 1), n, s, s, ui);
		if (!chk_res(n, 2, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 5, 1, Ck.CNONE },
					{ 4, 0, Ck.WHITE },
					{ 4, 1, Ck.CNONE } }, 3))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 5, 1, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 4, 1, Ck.WHITE } }, 3))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean two_moves(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> i = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test two quantum moves");

		b.move_at(Ck.WHITE, new Pos(5, 1), i, s, s, ui);
		i.get(0).move_at(Ck.WHITE, new Pos(5, 2), n, s, s, ui);
		i.get(1).move_at(Ck.WHITE, new Pos(5, 2), n, s, s, ui);
		if (!chk_res(n, 3, s, 0))
			return false;

		if (!pop_comb(n, new int[][] { 
					{ 5, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 4, 0, Ck.WHITE },
					{ 4, 1, Ck.WHITE },
					{ 4, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] { 
					{ 5, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 4, 0, Ck.WHITE },
					{ 4, 1, Ck.CNONE },
					{ 4, 2, Ck.WHITE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] { 
					{ 5, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 4, 1, Ck.WHITE },
					{ 4, 2, Ck.WHITE } }, 5))
			return false;

		return true;
	}

	static boolean stuck_move(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test stuck move");

		b.move_at(Ck.WHITE, new Pos(6, 0), n, s, s, ui);
		if (!chk_res(n, 0, s, 1))
			return false;

		return true;
	}

	static boolean one_capt(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one capture");

		b.set(new Pos(5, 0), Ck.CNONE);
		b.set(new Pos(3, 1), Ck.WHITE);
		b.move_at(Ck.BLACK, new Pos(2, 0), n, s, s, ui);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 2, 0, Ck.CNONE },
					{ 4, 1, Ck.BLACK },
					{ 3, 1, Ck.CNONE } }, 4))
			return false;

		return true;
	}

	static boolean two_capt(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test two captures");

		b.set(new Pos(6, 0), Ck.CNONE);
		b.set(new Pos(3, 1), Ck.WHITE);
		b.move_at(Ck.BLACK, new Pos(2, 0), n, s, s, ui);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 6, 0, Ck.BLACK } }, 5))
			return false;

		return true;
	}

	static boolean one_qapt(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test quantum capture");

		b.set(new Pos(6, 0), Ck.CNONE);
		b.set(new Pos(3, 1), Ck.WHITE);
		b.set(new Pos(6, 2), Ck.CNONE);
		b.set(new Pos(4, 3), Ck.WHITE);
		b.move_at(Ck.BLACK, new Pos(2, 0), n, s, s, ui);
		if (!chk_res(n, 2, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 6, 0, Ck.BLACK },
					{ 5, 2, Ck.WHITE },
					{ 6, 2, Ck.CNONE } }, 7))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 1, Ck.WHITE },
					{ 6, 0, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 6, 2, Ck.BLACK } }, 7))
			return false;

		return true;
	}

	static boolean goto_king(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test move to king");

		b.set(new Pos(1, 0), Ck.WHITE);
		b.set(new Pos(0, 0), Ck.CNONE);
		b.set(new Pos(2, 0), Ck.CNONE);
		b.move_at(Ck.WHITE, new Pos(1, 0), n, s, s, ui);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 1, 0, Ck.CNONE },
					{ 0, 0, Ck.WHITE_K } }, 2))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean king_move(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one king move");

		b.set(new Pos(5, 1), Ck.WHITE_K);
		b.move_at(Ck.WHITE, new Pos(5, 1), n, s, s, ui);
		if (!chk_res(n, 4, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.WHITE_K },
					{ 4, 0, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 3, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 4, 0, Ck.WHITE_K },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 3, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.WHITE_K },
					{ 3, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 3, 2, Ck.WHITE_K } }, 5))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean king_qapt(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test double king qapture");

		b.set(new Pos(0, 2), Ck.CNONE);
		b.set(new Pos(1, 2), Ck.CNONE);
		b.set(new Pos(5, 0), Ck.WHITE_K);
		b.move_at(Ck.WHITE, new Pos(5, 0), n, s, s, ui);
		if (!chk_res(n, 3, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 0, 2, Ck.WHITE_K },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.BLACK },
					{ 2, 1, Ck.CNONE },
					{ 3, 3, Ck.CNONE },
					{ 4, 3, Ck.CNONE } }, 6))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 0, 2, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 2, 1, Ck.CNONE },
					{ 3, 3, Ck.WHITE_K },
					{ 4, 3, Ck.CNONE } }, 6))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 0, 2, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 2, 1, Ck.CNONE },
					{ 3, 3, Ck.CNONE },
					{ 4, 3, Ck.WHITE_K } }, 6))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean king_capt(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one king long capture");

		b.set(new Pos(1, 0), Ck.CNONE);
		b.set(new Pos(5, 2), Ck.WHITE_K);
		b.move_at(Ck.WHITE, new Pos(5, 2), n, s, s, ui);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 1, 0, Ck.WHITE_K },
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE } }, 5))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean capt_n_kapt(Nilui ui)
	{
		Board b = new Board();
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test capture -> king -> capture");

		b.set(new Pos(0, 1), Ck.CNONE);
		b.set(new Pos(1, 2), Ck.CNONE);
		b.set(new Pos(2, 0), Ck.WHITE);
		b.move_at(Ck.WHITE, new Pos(2, 0), n, s, s, ui);
		if (!chk_res(n, 2, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 1, 1, Ck.CNONE },
					{ 0, 1, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 3, 3, Ck.WHITE_K },
					{ 4, 3, Ck.CNONE } }, 7))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 1, 1, Ck.CNONE },
					{ 0, 1, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 3, 3, Ck.CNONE },
					{ 4, 3, Ck.WHITE_K } }, 7))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static void run()
	{
		Nilui ui = new Nilui();

		if (!one_move(ui))
			return;
		if (!two_moves(ui))
			return;
		if (!stuck_move(ui))
			return;
		if (!one_capt(ui))
			return;
		if (!two_capt(ui))
			return;
		if (!one_qapt(ui))
			return;
		if (!goto_king(ui))
			return;
		if (!king_move(ui))
			return;
		if (!king_capt(ui))
			return;
		if (!king_qapt(ui))
			return;
		if (!capt_n_kapt(ui))
			return;

		System.out.println("PASS");
	}
}

/*
 ******************
 * Stupid CLI API *
 ******************
 */

class CLI implements UI
{
	public void no_checker(Pos pos)
	{
		System.out.format("No checker at %s\n", pos.s());
	}
	public void measure_cell(Pos pos)
	{
		System.out.format("Need to measure the cell at %s\n", pos.s());
	}
	public void make_move(Pos pos)
	{
		System.out.format("Moving from %s\n", pos.s());
	}
	public void cell_stuck(Pos pos)
	{
		System.out.format("No move possible from %s\n", pos.s());
	}
	public void moved_to(Pos npos)
	{
		System.out.format("`--> %s\n", npos.s());
	}
	public void capture_at(Pos npos, Pos nnpos)
	{
		System.out.format("`-(%s)-> %s\n", npos.s(), nnpos.s());
	}

	public void log(String str)
	{
		System.out.println(str);
	}

	private void show_board(Game g)
	{
		int n;
		final String board = "-------------";
		final String space = "             ";
		final String txfmt = "%13s";

		for (int l = 0; l < 8; l++) {
			boolean even = (l % 2 == 0);

			for (n = 0; n < 8; n++)
				System.out.format("+" + board);
			System.out.format("+\n");
			for (n = 0; n < 8; n++)
				System.out.format("|" + space);
			System.out.format("|\n");

			for (n = 0; n < 4; n++) {
				Pos pos = new Pos(l, n);
				Cell c = g.superposition(pos);

				if (even)
					System.out.format("|" + space);

				String s = "";
				Prob p;

				p = c.c_prob[Ck.WHITE];
				if (!p.is_zero())
					s += String.format("W%s", p.s());
				s += ":";
				p = c.c_prob[Ck.BLACK];
				if (!p.is_zero())
					s += String.format("B%s", p.s());

				System.out.format("|" + txfmt, s);

				if (!even)
					System.out.format("|" + space);
			}

			System.out.format("| %d\n", l);
			for (n = 0; n < 8; n++)
				System.out.format("|" + space);
			System.out.format("|\n");
		}

		for (n = 0; n < 8; n++)
			System.out.format("+" + board);
		System.out.format("+ %s move\n", g.turn == Ck.BLACK ? 'B' : 'W');
	}

	public void run(Game g)
	{
		System.out.println("Hello");

		while (true) {
			String in = System.console().readLine();
			if (in == null) {
				break;
			}

			String cmd[] = in.split(" ");
			if (cmd.length >= 1) {
				if (cmd[0].equals("x"))
					break;
				if (cmd[0].equals("s")) {
					show_board(g);
					continue;
				}
				if (cmd[0].equals("m")) {
					g.move(Integer.parseInt(cmd[1]), Integer.parseInt(cmd[2]), this);
					continue;
				}
			}

			System.out.println("x        for quit");
			System.out.println("s        for show");
			System.out.println("m %l %n  for move");
		}

		System.out.println("Bye");
	}
}

/*
 * GUI for experiments
 */

class GUI extends JFrame implements UI, MouseListener
{
	public void no_checker(Pos pos) { }
	public void measure_cell(Pos pos) { }
	public void make_move(Pos pos) { }
	public void cell_stuck(Pos pos) { }
	public void moved_to(Pos npos) { }
	public void capture_at(Pos npos, Pos nnpos) { }
	public void log(String str) { }

	private final static int fw = 50;
	private final static int brd = 10;
	private final static int voff = 50;
	private final static int win_w = 8 * fw + 2 * brd;
	private final static int win_h = 8 * fw + 2 * brd + voff;

	private Game game;
	private Label s_label;

	public GUI(Game g)
	{
		super("Quantum Checkers");
		this.game = g;
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
		int x = e.getX();
		int y = e.getY();

		int col = (y - voff - brd) / fw;
		int row = (x - brd) / fw;

		/*
		 * Now turn this into Board's cell number 
		 * XXX: move this into Game.
		 */

		if (col % 2 == 0) {
			if (row % 2 == 0)
				return;
			row = (row - 1) / 2;
		} else {
			if (row % 2 == 1)
				return;
			row /= 2;
		}

		this.game.move(col, row, this);

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
		int[] f;

		/*
		 * Turn board cell number into graphics coordinates
		 */
		n *= 2;
		if (l % 2 == 0)
			n++;

		x = brd + n * fw;
		y = voff + brd + l * fw;

		p = c.c_prob[Ck.WHITE];
		if (!p.is_zero()) {
			f = p.raw();
			g2d.setPaint(Color.white);
			angle = 360 * f[0] / f[1];
			g.fillArc(x + 3, y + 3, fw - 6, fw - 6, 30, angle);
		}

		p = c.c_prob[Ck.BLACK];
		if (!p.is_zero()) {
			f = p.raw();
			angle = 360 * f[0] / f[1];
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
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
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

		for (int l = 0; l < 8; l++) {
			for (int n = 0; n < 4; n++) {
				Pos p = new Pos(l, n);
				Cell c = this.game.superposition(p);
				draw_checker(g, l, n, c);
			}
		}

		String s_text = "";

		/* And show who's move it is */
		g2d.setPaint(Color.green);
		g2d.setStroke(new BasicStroke(3));
		if (this.game.turn == Ck.WHITE) {
			g.drawLine(brd, win_h - brd - 1, win_w - brd, win_h - brd - 1);
			s_text += "Turn: W ";
		} else {
			g.drawLine(brd, voff + brd - 1, win_w - brd, voff + brd - 1);
			s_text += "Turn: B ";
		}

		s_text += String.format(" Stats: %s", this.game.stats());

		s_label.setText(s_text);
	}
}

public class mvqc
{
	public static void main(String[] args)
	{
		Game g = new Game();

		if (args.length < 1) {
			System.out.println("Specify UI type cli, gui or test");
			return;
		}

		if (args[0].equals("cli")) {
			CLI cli = new CLI();
			cli.run(g);
			return;
		}

		if (args[0].equals("gui")) {
			GUI gui = new GUI(g);
			return;
		}

		if (args[0].equals("test")) {
			Test.run();
			return;
		}
	}
}
