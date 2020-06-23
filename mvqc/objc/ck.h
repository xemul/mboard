#define CNONE 0
#define CBOT  1
#define CTOP  2
#define KING  0x8
#define CBOT_K (CBOT | KING)
#define CTOP_K (CTOP | KING)

int base(int ck);
BOOL is_king(int ck);
int kingify(int ck);
int opponent(int ck);
