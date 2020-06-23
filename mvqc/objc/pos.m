#import <Foundation/Foundation.h>
#import "pos.h"

@implementation Pos
-(id)init: (int)line nr: (int) nr
{
	self = [super init];
	l = line;
	n = nr;
	return self;
}

-(Pos *)move: (int)direction board_size: (int)bsiz
{
	int nl;

	if (direction == M_UR || direction == M_UL) {
		if (l == 0)
			return nil;
		nl = l - 1;
	} else {
		if (l == bsiz - 1)
			return nil;
		nl = l + 1;
	}

	if (l % 2 == 0) {
		/*
		 * Even line, left move is nr == nr,
		 * right move is +1 and possible for all but last cell
		 */
		if (direction == M_UL || direction == M_DL)
			return [[Pos alloc] init: nl nr: n];
		else if (n < bsiz / 2 - 1)
			return [[Pos alloc] init: nl nr: n + 1];
		else
			return nil;
	} else {
		/*
		 * Odd line, right move is nr == nr,
		 * lefot move is -1 and possible for all but 0th cell
		 */
		if (direction == M_UR || direction == M_DR)
			return [[Pos alloc] init: nl nr: n];
		else if (n > 0)
			return [[Pos alloc] init: nl nr: n - 1];
		else
			return nil;
	}
}

-(NSMutableArray *)fly_move: (int)direction board_size: (int)bsiz
{
	NSMutableArray *posl = [[NSMutableArray alloc] init];

	Pos *pos = self;
	while (true) {
		Pos *npos = [pos move: direction board_size: bsiz];
		if (npos == nil)
			break;

		[posl addObject: npos];
		[npos release];
		pos = npos;
	}

	return posl;
}
@end

#ifdef DBG
int main( int argc, const char * argv[])
{
	int val;
	Pos *r;
	NSMutableArray *fr;

	NSLog(@"Pos tests");

	/* Regular moves */
	Pos *p = [[Pos alloc] init: 0 nr: 0];
	r = [p move: M_UR board_size: 8];
	if (r != nil) {
		NSLog(@"0.0 UR move");
		return 1;
	}

	r = [p move: M_UL board_size: 8];
	if (r != nil) {
		NSLog(@"0.0 UL move");
		return 1;
	}

	r = [p move: M_DL board_size: 8];
	if (r == nil || r->l != 1 || r->n != 0) {
		NSLog(@"0.0 DL move");
		return 1;
	}

	r = [p move: M_DR board_size: 8];
	if (r == nil || r->l != 1 || r->n != 1) {
		NSLog(@"0.0 DR move");
		return 1;
	}

	p = [[Pos alloc] init: 2 nr: 2];
	r = [p move: M_UR board_size: 8];
	if (r == nil || r->l != 1 || r->n != 3) {
		NSLog(@"2.2 UR move");
		return 1;
	}

	r = [p move: M_UL board_size: 8];
	if (r == nil || r->l != 1 || r->n != 2) {
		NSLog(@"2.2 UL move");
		return 1;
	}

	r = [p move: M_DL board_size: 8];
	if (r == nil || r->l != 3 || r->n != 2) {
		NSLog(@"2.2 DL move");
		return 1;
	}

	r = [p move: M_DR board_size: 8];
	if (r == nil || r->l != 3 || r->n != 3) {
		NSLog(@"0.0 DR move");
		return 1;
	}

	/* Fly moves */
	p = [[Pos alloc] init: 3 nr: 0];

	fr = [p fly_move: M_UL board_size: 8];
	if (fr.count != 0) {
		NSLog(@"3.0 UL fly move");
		return 1;
	}

	fr = [p fly_move: M_UR board_size: 8];
	if (fr.count != 3) {
		NSLog(@"3.0 UR fly move");
		return 1;
	}

	r = [fr objectAtIndex: 0];
	if (r == nil || r->l != 2 || r->n != 0) {
		NSLog(@"3.0 UR fly 0");
		return 1;
	}
	r = [fr objectAtIndex: 1];
	if (r == nil || r->l != 1 || r->n != 1) {
		NSLog(@"3.0 UR fly 1");
		return 1;
	}
	r = [fr objectAtIndex: 2];
	if (r == nil || r->l != 0 || r->n != 1) {
		NSLog(@"3.0 UR fly 2");
		return 1;
	}

	return 0;
}
#endif
