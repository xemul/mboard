#import <Foundation/Foundation.h>
#import "rand.h"
#import "prob.h"
#import "ck.h"
#import "cell.h"

@implementation Cell
-(id)init
{
	self = [super init];
	result = -1;
	c_prob[CTOP] = [[Prob alloc] init: 0 den: 1];
	c_prob[CBOT] = [[Prob alloc] init: 0 den: 1];
}

-(void)dealloc
{
	[c_prob[CTOP] release];
	[c_prob[CBOT] release];
	[super dealloc];
}

-(void)update: (int)ck prob: (Prob *)p
{
	Prob *mp = c_prob[base(ck)];
	[mp add: p];
}

-(void)measure: (Randn *)rnd
{
	int r;

	r = [Prob measure2: c_prob[CBOT] prob2: c_prob[CTOP] randn: rnd];

	switch (r) {
		case 0:
			result = CNONE;
			break;
		case 1:
			result = CBOT;
			break;
		case 2:
			result = CTOP;
			break;
	}
}

-(BOOL)match: (int)ck
{
	if (result == -1) {
		NSException *e = [NSException exceptionWithName:@"cell" reason:@"no measure" userInfo:nil];
		@throw e;
	}

	return result == base(ck);
}
@end

#ifdef DBG
int main( int argc, const char * argv[])
{
	int val;

	NSLog(@"Cell tests");

	return 0;
}
#endif
