CC = gcc
CFLAGS = -lobjc -std=c99 -I/usr/include/GNUstep/ \
	 -L/usr/lib/GNUstep/Libraries -lgnustep-base \
	 -fconstant-string-class=NSConstantString -Werror -fobjc-exceptions

prob: prob-dbg.o rand.o
	$(CC) $(CFLAGS) $< -o $@

pos: pos-dbg.o
	$(CC) $(CFLAGS) $< -o $@

cell: cell-dbg.o prob.o rand.o ck.o
	$(CC) $(CFLAGS) $^ -o $@

board: board-dbg.o pos.o prob.o rand.o rules.o noui.o ck.o
	$(CC) $(CFLAGS) $^ -o $@

game: game-dbg.o board.o cell.o pos.o prob.o rand.o rules.o noui.o ck.o
	$(CC) $(CFLAGS) $^ -o $@

%-dbg.o: %.m %.h
	$(CC) $(CFLAGS) -c $< -o $@ -DDBG

%.o: %.m %.h
	$(CC) $(CFLAGS) -c $< -o $@
