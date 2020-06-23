#define BOARD_SIZE	8 /* FIXME */

@interface Rules:NSObject
{
@public
	int	f_init[BOARD_SIZE][BOARD_SIZE];
	int	ck_num;
	int	first_move;
	BOOL	fly_king;
	BOOL	cpt_back;
	BOOL	cpt_cont;
}

+(Rules *)rus;
+(Rules *)eng;
@end
