#define M_NONE	0
#define M_UL  1
#define M_UR  2
#define M_DL  3
#define M_DR  4

#define for_all_moves(dir) for (dir = 1; dir <= 4; dir++)

@interface Pos: NSObject
{
@public
	int l;
	int n;
}

-(id)init: (int) line nr: (int) nr;
-(Pos *)move: (int) direction board_size: (int) bsiz;
-(NSMutableArray *)fly_move: (int) direction board_size: (int) bsiz;
@end
