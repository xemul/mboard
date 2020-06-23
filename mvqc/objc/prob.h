@interface Prob: NSObject
{
@private
	int num;
	int den;
}

-(id)init: (int) n den: (int) d;
-(void)add: (Prob *) v;
-(void)mul: (Prob *) v;
-(BOOL)is_zero;
-(BOOL)is_one;
-(int)mul_and_get: (int) val;
+(int)measure2: (Prob *)a prob2: (Prob *)b randn: (Randn *)rnd;

-(float)getf;
-(BOOL)is: (int) n den: (int) d;
@end
