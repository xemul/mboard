#import <Foundation/Foundation.h>
#import "ck.h"
#import "rand.h"
#import "prob.h"
#import "pos.h"
#import "ui.h"
#import "noui.h"
#import "rules.h"
#import "board.h"

int n_boards = 0;
int board_id = 1;

@implementation Board
-(id)init: (Rules *)_rules f_init: (int [BOARD_SIZE][BOARD_SIZE])_init ui: (id<UI>) _ui
{
	self = [super init];

	ui = _ui;
	rules = _rules;

	parent = nil;
	n_kids = 0;
	n_checkers[CTOP] = n_checkers[CBOT] = rules->ck_num;

	int l, n;
	for (l = 0; l < BOARD_SIZE; l++)
		for (n = 0; n < BOARD_SIZE/2; n++)
			f[l][n] = _init[l][n];

	bid = board_id++;
	n_boards++;
	return self;
}

-(void)dealloc
{
	n_boards--;
	if (self->parent) {
		NSException *e = [NSException exceptionWithName:@"board" reason:@"parent drop" userInfo:nil];
		@throw e;
	}

	[super dealloc];
}

-(id)init: (Rules *)_rules ui: (id<UI>) _ui
{
	[self init: _rules f_init: _rules->f_init ui: _ui];
}

-(BOOL)empty: (int)base;
{
	return n_checkers[base] == 0;
}

-(Prob *)prob
{
	Prob *p = [[Prob alloc] init: 1 den: 1];
	Board *par = parent;
	while (par != nil) {
		Prob *tp = [[Prob alloc] init: 1 den: par->n_kids];
		[p mul: tp];
		[tp release];
		par = par->parent;
	}

	return p;
}

-(Board *)fork: (int) ffor
{
	Board *ret = [[Board alloc] init: rules f_init: f ui: ui];
	ret->n_checkers[CTOP] = n_checkers[CTOP];
	ret->n_checkers[CBOT] = n_checkers[CBOT];
	ret->parent = [self retain];
	ret->parent->n_kids++;

	return ret;
}

-(void)drop
{
	if (self->n_kids != 0) {
		NSException *e = [NSException exceptionWithName:@"board" reason:@"kids drop" userInfo:nil];
		@throw e;
	}

	Board *p = parent;
	if (p != nil) {
		parent = nil;
		p->n_kids--;
		if (p->n_kids == 0)
			[p drop];
		[p release];
	} else {
		NSException *e = [NSException exceptionWithName:@"board" reason:@"root drop" userInfo:nil];
		@throw e;
	}
}

-(int)get: (Pos *)p
{
	return f[p->l][p->n];
}

-(int)getc: (Pos *)p
{
	return base([self get: p]);
}

-(void)set: (Pos *)p ck: (int)ck
{
	f[p->l][p->n] = ck;
}

-(Pos *)landing: (Pos *)pos dir: (int) dir
{
	Pos *apos = [pos move: dir board_size: BOARD_SIZE];
	if (apos == nil)
		return nil;

	if ([self get: apos] != CNONE) {
		[apos release];
		return nil;
	}

	return apos;
}

