struct game_stats {
	Prob *p_top;
	Prob *p_bot;
	int n_boards;
};

@interface Game:NSObject
{
@public
	int		turn;
	int		measurements;
@private
	NSMutableArray	*mvb;
	Randn		*rand;
	id<UI>		ui;
	Board		*i_board;
}

-(id)init: (Rules *)_rules  ui: (id<UI>) _ui  rand: (Randn *)_rand;
-(void)dealloc;
-(void)move: (int) line column: (int) col;
-(void)move_c: (int) line  nr: (int) nr;
-(Cell *)superposition: (Pos *) pos;
-(void)stats: (struct game_stats *)st;
@end
