#import <Foundation/Foundation.h>
#import "rand.h"
#import "prob.h"
#import "pos.h"
#import "ui.h"
#import "noui.h"

@implementation Noui
-(void)no_checker: (Pos *) pos
{
}

-(void)measure_cell: (Pos *) pos
{
}

-(void)measure_situation
{
}

-(void)make_move: (Pos *) pos
{
}

-(void)moved_to: (Pos *) npos
{
}

-(void)capture_at: (Pos *)npos moved_to:(Pos *)nnpos
{
}

-(void)cell_stuck: (Pos *) pos
{
}

-(void)game_over: (int) lose_turn lose_prob: (Prob *)prob
{
}

-(void)ai_move: (Pos *) pos
{
}

-(void)log: (NSString *) str
{
}
@end
