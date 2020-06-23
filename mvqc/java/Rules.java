public class Rules
{
	public int board_size;
	public int first_move;
	public boolean cpt_back; /* Can man capture back? */
	public boolean fly_king; /* Can king fly? */
	public boolean cpt_cont; /* Can man continue capturing after becoming a king? */

	public int[][] init;
	public int ck_num;

	public static int[][] init_8x8 = {
			{ Ck.CTOP, Ck.CTOP, Ck.CTOP, Ck.CTOP },
			{ Ck.CTOP, Ck.CTOP, Ck.CTOP, Ck.CTOP },
			{ Ck.CTOP, Ck.CTOP, Ck.CTOP, Ck.CTOP },

			{ Ck.CNONE, Ck.CNONE, Ck.CNONE, Ck.CNONE },
			{ Ck.CNONE, Ck.CNONE, Ck.CNONE, Ck.CNONE },

			{ Ck.CBOT, Ck.CBOT, Ck.CBOT, Ck.CBOT },
			{ Ck.CBOT, Ck.CBOT, Ck.CBOT, Ck.CBOT },
			{ Ck.CBOT, Ck.CBOT, Ck.CBOT, Ck.CBOT }
		};

	private Rules() { }

	public static Rules make_rus_rules()
	{
		Rules r = new Rules();
		r.board_size = 8;
		r.first_move = Ck.CBOT;
		r.cpt_back = true;
		r.fly_king = true;
		r.cpt_cont = true;
		r.init = Rules.init_8x8;
		r.ck_num = 12;
		return r;
	}

	public static Rules make_eng_rules()
	{
		Rules r = new Rules();
		r.board_size = 8;
		r.first_move = Ck.CBOT;
		r.cpt_back = false;
		r.fly_king = false;
		r.cpt_cont = false;
		r.init = Rules.init_8x8;
		r.ck_num = 12;
		return r;
	}
}


