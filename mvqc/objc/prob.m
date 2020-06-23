#import <Foundation/Foundation.h>
#import "rand.h"
#import "prob.h"

@implementation Prob
-(id)init: (int)n den: (int)d
{
	self = [super init];
	num = n;
	den = d;
	return self;
}

+(int) gcd: (int) x y: (int) y
{
	while (y != 0) {
		int i = x;
		x = y;
		y = i % x;
	}

	return x;
}

-(void) set_and_reduce: (int)n den: (int)d
{
	int red = [Prob gcd: n y: d];

	if (red != 1) {
		n /= red;
		d /= red;
	}

	num = n;
	den = d;
}

-(void) add: (Prob *) v
{
	int nnum = num * v->den + v->num * den;
	int nden = den * v->den;
	[self set_and_reduce: nnum den: nden];
}

-(void) mul: (Prob *) v
{
	int nnum = num * v->num;
	int nden = den * v->den;
	[self set_and_reduce: nnum den: nden];
}

-(BOOL)is_zero
{
	if (num == 0) {
		if (den != 1) {
			NSException *e = [NSException exceptionWithName:@"prob" reason:@"0/N" userInfo:nil];
			@throw e;
		}

		return true;
	}

	return false;
}

-(BOOL)is_one
{
	if (num == 1 && den == 1)
		return true;
	if (num == den) {
		NSException *e = [NSException exceptionWithName:@"prob" reason:@"N/N" userInfo:nil];
		@throw e;
	}
	return false;
}

-(int)mul_and_get: (int) val
{
	return val * num / den;
}

-(float)getf
{
	return (float)num / (float)den;
}

-(BOOL)is: (int) n den: (int) d
{
	return num == n && den == d;
}

+(int)measure2: (Prob *)a prob2: (Prob *)b randn: (Randn *)rnd
{
	int p1 = a->num * b->den;
	int p2 = b->num * a->den;
	int tot = a->den * b->den;

	if (p1 + p2 > tot) {
		NSException *e = [NSException exceptionWithName:@"prob" reason:@"m2" userInfo:nil];
		@throw e;
	}

	int r = [rnd nextInt: tot];

	if (r < p1)
		return 1;
	else if (r < p1 + p2)
		return 2;
	else
		return 0;
}
@end

#ifdef DBG
int main( int argc, const char * argv[])
{
	int val;

	NSLog(@"Prob tests");

	Prob *p = [[Prob alloc] init: 2 den: 3];
	if (![p is: 2 den: 3]) {
		NSLog(@"Set err");
		return 1;
	}

	val = [p mul_and_get: 6]; /* 2/3 * 6 = 4 */
	if (val != 4) {
		NSLog(@"MnG err");
		return 1;
	}

	[p add: [[Prob alloc] init: 2 den: 3]]; /* 2/3 + 2/3 = 4/3 */
	if (![p is: 4 den: 3]) {
		NSLog(@"Add err");
		return 1;
	}

	[p mul: [[Prob alloc] init: 1 den: 2]]; /* 4/3 * 1/2 = 2/3 */
	if (![p is: 2 den: 3]) {
		NSLog(@"Mul err");
		return 1;
	}

	return 0;
}
#endif
