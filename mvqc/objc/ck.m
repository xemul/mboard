#import <Foundation/Foundation.h>
#import "ck.h"

int base(int ck)
{
	return ck & ~KING;
}

BOOL is_king(int ck)
{
	return ((ck & KING) != 0) ? true : false;
}

int kingify(int ck)
{
	return ck | KING;
}

int opponent(int ck)
{
	if (ck == CTOP)
		return CBOT;
	if (ck == CBOT)
		return CTOP;
	return CNONE;
}
