#import <Foundation/Foundation.h>
#import "ck.h"
#import "rules.h"

@implementation Rules
+(Rules *)rus
{
	Rules *r = [[Rules alloc] init];

	r->fly_king = true;
	r->cpt_back = true;
	r->cpt_cont = true;
	r->ck_num = 12;
	r->first_move = CBOT;
	r->f_init[0][0] = r->f_init[0][1] = r->f_init[0][2] = r->f_init[0][3] = CTOP;
	r->f_init[1][0] = r->f_init[1][1] = r->f_init[1][2] = r->f_init[1][3] = CTOP;
	r->f_init[2][0] = r->f_init[2][1] = r->f_init[2][2] = r->f_init[2][3] = CTOP;
	r->f_init[3][0] = r->f_init[3][1] = r->f_init[3][2] = r->f_init[3][3] = CNONE;
	r->f_init[4][0] = r->f_init[4][1] = r->f_init[4][2] = r->f_init[4][3] = CNONE;
	r->f_init[5][0] = r->f_init[5][1] = r->f_init[5][2] = r->f_init[5][3] = CBOT;
	r->f_init[6][0] = r->f_init[6][1] = r->f_init[6][2] = r->f_init[6][3] = CBOT;
	r->f_init[7][0] = r->f_init[7][1] = r->f_init[7][2] = r->f_init[7][3] = CBOT;

	return r;
}

+(Rules *)eng
{
	Rules *r = [[Rules alloc] init];

	r->fly_king = false;
	r->cpt_back = false;
	r->cpt_cont = false;
	r->ck_num = 12;
	r->first_move = CBOT;
	r->f_init[0][0] = r->f_init[0][1] = r->f_init[0][2] = r->f_init[0][3] = CTOP;
	r->f_init[1][0] = r->f_init[1][1] = r->f_init[1][2] = r->f_init[1][3] = CTOP;
	r->f_init[2][0] = r->f_init[2][1] = r->f_init[2][2] = r->f_init[2][3] = CTOP;
	r->f_init[3][0] = r->f_init[3][1] = r->f_init[3][2] = r->f_init[3][3] = CNONE;
	r->f_init[4][0] = r->f_init[4][1] = r->f_init[4][2] = r->f_init[4][3] = CNONE;
	r->f_init[5][0] = r->f_init[5][1] = r->f_init[5][2] = r->f_init[5][3] = CBOT;
	r->f_init[6][0] = r->f_init[6][1] = r->f_init[6][2] = r->f_init[6][3] = CBOT;
	r->f_init[7][0] = r->f_init[7][1] = r->f_init[7][2] = r->f_init[7][3] = CBOT;

	return r;
}
@end
