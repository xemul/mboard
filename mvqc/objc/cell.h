@interface Cell:NSObject
{
@public
	Prob *c_prob[3];
@private
	int result;
}

-(id)init;
-(void)dealloc;
-(void)update: (int)ck prob: (Prob *)p;
-(void)measure: (Randn *)rand;
-(BOOL)match: (int)ck;
@end
