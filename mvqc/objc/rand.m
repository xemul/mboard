#import <Foundation/Foundation.h>
#import "rand.h"

@implementation Randn
-(id)init: (unsigned long) s
{
	self = [super init];
	seed = s;
	next = -1;
	return self;
}

-(int)nextInt: (int) max
{
	if (next != -1) {
		int ret = next;
		next = -1;
		return ret % max;
	}

	seed = ((seed * 1664525 + 1013904223) >> 16)/(float)0x10000;
	return seed % max;
}

-(void)set_next_int: (int)nxt
{
	next = nxt;
}
@end
