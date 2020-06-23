@interface Randn: NSObject
{
@private
	unsigned long seed;
	int next; /* negative is invalid */
}
-(id)init: (unsigned long) s;
-(int)nextInt: (int)max;
-(void)set_next_int: (int)nxt;
@end
