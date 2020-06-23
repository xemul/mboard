#import <Foundation/Foundation.h>
#import "ck.h"
#import "rand.h"
#import "prob.h"
#import "pos.h"
#import "ui.h"
#import "noui.h"
#import "rules.h"
#import "board.h"
#import "cell.h"
#import "game.h"

@implementation Game
-(id)init: (Rules *)_rules  ui: (id<UI>) _ui  rand: (Randn *)_rand
{
	self = [super init];

	ui = _ui;
	turn = _rules->first_move;
	rand = _rand;
	measurements = 0;

	i_board = [[Board alloc] init: _rules  ui: _ui];
	mvb = [[NSMutableArray alloc] init];
	[mvb addObject:i_board];

	return self;
}

-(void)dealloc
{
	[i_board lock];
	for (int i = 0; i < mvb.count; i++) {
		Board *b = [mvb objectAtIndex:i];
		if (b == i_board)
			continue;
		[b drop];
	}
	[i_board unlock];

	[mvb release];
	[i_board release];


#ifdef DBG
	if (n_boards != 0)
		NSLog(@"Leaky boards %d", n_boards);
	else
		NSLog(@"Remove game PASS");
#endif

	[super dealloc];
}

-(Cell *)superposition: (Pos *) pos
{
	Cell *c = [[Cell alloc] init];

	for (int i = 0; i < mvb.count; i++) {
		Board *b = [mvb objectAtIndex: i];
		int ck = [b get: pos];
		if (ck != CNONE) {
			Prob *bp = [b prob];
			[c update: ck  prob: bp];
			[bp release];
		}
	}

	return c;
}

-(void)setmv: (NSMutableArray *)newmvb drop:(NSMutableArray *)dropmvb
{
	for (int i = 0; i < dropmvb.count; i++) {
		Board *b = [dropmvb objectAtIndex:i];
		[b drop];
	}
	[dropmvb release];

	[mvb release];
	mvb = newmvb;
}

-(void)switch_turn
{
	Prob *lose = [[Prob alloc] init: 0 den:1];

	turn = opponent(turn);

	for (int i = 0; i < mvb.count; i++) {
		Board *b = [mvb objectAtIndex:i];
		if (![b can_move:turn]) {
			Prob *bp = [b prob];
			[lose add: bp];
			[bp release];
		}
	}

	if (![lose is_zero]) {
		[ui game_over:turn lose_prob:lose];
		turn = CNONE;
	}
	[lose release];
}

-(void)tune_multiverse: (Pos *)pos  with: (Cell *)cell
{
	NSMutableArray *nmvb = [[NSMutableArray alloc] init];

	for (int i = 0; i < mvb.count; i++) {
		Board *b = [mvb objectAtIndex:i];
		int ck = [b get:pos];

		if ([cell match:ck])
			[nmvb addObject:b];
		else
			[b drop];
	}

	[mvb release];
	mvb = nmvb;
}

-(void) move_c: (int) line  nr: (int) nr
{
	int i;
	Pos *pos = [[Pos alloc] init: line  nr: nr];
	Cell *cell = [self superposition: pos];
	Prob *prob = cell->c_prob[turn];

	if (turn == CNONE)
		return;

	/* First -- check whether our checker can be in this cell at all */
	if ([prob is_zero]) {
		[ui no_checker: pos];
		[cell release];
		[pos release];
		return;
	}

	/* Next -- find out whether it's 100% there or not */
	if (![prob is_one]) {
		/* Need to take the measurement and decide what's really there */
		measurements++;
		[ui measure_cell: pos];
		[cell measure: rand];

		/*
		 * After we know whether the checker is there or
		 * not -- update the multiverse to keep only those,
		 * that have this checker in this cell
		 */
		[self tune_multiverse: pos  with: cell];

		if (![cell match: turn]) {
			/* XXX -- should be pass the move to peer? */
			[ui no_checker: pos];
			[cell release];
			[pos release];
			return;
		}
	}

	[ui make_move: pos];

	/* Try to move the checker on each board we have in the multiverse */
	NSMutableArray *newmv = [[NSMutableArray alloc] init];
	NSMutableArray *stkmv = [[NSMutableArray alloc] init];

	for (i = 0; i < mvb.count; i++) {
		Board *b = [mvb objectAtIndex: i];
		[b move_at: turn at:pos  nl:newmv sl:stkmv ol:stkmv];
	}

	if (stkmv.count == 0) {
		/*
		 * We moved on all boards. Just proceed.
		 */

		[mvb release];
		mvb = newmv;
		[self switch_turn];
		[cell release];
		[pos release];
		return;
	}

	if (newmv.count == 0) {
		/*
		 * We were unable to move at all. This is possible, so
		 * just ask the player to pick another cell.
		 */

		[stkmv release];
		[ui cell_stuck: pos];
		[cell release];
		[pos release];
		return;
	}

	measurements++;
	[ui measure_situation];

	/*
	 * Some boards get stuck. We should measure the probabilities
	 * as described above and update the multiverse accordingly.
	 */

	Prob *m_prob = [[Prob alloc] init:0 den:1];
	Prob *s_prob = [[Prob alloc] init:0 den:1];

	for (i = 0; i < newmv.count; i++) {
		Board *b = [newmv objectAtIndex: i];
		Prob *bp = [b prob];
		[m_prob add: bp];
		[bp release];
	}

	for (i = 0; i < stkmv.count; i++) {
		Board *b = [stkmv objectAtIndex: i];
		Prob *bp = [b prob];
		[s_prob add: bp];
		[bp release];
	}

	switch ([Prob measure2:m_prob prob2:s_prob randn:rand]) {
	case 1:
		/* We're on boards that can move */
		[self setmv:newmv drop:stkmv];
		[self switch_turn];
		break;
	case 2:
		/* We're on stuck boards */
		[self setmv:stkmv drop:newmv];
		break;
	case 0: {
			NSException *e = [NSException exceptionWithName:@"game" reason:@"bad measure" userInfo:nil];
			@throw e;
		}
	}

	[m_prob release];
	[s_prob release];
	[cell release];
	[pos release];
}

