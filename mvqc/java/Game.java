import java.util.ArrayList;
import java.util.Random;

class Ck
{
	/*
	 * Checkers positions (TOP/BOT), KING bit and helper routines.
	 * XXX Makes sence to make checker a class, rather than int?
	 */
	final static int CNONE = 0;
	final static int CBOT = 1;
	final static int CTOP = 2;
	final static int KING = 0x8;
	final static int BOT_K = (Ck.CBOT | Ck.KING);
	final static int TOP_K = (Ck.CTOP | Ck.KING);

	public static int base(int ck) /* move direction */
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

	public static int opponent(int ck)
	{
		if (ck == Ck.CBOT)
			return Ck.CTOP;
		if (ck == Ck.CTOP)
			return Ck.CBOT;
		throw new Error();
	}
}

class Cell
{
	public Prob[] c_prob;
	private int result;

	public Cell()
	{
		/*
		 * Each cell can have checkers of both directions, so
		 * we need the probability of both
		 */

		c_prob = new Prob[3]; // 0 will be empty
		c_prob[Ck.CBOT] = new Prob(0, 1);
		c_prob[Ck.CTOP] = new Prob(0, 1);
		result = -1;
	}

	public void update(int ck, Prob prob)
	{
		/*
		 * XXX One doesn't tell kings from checkers even if
		 * making a cell measurement. Instead, both of them
		 * form a superposition and move each in its own
		 * sub board
		 */

		c_prob[Ck.base(ck)].add(prob);
	}

	public void measure(Random rand)
	{
		/*
		 * We have c_prob[$base] probability of the $base checker
		 * there and need to roll the dice to decide what really is
		 * XXX: there are two things we can measure -- what is in the
		 * cell or is there the checker of the game.turn base there.
		 * In the former case we may end up in opponent's checker
		 * disappear after the measure. In the latter -- we will
		 * happen into different set of boards.
		 */

		switch (Prob.measure2(c_prob[Ck.CBOT], c_prob[Ck.CTOP], rand)) {
		case 0:
			result = Ck.CNONE;
			break;
		case 1:
			result = Ck.CBOT;
			break;
		case 2:
			result = Ck.CTOP;
			break;
		}
	}

	public boolean match(int ck)
	{
		if (result == -1)
			throw new Error();
		return result == Ck.base(ck);
	}
}

class Board
{
	private static int id_gen = 1;
	public int id;

	private int[][] f;
	private Board parent;
	private int n_kids;
	private int[] n_checkers;
	private UI ui;
	private Rules rules;

	public Board(Rules r, int[][] init, UI _ui)
	{
		this.id = Board.id_gen;
		Board.id_gen++;

		this.f = new int[r.board_size][r.board_size/2];
		for (int l = 0; l < r.board_size; l++)
			for (int n = 0; n < r.board_size/2; n++)
				this.f[l][n] = init[l][n];

		ui = _ui;
		rules = r;
		parent = null;
		n_kids = 0;
		n_checkers = new int[3];
		n_checkers[Ck.CBOT] = n_checkers[Ck.CTOP] = r.ck_num;
	}

	public Board(Rules r, UI _ui) { this(r, r.init, _ui); }