-(BOOL)try_to_capture_at: (int) ck at: (Pos *)pos what: (Pos *)npos dir: (int)dir nl: (NSMutableArray *)new_mvb
{
	Pos *nnpos = [self landing: npos dir: dir];
	if (nnpos == nil)
		return false;

	int peer_c = opponent(ck);

	if (rules->fly_king && is_king(ck)) {
		NSMutableArray *nnposl = [npos fly_move: dir board_size: BOARD_SIZE];

		for (int i = 0; i < nnposl.count; i++) {
			Pos *nnpos2 = [nnposl objectAtIndex: i];
			if ([self get: nnpos2] != CNONE)
				break;

			[ui capture_at: npos moved_to: nnpos2];

			Board *nb = [self fork:1];
			[nb set: pos ck: CNONE];
			[nb set: npos ck: CNONE];
			nb->n_checkers[peer_c]--;

			if (![nb try_to_capture: ck at: nnpos2 nl: new_mvb]) {
				[nb set: nnpos2 ck: ck];
				[new_mvb addObject: nb];
				[ui moved_to: nnpos2];
			}

			[nb release];
		}

		[nnposl release];
	} else {
		BOOL cpt_next = false;

		[ui capture_at: npos moved_to: nnpos];

		Board *nb = [self fork:2];
		[nb set: pos ck: CNONE];
		[nb set: npos ck: CNONE];
		nb->n_checkers[peer_c] -= 1;

		int king_line = (ck == CBOT ? 0 : 7);
		if (nnpos->l == king_line) {
			ck = kingify(ck);
			cpt_next = rules->cpt_cont;
		}

		if (!cpt_next || ![nb try_to_capture: ck  at: nnpos nl: new_mvb]) {
			[nb set: nnpos ck: ck];
			[new_mvb addObject: nb];
			[ui moved_to: nnpos];
		}

		[nb release];
	}

	[nnpos release];
	return true;
}

-(BOOL)try_to_capture: (int) ck at: (Pos *)pos nl: (NSMutableArray *)new_mvb
{
	BOOL did_it = false;
	int ck_base = base(ck);
	int dir;

	if (rules->fly_king && is_king(ck)) {
		for_all_moves(dir) {
			NSMutableArray *nposl = [pos fly_move: dir board_size: BOARD_SIZE];

			for (int i = 0; i < nposl.count; i++) {
				Pos *npos = [nposl objectAtIndex: i];
				int nbase = [self getc: npos];

				if (nbase == CNONE)
					continue;
				if (nbase == ck_base)
					break;

				if ([self try_to_capture_at: ck at: pos what: npos dir: dir nl: new_mvb])
					did_it = true;

				break;
			}

			[nposl release];
		}
	} else {
		int dirs[5];

		if (rules->cpt_back || is_king(ck)) {
			dirs[0] = M_UR;
			dirs[1] = M_UL;
			dirs[2] = M_DR;
			dirs[3] = M_DL;
			dirs[4] = M_NONE;
		} else {
			if (ck == CBOT) {
				dirs[0] = M_UR;
				dirs[1] = M_UL;
			} else {
				dirs[0] = M_DL;
				dirs[1] = M_DR;
			}
			dirs[2] = M_NONE;
		}

		for (int i = 0; dirs[i] != M_NONE; i++) {
			dir = dirs[i];

			int peer = opponent(ck_base);
			Pos *npos = [pos move: dir board_size: BOARD_SIZE];

			if (npos != nil) {
				if ([self getc: npos] == peer)
					if ([self try_to_capture_at: ck at: pos what: npos dir: dir nl: new_mvb])
						did_it = true;

				[npos release];
			}
		}
	}

	return did_it;
}

-(BOOL)check_can_capture: (int)ck at:(Pos *)pos
{
	int ck_base = base(ck);
	int dir;

	if (rules->fly_king && is_king(ck)) {
		for_all_moves(dir) {
			NSMutableArray *nposl = [pos fly_move: dir board_size: BOARD_SIZE];

			for (int i = 0; i < nposl.count; i++) {
				Pos *npos = [nposl objectAtIndex: i];
				int nbase = [self getc: npos];

				if (nbase == CNONE)
					continue;
				if (nbase == ck_base)
					break;

				Pos *lpos = [self landing: npos dir: dir];
				if (lpos != nil) {
					[lpos release];
					[nposl release];
					return true;
				}
			}

			[nposl release];
		}
	} else {
		int dirs[5];

		if (rules->cpt_back || is_king(ck)) {
			dirs[0] = M_UR;
			dirs[1] = M_UL;
			dirs[2] = M_DR;
			dirs[3] = M_DL;
			dirs[4] = M_NONE;
		} else {
			if (ck == CBOT) {
				dirs[0] = M_UR;
				dirs[1] = M_UL;
			} else {
				dirs[0] = M_DL;
				dirs[1] = M_DR;
			}
			dirs[2] = M_NONE;
		}

		for (int i = 0; dirs[i] != M_NONE; i++) {
			dir = dirs[i];
			int peer = opponent(ck_base);
			Pos *npos = [pos move: dir board_size: BOARD_SIZE];

			if (npos == nil)
				continue;

			if ([self getc: npos] != peer) {
				[npos release];
				continue;
			}

			Pos *lpos = [self landing: npos dir: dir];
			if (lpos == nil) {
				[npos release];
				continue;
			}

			[npos release];
			[lpos release];
			return true;
		}
	}

	return false;
}