-(void) move: (int) line  column: (int) col
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

	[self move_c: line  nr: col];
}

-(void)stats: (struct game_stats *)st;
{
	int l, n;

	st->p_top = [[Prob alloc]init:0 den:1];
	st->p_bot = [[Prob alloc]init:0 den:1];

	for (l = 0; l < BOARD_SIZE; l++) {
		for (n = 0; n < BOARD_SIZE/2; n++) {
			Pos *p = [[Pos alloc] init:l nr:n];
			Cell *c = [self superposition:p];
			[st->p_bot add: c->c_prob[CBOT]];
			[st->p_top add: c->c_prob[CTOP]];
			[p release];
			[c release];
		}
	}

	st->n_boards = mvb.count;
}
@end

#ifdef DBG
int main( int argc, const char * argv[])
{
	Rules *r = [ Rules rus ];
	Noui *nui = [[ Noui alloc ] init];
	Randn *rnd = [[Randn alloc] init];
	Cell *c;
	Pos *p;

	Game *g = [[Game alloc] init: r ui:nui  rand:rnd];
	//2 - T - T      - T - T
	//3 - - - -      - - - -
	//4 - - - -  ->  - B - -
	//5 B - B -      - - B -
	// I->b1 (init board, new board)
	[g move_c: 5 nr: 0];
	if (n_boards != 2) {
		NSLog(@"1 (%d)", n_boards);
		return 1;
	}
	//2 - T - T      - - - T
	//3 - - - -      t - t -
	//4 - B - -  ->  - B - -
	//5 - - B -      - - B -
	// I->b1--> tl
	//       `> tr
	// all prev + 2 new boards
	[g move_c: 2 nr: 0];
	if (n_boards != 4) {
		NSLog(@"2 (%d)", n_boards);
		return 1;
	}
	//2 - - - T      - - - T
	//3 t - t -      x - x -
	//4 - B - -  ->  - - - -
	//5 - - B -      - - B -
	// x is T & B
	// I->b1--> tl-> br
	//       `> tr-> bl
	// all prev + 2 new boards
	[g move_c: 4 nr: 0];
	if (n_boards != 6) {
		NSLog(@"3 (%d)", n_boards);
		return 1;
	}
	// Top moves
	//2 - - - T -    - - - T -
	//3 T - B - -    B - T - -
	//4 - - - - - &  - - - - -
	//5 - - B - B    - - B - B
	//           v
	//2 - - - - -    - - - - -
	//3 T - - - -    B - T - T
	//4 - T - - - &  - - - - -
	//5 - - B - B    - - B - B
	// I->b1--> tl-> br -> tcapt
	//       `> tr-> bl -> tmovr
	NSLog(@"Top qcapture");
	[g move_c: 2 nr: 1];
	if (n_boards != 8) {
		NSLog(@"4 (%d)", n_boards);
		return 1;
	}

	/* Now check the 3.0 cell, it should have both -- black and white pieces with equal prob */
	p = [[Pos alloc] init: 3 nr: 0];
	c = [g superposition: p];
	if (![c->c_prob[CTOP] is: 1 den: 2]) {
		NSLog(@"4.t (%f)", [c->c_prob[CTOP] getf]);
		return 1;
	}
	if (![c->c_prob[CBOT] is: 1 den: 2]) {
		NSLog(@"4.b (%f)", [c->c_prob[CBOT] getf]);
		return 1;
	}
	[p release];
	[c release];

	// Bottom moves.
	// It will be measured, so set next into to 0 to make BOT result
	if (g->measurements != 0) {
		NSLog(@"5.pre.m (%d)", g->measurements);
		return 1;
	}
	if (g->turn != CBOT) {
		NSLog(@"5.pre.t");
		return 1;
	}
	[rnd set_next_int: 0];
	//2 - - - - -    - - - - -
	//3 T - - - -    B - T - T
	//4 - T - - - &  - - - - -
	//5 - - B - B    - - B - B
	// measure! (and the B piece is alive :) )
	//1 T - T - T       T - T - T
	//2 - - - - -       - B - - -
	//3 B - T - T       - - T - T
	//4 - - - - -  ->   - - - - -
	//5 - - B - B       - - B - B
	// I -> b1- x tl -> br -> tcapt (dropped due to measurement)
	//         `> tr -> bl -> tmovr -> bmov
	[g move_c:3 nr: 0];
	if (g->turn != CTOP) {
		NSLog(@"5.t");
		return 1;
	}
	if (g->measurements != 1) {
		NSLog(@"5.m (%d)", g->measurements);
		return 1;
	}
	if (n_boards != 6) {
		NSLog(@"5 (%d)", n_boards);
		return 1;
	}

	{
		struct game_stats st;

		[g stats: &st];
		NSLog(@"ST: n %d  top %.1f  bot %.1f", st.n_boards, [st.p_top getf], [st.p_bot getf]);

		if (st.n_boards != 1) {
			NSLog(@"5 n_boards");
			return 1;
		}

		[st.p_top release];
		[st.p_bot release];
	}

	// Now check that attempt to move by top is
	// blocked by the need to capture
	[g move_c: 3 nr: 1];
	if (g->turn != CTOP) {
		NSLog(@"6.m");
		return 1;
	}
	if (n_boards != 6) {
		NSLog(@"6.m (%d)", n_boards);
		return 1;
	}

	[g dealloc];
	return 0;
}
#endif
