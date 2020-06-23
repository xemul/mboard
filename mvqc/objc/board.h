extern int n_boards;

@interface Board:NSObject
{
@public
	int		bid;
	unsigned int	n_kids;
@private
	int		f[BOARD_SIZE][BOARD_SIZE];
	Board		*parent;
	int		n_checkers[3];

	id<UI>		ui;
	Rules		*rules;
}

-(id)init: (Rules *)_rules f_init: (int [BOARD_SIZE][BOARD_SIZE])_init ui: (id<UI>) _ui;
-(id)init: (Rules *)_rules ui: (id<UI>) _ui;
-(void)dealloc;
-(BOOL)empty: (int)base;
-(Prob *)prob;
-(Board *)fork: (int) ffor;
-(void)drop;
-(int)get: (Pos *)p;
-(int)getc: (Pos *)p;
-(void)set: (Pos *)p ck: (int) ck;
-(void)move_at: (int)turn at: (Pos *)pos
			nl: (NSMutableArray *)new_mvb
			sl: (NSMutableArray *)stk_mvb
			ol: (NSMutableArray *)oth_mvb;
-(BOOL)can_move: (int)turn;

-(void)lock;
-(void)unlock;

/* Forward declarations */
-(BOOL)try_to_capture: (int) ck at: (Pos *)pos nl: (NSMutableArray *)new_mvb;

@end