-(BOOL)valid_to_move: (int)ck at: (Pos *)pos
{
	int ck_base = base(ck);

	for (int l = 0; l < BOARD_SIZE; l++) {
		for (int n = 0; n < BOARD_SIZE/2; n++) {
			if (l == pos->l && n == pos->n)
				continue;

			Pos *p = [[Pos alloc] init: l nr: n];
			int ock = [self get: p];

			if (base(ock) == ck_base)
				if ([self check_can_capture: ock at: p]) {
					[p release];
					return false;
				}

			[p release];
		}
	}

	return true;
}

-(BOOL)try_to_move: (int) ck at: (Pos *)pos nl: (NSMutableArray *)new_mvb
{
	BOOL didit = false;
	int dir;

	if (rules->fly_king && is_king(ck)) {
		for_all_moves(dir) {
			NSMutableArray *nposl = [pos fly_move: dir board_size: BOARD_SIZE];

			for (int i = 0; i < nposl.count; i++) {
				Pos *npos = [nposl objectAtIndex: i];
				if ([self get: npos] != CNONE)
					break;

				Board *nb = [self fork:3];
				[nb set: pos ck: CNONE];
				[nb set: npos ck: ck];
				[new_mvb addObject: nb];
				[nb release];
				[ui moved_to: npos];
				didit = true;
			}
		}

	} else {
		int dirs[5];
		int king_line;

		if (is_king(ck)) {
			king_line = -1;
			dirs[0] = M_UR;
			dirs[1] = M_UL;
			dirs[2] = M_DR;
			dirs[3] = M_DL;
			dirs[4] = M_NONE;
		} else {
			if (ck == CBOT) {
				dirs[0] = M_UR;
				dirs[1] = M_UL;
				king_line = 0;
			} else {
				dirs[0] = M_DL;
				dirs[1] = M_DR;
				king_line = 7;
			}
			dirs[2] = M_NONE;
		}

		for (int i = 0; dirs[i] != M_NONE; i++) {
			dir = dirs[i];
			Pos *npos = [pos move: dir  board_size: BOARD_SIZE];
			if (npos != nil) {
				if ([self get: npos] == CNONE) {
					Board *nb = [self fork:4];
					[nb set: pos ck: CNONE];
					if (npos->l == king_line)
						ck = kingify(ck);
					[nb set: npos ck: ck];
					[new_mvb addObject: nb];
					[nb release];
					[ui moved_to: npos];
					didit = true;
				}

				[npos release];
			}
		}
	}

	return didit;
}

-(void)move_at: (int)turn at: (Pos *)pos
			nl: (NSMutableArray *)new_mvb
			sl: (NSMutableArray *)stk_mvb
			ol: (NSMutableArray *)oth_mvb
{
	int ck = [self get: pos];
	if (base(ck) != turn) {
		NSException *e = [NSException exceptionWithName:@"board" reason:@"bad move color" userInfo:nil];
		@throw e;
	}

	if ([self try_to_capture: ck at: pos nl: new_mvb])
		return;

	if (![self valid_to_move: ck at: pos]) {
		[oth_mvb addObject: self];
		return;
	}

	if ([self try_to_move: ck at: pos nl: new_mvb])
		return;

	[stk_mvb addObject: self];
	return;
}

