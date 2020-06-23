public interface UI
{
	void no_checker(Pos pos);
	void measure_cell(Pos pos);
	void measure_situation();
	void make_move(Pos pos);
	void moved_to(Pos npos);
	void capture_at(Pos npos, Pos nnpos);
	void cell_stuck(Pos pos);
	void game_over(int lose_turn, Prob lose_prob);
	void ai_move(Pos pos);

	void log(String str);
}