	public boolean empty(int base)
	{
		return n_checkers[base] == 0;
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
			p.mul(new Prob(1, par.n_kids));
			par = par.parent;
		}
		return p;
	}

	public Board fork()
	{
		Board ret = new Board(rules, this.f, ui);

		/* XXX actually, we can borrow parent's array */
		ret.n_checkers = new int[3];
		ret.n_checkers[Ck.CBOT] = n_checkers[Ck.CBOT];
		ret.n_checkers[Ck.CTOP] = n_checkers[Ck.CTOP];

		ret.parent = this;
		this.n_kids++;

		return ret;
	}

	private void drop_kid(Board kid)
	{
		this.n_kids--;
		if (this.n_kids == 0)
			this.drop();
	}

	public void drop()
	{
		if (this.n_kids != 0)
			throw new Error();

		Board par = this.parent;
		if (par != null) {
			this.parent = null;
			par.drop_kid(this);
		} else
			throw new Error();
	}

	public void drop_kids()
	{
		this.n_kids = 0;
	}

	public int get(Pos pos)  { return f[pos.l][pos.n]; }
	public int getc(Pos pos) { return Ck.base(get(pos)); }
	public void set(Pos pos, int ck) { f[pos.l][pos.n] = ck; }

	public int balance(int turn)
	{
		return n_checkers[turn] - n_checkers[Ck.opponent(turn)];
	}

	public void move_at(int turn, Pos pos,
			ArrayList<Board> new_mvb,
			ArrayList<Board> stuck_mvb,
			ArrayList<Board> othc_mv)
	{
		/*
		 * Here we're on the "real" board w/o any quantum effects.
		 *
		 * Checker at line.nr is of 'turn' base, this should
		 * have been checked by the caller.
		 */

		int ck = get(pos);
		if (Ck.base(ck) != turn)
			throw new Error();

		/* First -- try to capture something */
		ui.log(String.format("Try capture from %s", pos.s()));
		if (try_to_capture(ck, pos, new_mvb))
			return;

		/*
		 * Second -- check whether any other checker can
		 * capture. If it can -- we can't move further.
		 */
		ui.log(String.format("Check for move possibility"));
		if (!valid_to_move(ck, pos)) {
			othc_mv.add(this);
			return;
		}

		/* Finally -- try to move regularly */
		ui.log(String.format("Try move from %s", pos.s()));
		if (try_to_move(ck, pos, new_mvb))
			return;

		/* No moves possible at this board.pos pair */
		ui.log("Board stuck");
		stuck_mvb.add(this);
	}

	/* Cell after peer is not available for move */
	private Pos landing(Pos pos, int dir)
	{
		Pos apos = pos.move(dir, rules.board_size);
		if (apos == null || get(apos) != Ck.CNONE)
			return null;
		else
			return apos;
	}

	private boolean try_to_capture_at(int ck, Pos pos, Pos npos, int dir, ArrayList<Board> new_mvb)
	{
		Pos nnpos = landing(npos, dir);
		if (nnpos == null)
			return false;

		int peer_c = Ck.opponent(Ck.base(ck));

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

		if (rules.fly_king && Ck.is_king(ck)) {
			ArrayList<Pos> nnposl = npos.fly_move(dir, rules.board_size);
			/*
			 * A king can land on any cell after the @npos, but
			 * once we meet non empty cell (next one is free) we
			 * should stop
			 */
			for (Pos nnpos2 : nnposl) {
				if (get(nnpos2) != Ck.CNONE)
					break;

				ui.capture_at(npos, nnpos2);

				Board nb = this.fork();
				nb.set(pos, Ck.CNONE);
				nb.set(npos, Ck.CNONE);
				nb.n_checkers[peer_c] -= 1;

				if (!nb.try_to_capture(ck, nnpos2, new_mvb)) {
					nb.set(nnpos2, ck);
					new_mvb.add(nb);
					ui.moved_to(nnpos2);
				}
			}
		} else {
			boolean cpt_next = true;

			ui.capture_at(npos, nnpos);
			Board nb = this.fork();
			nb.set(pos, Ck.CNONE);
			nb.set(npos, Ck.CNONE);
			nb.n_checkers[peer_c] -= 1;

			int king_line = (ck == Ck.CBOT ? 0 : 7);
			if (nnpos.l == king_line) {
				ck = Ck.kingify(ck);
				/*
				 * We've become a king and need to check whether
				 * the rules allow us to continue capturing or
				 * should we stop here.
				 */
				cpt_next = rules.cpt_cont;
			}

			if (!cpt_next || !nb.try_to_capture(ck, nnpos, new_mvb)) {
				nb.set(nnpos, ck);
				new_mvb.add(nb);
				ui.moved_to(nnpos);
			}
		}

		return true;
	}

	private boolean try_to_capture(int ck, Pos pos, ArrayList<Board> new_mvb)
	{
		boolean didit = false;
		int base = Ck.base(ck);

		if (rules.fly_king && Ck.is_king(ck)) {
			for (int dir : Pos.all_moves) {
				ArrayList<Pos> nposl = pos.fly_move(dir, rules.board_size);

				for (Pos npos : nposl) {
					int nbase = getc(npos);

					if (nbase == Ck.CNONE)
						continue;
					if (nbase == base)
						break;

					if (try_to_capture_at(ck, pos, npos, dir, new_mvb))
						didit = true;

					break;
				}
			}
		} else {
			int[] dirs;

			if (rules.cpt_back || Ck.is_king(ck))
				dirs = Pos.all_moves;
			else {
				if (ck == Ck.CBOT)
					dirs = Pos.up_moves;
				else
					dirs = Pos.down_moves;
			}

			for (int dir : dirs) {
				int peer = Ck.opponent(base);
				Pos npos = pos.move(dir, rules.board_size);

				if (npos != null && getc(npos) == peer)
					if (try_to_capture_at(ck, pos, npos, dir, new_mvb))
						didit = true;
			}
		}

		return didit;
	}

	private boolean check_can_capture(int ck, Pos pos)
	{
		int base = Ck.base(ck);

		if (rules.fly_king && Ck.is_king(ck)) {
			for (int dir : Pos.all_moves) {
				ArrayList<Pos> nposl = pos.fly_move(dir, rules.board_size);

				for (Pos npos : nposl) {
					int nbase = getc(npos);

					if (nbase == Ck.CNONE)
						continue;
					if (nbase == base)
						break;

					if (landing(npos, dir) != null)
						return true;
				}
			}
		} else {
			int[] dirs;

			if (rules.cpt_back || Ck.is_king(ck))
				dirs = Pos.all_moves;
			else {
				if (ck == Ck.CBOT)
					dirs = Pos.up_moves;
				else
					dirs = Pos.down_moves;
			}

			for (int dir : dirs) {
				int peer = Ck.opponent(base);
				Pos npos = pos.move(dir, rules.board_size);

				if (npos != null && getc(npos) == peer && landing(npos, dir) != null)
					return true;
			}
		}

		return false;
	}

	private boolean check_can_move(int ck, Pos pos)
	{
		int[] dirs;

		/*
		 * Just check for adjacent cells (in proper directions). Even
		 * if it's a king, next cell being busy means "locked" as
		 * the ability to capture has already been checked
		 */

		if (Ck.is_king(ck))
			dirs = Pos.all_moves;
		else {
			if (ck == Ck.CBOT)
				dirs = Pos.up_moves;
			else
				dirs = Pos.down_moves;
		}

		for (int dir: dirs) {
			Pos npos = pos.move(dir, rules.board_size);
			if (npos != null && get(npos) == Ck.CNONE)
				return true;
		}

		return false;
	}

	public boolean can_move(int turn)
	{
		for (int l = 0; l < rules.board_size; l++) {
			for (int n = 0; n < rules.board_size/2; n++) {
				Pos p = new Pos(l, n);
				int ck = get(p);

				if (Ck.base(ck) != turn)
					continue;
				if (check_can_capture(ck, p))
					return true;
				if (check_can_move(ck, p))
					return true;
			}
		}

		return false;
	}

	private boolean valid_to_move(int ck, Pos pos)
	{
		int base = Ck.base(ck);

		/*
		 * This checks whether any other checker has
		 * possibility to capture.
		 */

		for (int l = 0; l < rules.board_size; l++) {
			for (int n = 0; n < rules.board_size/2; n++) {
				Pos p = new Pos(l, n);
				int ock = get(p);

				if (Ck.base(ock) != base)
					continue;
				if (l == pos.l && n == pos.n)
					continue;

				if (check_can_capture(ock, p))
					return false;
			}
		}

		return true;
	}

	private boolean try_to_move(int ck, Pos pos, ArrayList<Board> new_mvb)
	{
		boolean didit = false;

		if (rules.fly_king && Ck.is_king(ck)) {
			for (int dir: Pos.all_moves) {
				ArrayList<Pos> nposl = pos.fly_move(dir, rules.board_size);
				for (Pos npos: nposl) {
					/*
					 * This is move. If we hit a checker we stop,
					 * even if we can capture one, since checks
					 * for capture has been already made
					 */
					if (get(npos) != Ck.CNONE)
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

			if (Ck.is_king(ck)) {
				dirs = Pos.all_moves;
				king_line = -1;
			} else {
				if (ck == Ck.CBOT) {
					dirs = Pos.up_moves;
					king_line = 0;
				} else {
					dirs = Pos.down_moves;
					king_line = 7;
				}
			}

			for (int dir: dirs) {
				Pos npos = pos.move(dir, rules.board_size);
				if (npos != null && get(npos) == Ck.CNONE) {
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

class AI_decision
{
	public int score;
	public Pos pos;

	public AI_decision()
	{
		score = 0;
	}
}

public class Game
{
	public int turn;
	public ArrayList<Board> mvb;
	private Random rand;
	private int measurements;
	private int board_size;
	private UI ui;

	public Game(Rules r, String init_s, UI _ui)
	{
		Board i_board;

		board_size = r.board_size;
		ui = _ui;
		turn = r.first_move;
		mvb = new ArrayList<Board>();
		rand = new Random();
		measurements = 0;
		if (init_s == null)
			i_board = new Board(r, _ui);
		else {
			int[][] init = new int[8][4];
			for (int l = 0; l < 8; l++) {
				for (int n = 0; n < 4; n++) {
					switch (init_s.charAt(l * 4 + n)) {
					case '-':
						break;
					case 'b':
						init[l][n] = Ck.CTOP;
						break;
					case 'w':
						init[l][n] = Ck.CBOT;
						break;
					}
				}
			}
			i_board = new Board(r, init, _ui);
		}

		mvb.add(i_board);
	}

	private void tune_multiverse(Pos pos, Cell cell)
	{
		ArrayList<Board> nmvb = new ArrayList<Board>();

		for (Board b: mvb) {
			int ck = b.get(pos);
			if (cell.match(ck))
				nmvb.add(b);
			else
				b.drop();
		}
		mvb = nmvb;
	}

	public Cell superposition(Pos pos)
	{
		Cell c = new Cell();
		for (Board b : mvb) {
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
		for (int l = 0; l < board_size; l++) {
			for (int n = 0; n < board_size/2; n++) {
				Cell c = superposition(new Pos(l, n));
				wp.add(c.c_prob[Ck.CBOT]);
				bp.add(c.c_prob[Ck.CTOP]);
			}
		}

		return String.format("mvb=%d m=%d wp=%s bp=%s",
				mvb.size(), measurements, wp.sx(), bp.sx());
	}

	private void setmv(ArrayList<Board> newmv, ArrayList<Board> drop)
	{
		for (Board b: drop)
			b.drop();

		mvb = newmv;
	}

	private void switch_turn()
	{
		Prob lose = new Prob(0, 1);

		turn = Ck.opponent(turn);

		for (Board b: mvb)
			if (!b.can_move(turn))
				lose.add(b.prob());

		if (!lose.is_zero()) {
			/*
			 * XXX in real checkers player either win or
			 * lost. In quantum checker player can lose
			 * on some boards and not on another (probably
			 * even win). And not to make win/lose be
			 * really probabilistic we abort the game once
			 * this prob is not zero.
			 */
			ui.game_over(turn, lose);
			turn = Ck.CNONE;
		}
	}

	private AI_decision try_move_ai(Pos pos)
	{
		AI_decision ret = null;
		ArrayList<Board> newmv = new ArrayList<Board>();
		ArrayList<Board> stkmv = new ArrayList<Board>();

		/* Do regular move and calculate the W/B balance after this. */
		for (Board b : mvb) {
			int ck = b.get(pos);
			if (Ck.base(ck) != turn)
				continue;

			b.move_at(turn, pos, newmv, stkmv, stkmv);
		}

		if (newmv.size() != 0) {
			ret = new AI_decision();
			ret.score = -stkmv.size(); /* Penalty for impossible moves */
			ret.pos = pos;

			for (Board b : newmv) {
				Prob bp = b.prob();
				int bal = b.balance(turn);
				ret.score += bp.mul_and_get(bal); /* This is not extremely accurate */
			}
		}

		for (Board b : mvb)
			b.drop_kids();

		return ret;
	}

	public void move_ai()
	{
		int ai_turn = turn;

		do {
			AI_decision best = null;

			for (int l = 0; l < 8; l++) {
				for (int n = 0; n < 4; n++) {
					AI_decision d;

					d = try_move_ai(new Pos(l, n));
					if (d != null && (best == null || best.score < d.score))
						best = d;
				}
			}

			/* If AI is not able to move, there should be game_over */
			if (best == null)
				throw new Error();

			ui.ai_move(best.pos);
			move_c(best.pos.l, best.pos.n);

		} while (turn == ai_turn);
	}

	/*
	 * A checker can be told by line number and either
	 * column number of its number in the row (0-3).
	 * So the first routine works on line/col and the
	 * second one on the line/nr addressing.
	 */

	public void move(int line, int col)
	{
		if (line % 2 == 0) {
			if (col % 2 == 0)
				return;
			col = (col - 1) / 2;
		} else {
			if (col % 2 == 1)
				return;
			col /= 2;
		}

		move_c(line, col);
	}

	public void move_c(int line, int nr)
	{
		Pos pos = new Pos(line, nr);
		Cell cell = superposition(pos);
		Prob prob = cell.c_prob[turn];

		/* First -- check whether our checker can be in this cell at all */
		if (prob.is_zero()) {
			ui.no_checker(pos);
			return;
		}

		/* Next -- find out whether it's 100% there or not */
		if (!prob.is_one()) {
			/* Need to take the measurement and decide what's really there */
			measurements++;
			ui.measure_cell(pos);
			cell.measure(rand);

			/*
			 * After we know whether the checker is there or
			 * not -- update the multiverse to keep only those,
			 * that have this checker in this cell
			 */
			tune_multiverse(pos, cell);

			if (!cell.match(turn)) {
				/* XXX -- should be pass the move to peer? */
				ui.no_checker(pos);
				return;
			}
		}

		ui.make_move(pos);

		/*
		 * XXX There are three possibilities of move attempt on
		 * the real board
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
		 * In -++ case we have to ask user to try again. Deciding
		 * whether or not we're "really" locked or cant move this
		 * checker can be done, but not necessary -- another attemp
		 * to move the same checker will fail anyway, attempt to
		 * move another checker can be done on both types of boards.
		 *
		 * Cases ++- and +-+ are clear -- we have to make the measurement
		 * where we are and (!) drop the "not here" boards from the
		 * multiverse. When we can move, we should drop "can't" move
		 * boards, it's clear. If we can't move, we should drop the
		 * "can" ones too, because otherwise player may try the same
		 * checker again, hit this situation and dice would show
		 * "can" this time. This guessing is not nice.
		 *
		 * Now what to do with +++ case. We roll the dice, if we can
		 * move, we drop locked and cant ones. But what if we hit
		 * the cant or locked? We should drop the can one, but should
		 * we drop the other? I think no, as this is symmetrical to
		 * the -++ case when we don't drop the other "can't" reason.
		 *
		 * Thus stuck and cant boards are effectively the same. So we
		 * measure their prob alltogether, but still collect them
		 * independently just to make further patching simpler.
		 */

		/* Try to move the checker on each board we have in the multiverse */
		ArrayList<Board> newmv = new ArrayList<Board>();
		ArrayList<Board> stkmv = new ArrayList<Board>();

		for (Board b : mvb)
			b.move_at(turn, pos, newmv, stkmv, stkmv);

		if (stkmv.isEmpty()) {
			/*
			 * We moved on all boards. Just proceed.
			 */

			mvb = newmv;
			switch_turn();
			return;
		}

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

		measurements++;
		ui.measure_situation();

		/*
		 * Some boards get stuck. We should measure the probabilities
		 * as described above and update the multiverse accordingly.
		 *
		 * XXX -- what to measure? There are two options. First is to
		 * measure the probability of the exact obstacle (the checkers
		 * that block the move). Second -- is to measure the probability
		 * of the hole situation on the board. I don't know whether these
		 * two are equal or not, but the latter one is MUCH simpler to
		 * implement, so here it is.
		 */

		Prob m_prob = new Prob(0, 1);
		Prob s_prob = new Prob(0, 1);

		for (Board b: newmv)
			m_prob.add(b.prob());
		for (Board b: stkmv)
			s_prob.add(b.prob());

		switch (Prob.measure2(m_prob, s_prob, rand)) {
		case 1:
			/* We're on boards that can move */
			setmv(newmv, stkmv);
			switch_turn();
			break;
		case 2:
			/* We're on stuck boards */
			setmv(stkmv, newmv);
			break;
		case 0:
			throw new Error();
		}
	}
}