-(BOOL)check_can_move: (int) ck  at: (Pos *)pos
{
	int dirs[5];

	if (is_king(ck)) {
		dirs[0] = M_UR;
		dirs[1] = M_UL;
		dirs[2] = M_DR;
		dirs[3] = M_DL;
		dirs[4] = M_NONE;
	} else {
		if (ck == CBOT) {
			dirs[0] = M_UR;
			dirs[1] = M_UL;
		} else {
			dirs[0] = M_DL;
			dirs[1] = M_DR;
		}
		dirs[2] = M_NONE;
	}

	for (int i = 0; dirs[i] != M_NONE; i++) {
		int dir = dirs[i];
		Pos *npos = [pos move:dir  board_size:BOARD_SIZE];
		if (npos != nil) {
			if ([self get:npos] == CNONE) {
				[npos release];
				return true;
			}

			[npos release];
		}
	}

	return false;
}

-(BOOL)can_move: (int)turn
{
	for (int l = 0; l < BOARD_SIZE; l++) {
		for (int n = 0; n < BOARD_SIZE/2; n++) {
			Pos *p = [[Pos alloc] init:l nr:n];
			int ck = [self get:p];

			if (base(ck) != turn) {
				[p release];
				continue;
			}

			if ([self check_can_capture:ck at:p]) {
				[p release];
				return true;
			}
			if ([self check_can_move:ck at:p]) {
				[p release];
				return true;
			}
		}
	}

	return false;
}

-(void)lock { self->n_kids++; }
-(void)unlock { self->n_kids--; }
@end

#ifdef DBG
NSMutableArray *chk_move(Rules *r, id<UI> ui,
		int in[][3], int nin, /* init */
		int mv[3], /* mov */
		int nmvs, int nstk)
{
	int i;
	Board *b = [[Board alloc] init:r ui:ui];

	for (i = 0; i < nin; i++) {
		Pos *p = [[Pos alloc]init:in[i][1] nr:in[i][2]];
		[b set:p ck:in[i][0]];
		[p release];
	}

	NSMutableArray *nl = [[NSMutableArray alloc]init];
	NSMutableArray *sl = [[NSMutableArray alloc]init];
	Pos *p = [[Pos alloc]init:mv[1] nr:mv[2]];
	[b move_at: mv[0] at:p  nl:nl sl:sl ol:sl];

	if (nl.count != nmvs) {
		NSLog(@"Mvs %d", nl.count);
		for (i = 0; i < nl.count; i++) {
			Board *b = [nl objectAtIndex: i];
			printf("b\n");
			for (int l = 0; l < BOARD_SIZE; l++) {
				if (l % 2 == 0)
					printf("   ");
				for (int n = 0; n < BOARD_SIZE/2; n++) {
					Pos *p = [[Pos alloc] init: l nr: n];
					printf("%2d    ", [b get: p]);
					[p release];
				}
				printf("\n");
			}
		}
		return nil;
	}

	if (sl.count != nstk) {
		NSLog(@"Stk");
		return nil;
	}

	[b lock];
	[b release];
	[sl release];

	return nl;
}

void chk_comb(NSMutableArray *nb, int chk[][3], int nc)
{
	for (int i = 0; i < nb.count; i++) {
		int c;
		Board *b = [nb objectAtIndex:i];
		for (c = 0; c < nc; c++) {
			bool m;
			Pos *p = [[Pos alloc]init:chk[c][1] nr:chk[c][2]];
			m = ([b get:p] == chk[c][0]);
			[p release];
			if (!m)
				break;
		}

		if (c == nc) /* Match */ {
			[b drop];
			[nb removeObjectAtIndex:i];
			return;
		}
	}
}

