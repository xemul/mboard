
/*
 **************************
 * Sanity tests for moves *
 **************************
 */

class Nilui implements UI
{
	public void no_checker(Pos pos) { }
	public void measure_cell(Pos pos) { }
	public void measure_situation() { }
	public void make_move(Pos pos) { }
	public void cell_stuck(Pos pos) { }
	public void moved_to(Pos npos) { System.out.format("-> %s\n", npos.s()); }
	public void capture_at(Pos npos, Pos nnpos) { System.out.format("-(%s)>%s\n", npos.s(), nnpos.s()); }
	public void game_over(int turn, Prob p) { }
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

	static boolean one_move(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one quantum move");

		b.move_at(Ck.CBOT, new Pos(5, 1), n, s, s);
		if (!chk_res(n, 2, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 5, 1, Ck.CNONE },
					{ 4, 0, Ck.CBOT },
					{ 4, 1, Ck.CNONE } }, 3))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 5, 1, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 4, 1, Ck.CBOT } }, 3))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean two_moves(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> i = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test two quantum moves");

		b.move_at(Ck.CBOT, new Pos(5, 1), i, s, s);
		i.get(0).move_at(Ck.CBOT, new Pos(5, 2), n, s, s);
		i.get(1).move_at(Ck.CBOT, new Pos(5, 2), n, s, s);
		if (!chk_res(n, 3, s, 0))
			return false;

		if (!pop_comb(n, new int[][] { 
					{ 5, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 4, 0, Ck.CBOT },
					{ 4, 1, Ck.CBOT },
					{ 4, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] { 
					{ 5, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 4, 0, Ck.CBOT },
					{ 4, 1, Ck.CNONE },
					{ 4, 2, Ck.CBOT } }, 5))
			return false;
		if (!pop_comb(n, new int[][] { 
					{ 5, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 4, 1, Ck.CBOT },
					{ 4, 2, Ck.CBOT } }, 5))
			return false;

		return true;
	}

	static boolean stuck_move(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test stuck move");

		b.move_at(Ck.CBOT, new Pos(6, 0), n, s, s);
		if (!chk_res(n, 0, s, 1))
			return false;

		return true;
	}

	static boolean one_capt(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one capture");

		b.set(new Pos(5, 0), Ck.CNONE);
		b.set(new Pos(3, 1), Ck.CBOT);
		b.move_at(Ck.CTOP, new Pos(2, 0), n, s, s);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 2, 0, Ck.CNONE },
					{ 4, 1, Ck.CTOP },
					{ 3, 1, Ck.CNONE } }, 4))
			return false;

		return true;
	}

	static boolean two_capt(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test two captures");

		b.set(new Pos(6, 0), Ck.CNONE);
		b.set(new Pos(3, 1), Ck.CBOT);
		b.move_at(Ck.CTOP, new Pos(2, 0), n, s, s);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 6, 0, Ck.CTOP } }, 5))
			return false;

		return true;
	}

	static boolean one_qapt(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test quantum capture");

		b.set(new Pos(6, 0), Ck.CNONE);
		b.set(new Pos(3, 1), Ck.CBOT);
		b.set(new Pos(6, 2), Ck.CNONE);
		b.set(new Pos(4, 3), Ck.CBOT);
		b.move_at(Ck.CTOP, new Pos(2, 0), n, s, s);
		if (!chk_res(n, 2, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 6, 0, Ck.CTOP },
					{ 5, 2, Ck.CBOT },
					{ 6, 2, Ck.CNONE } }, 7))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 1, Ck.CBOT },
					{ 6, 0, Ck.CNONE },
					{ 5, 2, Ck.CNONE },
					{ 6, 2, Ck.CTOP } }, 7))
			return false;

		return true;
	}

	static boolean goto_king(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test move to king");

		b.set(new Pos(1, 0), Ck.CBOT);
		b.set(new Pos(0, 0), Ck.CNONE);
		b.set(new Pos(2, 0), Ck.CNONE);
		b.move_at(Ck.CBOT, new Pos(1, 0), n, s, s);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 1, 0, Ck.CNONE },
					{ 0, 0, Ck.BOT_K } }, 2))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean king_move(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one king move");

		b.set(new Pos(5, 1), Ck.BOT_K);
		b.move_at(Ck.CBOT, new Pos(5, 1), n, s, s);
		if (!chk_res(n, 4, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.BOT_K },
					{ 4, 0, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 3, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 4, 0, Ck.BOT_K },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 3, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.BOT_K },
					{ 3, 2, Ck.CNONE } }, 5))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 3, 0, Ck.CNONE },
					{ 4, 0, Ck.CNONE },
					{ 5, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 3, 2, Ck.BOT_K } }, 5))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean king_qapt(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test double king qapture");

		b.set(new Pos(0, 2), Ck.CNONE);
		b.set(new Pos(1, 2), Ck.CNONE);
		b.set(new Pos(5, 0), Ck.BOT_K);
		b.move_at(Ck.CBOT, new Pos(5, 0), n, s, s);
		if (!chk_res(n, 3, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 0, 2, Ck.BOT_K },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CTOP },
					{ 2, 1, Ck.CNONE },
					{ 3, 3, Ck.CNONE },
					{ 4, 3, Ck.CNONE } }, 6))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 0, 2, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 2, 1, Ck.CNONE },
					{ 3, 3, Ck.BOT_K },
					{ 4, 3, Ck.CNONE } }, 6))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 0, 2, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 2, 1, Ck.CNONE },
					{ 3, 3, Ck.CNONE },
					{ 4, 3, Ck.BOT_K } }, 6))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean king_capt(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test one king long capture");

		b.set(new Pos(1, 0), Ck.CNONE);
		b.set(new Pos(5, 2), Ck.BOT_K);
		b.move_at(Ck.CBOT, new Pos(5, 2), n, s, s);
		if (!chk_res(n, 1, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 1, 0, Ck.BOT_K },
					{ 2, 0, Ck.CNONE },
					{ 3, 1, Ck.CNONE },
					{ 4, 1, Ck.CNONE },
					{ 5, 2, Ck.CNONE } }, 5))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static boolean capt_n_kapt(Nilui ui, Rules r)
	{
		Board b = new Board(r, ui);
		ArrayList<Board> n = new ArrayList<Board>();
		ArrayList<Board> s = new ArrayList<Board>();

		System.out.println("* Test capture -> king -> capture");

		b.set(new Pos(0, 1), Ck.CNONE);
		b.set(new Pos(1, 2), Ck.CNONE);
		b.set(new Pos(2, 0), Ck.CBOT);
		b.move_at(Ck.CBOT, new Pos(2, 0), n, s, s);
		if (!chk_res(n, 2, s, 0))
			return false;

		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 1, 1, Ck.CNONE },
					{ 0, 1, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 3, 3, Ck.BOT_K },
					{ 4, 3, Ck.CNONE } }, 7))
			return false;
		if (!pop_comb(n, new int[][] {
					{ 2, 0, Ck.CNONE },
					{ 1, 1, Ck.CNONE },
					{ 0, 1, Ck.CNONE },
					{ 1, 2, Ck.CNONE },
					{ 2, 2, Ck.CNONE },
					{ 3, 3, Ck.CNONE },
					{ 4, 3, Ck.BOT_K } }, 7))
			return false;

		if (n.size() != 0)
			throw new Error(); /* Test for pop_comb works, only here */

		return true;
	}

	static void run()
	{
		Nilui ui = new Nilui();
		Rules r = Rules.make_rus_rules();

		if (!one_move(ui, r))
			return;
		if (!two_moves(ui, r))
			return;
		if (!stuck_move(ui, r))
			return;
		if (!one_capt(ui, r))
			return;
		if (!two_capt(ui, r))
			return;
		if (!one_qapt(ui, r))
			return;
		if (!goto_king(ui, r))
			return;
		if (!king_move(ui, r))
			return;
		if (!king_capt(ui, r))
			return;
		if (!king_qapt(ui, r))
			return;
		if (!capt_n_kapt(ui, r))
			return;

		System.out.println("PASS");
	}
}