int main( int argc, const char * argv[])
{
	NSLog(@"Board tests");

	Rules *r = [ Rules rus ];
	Noui *nui = [[ Noui alloc ] init ];
	Board *b = [[ Board alloc ] init: r ui: nui ];

	NSLog(@"Check empty");
	if ([b empty: CTOP] || [b empty: CBOT]) {
		NSLog(@"N checkers");
		return 1;
	}

	NSLog(@"Check prob");
	Prob *p = [b prob];
	if (![p is: 1 den: 1]) {
		NSLog(@"1 prob");
		return 1;
	}
	[p release];

	NSLog(@"Check fork");
	Board *k1 = [b fork:13];
	Board *k2 = [b fork:14];

	p = [k1 prob];
	if (![p is: 1 den: 2]) {
		NSLog(@"1/2 prob of fork");
		return 1;
	}
	[p release];

	NSLog(@"Check drop");
	[k1 drop];
	p = [k2 prob];
	if (![p is: 1 den: 1]) {
		NSLog(@"1/1 prob of fork/drop");
		return 1;
	}
	[p release];

	[b lock];
	[k2 drop];
	[b unlock];

	NSLog(@"Release k");
	[k1 release];
	[k2 release];
	NSLog(@"Release b");
	[b release];


	/* Blocked move */
	{
		NSLog(@"1.0 move");
		int i[0][3];
		int m[3] = { CTOP, 1, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 0, m, 0, 1);
		[n release];
		NSLog(@"Pass");
	}

	/* Quantum move */
	{
		NSLog(@"2.0 move");
		int i[0][3];
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 0, m, 2, 0);
		int cmb1[4][3] = { { CTOP, 3, 0 }, { CNONE, 3, 1 }, { CNONE, 2, 0 }, { CNONE, 4, 1 } };
		chk_comb(n, cmb1, 4);
		int cmb2[4][3] = { { CNONE, 3, 0 }, { CTOP, 3, 1 }, { CNONE, 2, 0 }, { CNONE, 4, 1 } };
		chk_comb(n, cmb2, 4);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Classical move */
	{
		NSLog(@"2.3 move");
		int i[0][3];
		int m[3] = { CTOP, 2, 3 };
		NSMutableArray *n = chk_move(r, nui, i, 0, m, 1, 0);
		int cmb1[3][3] = { { CNONE, 2, 3 }, { CTOP, 3, 3 }, { CNONE, 4, 2 } };
		chk_comb(n, cmb1, 3);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Fly king move */
	{
		NSLog(@"2.1 king move");
		int i[1][3] = { CTOP_K, 2, 1 };
		int m[3] = { CTOP, 2, 1 };
		NSMutableArray *n = chk_move(r, nui, i, 1, m, 4, 0);
		int cmb1[4][3] = { { CNONE, 2, 1 }, { CTOP_K, 3, 1 }, { CNONE, 4, 0 }, { CBOT, 5, 0 } };
		chk_comb(n, cmb1, 4);
		int cmb2[4][3] = { { CNONE, 2, 1 }, { CNONE, 3, 1 }, { CTOP_K, 4, 0 }, { CBOT, 5, 0 } };
		chk_comb(n, cmb2, 4);
		int cmb3[4][3] = { { CNONE, 2, 1 }, { CTOP_K, 3, 2 }, { CNONE, 4, 2 }, { CBOT, 5, 3 } };
		chk_comb(n, cmb3, 4);
		int cmb4[4][3] = { { CNONE, 2, 1 }, { CNONE, 3, 2 }, { CTOP_K, 4, 2 }, { CBOT, 5, 3 } };
		chk_comb(n, cmb4, 4);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Classical capture */
	{
		NSLog(@"2.0 capt");
		int i[1][3] = { { CBOT, 3, 1 } };
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 1, m, 1, 0);
		int cmb1[3][3] = { { CNONE, 2, 0 }, { CNONE, 3, 1 }, { CTOP, 4, 1 } };
		chk_comb(n, cmb1, 3);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Must capture blocks move */
	{
		NSLog(@"2.0 no move");
		int i[1][3] = { { CBOT, 3, 3 } };
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 1, m, 0, 1);
		[n release];
		NSLog(@"Pass");
	}

	/* Quantum capture */
	{
		NSLog(@"2.1 q capt");
		int i[2][3] = { { CBOT, 3, 1 }, { CBOT, 3, 2 } };
		int m[3] = { CTOP, 2, 1 };
		NSMutableArray *n = chk_move(r, nui, i, 2, m, 2, 0);
		int cmb1[3][3] = { { CNONE, 2, 1 }, { CNONE, 3, 1 }, { CTOP, 4, 0 } };
		chk_comb(n, cmb1, 3);
		int cmb2[3][3] = { { CNONE, 2, 1 }, { CNONE, 3, 2 }, { CTOP, 4, 2 } };
		chk_comb(n, cmb2, 3);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Blocked king capture */
	{
		NSLog(@"2.0 k move not capt");
		int i[2][3] = { { CTOP_K, 2, 0 }, { CNONE, 7, 3 } };
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 2, m, 3, 0);
		int cmb1[4][3] = { { CNONE, 2, 0 }, { CTOP_K, 3, 1 }, { CNONE, 4, 1 }, { CNONE, 7, 3 } };
		chk_comb(n, cmb1, 4);
		int cmb2[4][3] = { { CNONE, 2, 0 }, { CNONE, 3, 1 }, { CTOP_K, 4, 1 }, { CNONE, 7, 3 } };
		chk_comb(n, cmb2, 4);
		int cmb3[4][3] = { { CNONE, 2, 0 }, { CNONE, 3, 1 }, { CNONE, 4, 1 }, { CTOP_K, 3, 0 } };
		chk_comb(n, cmb3, 4);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Single king capture */
	{
		NSLog(@"2.0 k capt");
		int i[2][3] = { { CTOP_K, 2, 0 }, { CNONE, 6, 2 } };
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 2, m, 1, 0);
		int cmb1[5][3] = { { CNONE, 2, 0 }, { CNONE, 5, 2 }, { CNONE, 6, 2 }, { CNONE, 5, 3 }, { CTOP_K, 4, 3} };
		chk_comb(n, cmb1, 5);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Big quantum king capture */
	{
		NSLog(@"2.0 kbq capt");
		int i[4][3] = { { CTOP_K, 2, 0 }, { CNONE, 6, 2 }, { CNONE, 6, 0 }, { CBOT, 3, 1 } };
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 4, m, 2, 0);
		int cmb1[6][3] = { { CNONE, 3, 1 }, { CBOT, 5, 1 }, { CNONE, 5, 2 }, { CNONE, 5, 3 },
					{ CTOP_K, 4, 3 }, { CNONE, 6, 0 } };
		chk_comb(n, cmb1, 6);
		int cmb2[6][3] = { { CNONE, 3, 1 }, { CNONE, 5, 1 }, { CBOT, 5, 2 }, { CBOT, 5, 3 },
					{ CNONE, 4, 3 }, { CTOP_K, 6, 0 } };
		chk_comb(n, cmb2, 5);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	/* Long king q capture */
	{
		NSLog(@"2.0 kq capt");
		int i[3][3] = { { CTOP_K, 2, 0 }, { CNONE, 6, 2 }, { CNONE, 7, 3 } };
		int m[3] = { CTOP, 2, 0 };
		NSMutableArray *n = chk_move(r, nui, i, 3, m, 2, 0);
		int cmb1[6][3] = { { CNONE, 2, 0 }, { CNONE, 5, 2 }, { CNONE, 6, 2 }, { CNONE, 5, 3 },
					{ CTOP_K, 4, 3}, { CNONE, 7, 3 }};
		chk_comb(n, cmb1, 6);
		int cmb2[6][3] = { { CNONE, 2, 0 }, { CNONE, 5, 2 }, { CNONE, 6, 2 }, { CBOT, 5, 3 },
					{ CNONE, 4, 3}, { CTOP_K, 7, 3 }};
		chk_comb(n, cmb2, 6);
		if (n.count != 0)
			NSLog(@"Comb");
		else
			NSLog(@"Pass");
		[n release];
	}

	return 0;
}
#endif
