#!/usr/bin/env python
import sys

init_seeds = 3
luns = 4


class lun:
	def __init__(self, lvl):
		self.c = init_seeds
		if lvl == 0:
			self.b = board(1)
		else:
			self.b = None

	def get(self): # Picks all seeds for planting
		ret = self.c
		self.c = 0
		return ret

	def plant(self, wi, ni, bsk): # Put one seed
		self.c += 1
		if self.b:
			self.b.move(wi, ni, bsk)

	def capture(self, wi, bsk): # Capture all seeds
		bsk.c[wi] += self.c
		self.c = 0

	def cango(self):
		return self.c > 0

	def str(self, r):
		if not self.b:
			return ''
		else:
			return self.b.rows(r) + ' :'


class board:
	def __init__(self, lvl = 0):
		self.luns = ( [ lun(lvl) for _ in xrange(0, luns) ],
				[ lun(lvl) for _ in xrange(0, luns) ] )

	def move(self, wi, ni, bsk):
		n = ni
		w = wi
		ls = self.luns[w]

		lun = ls[n]
		if not lun.cango():
			print 'Cannot move from here'
			return False

		seeds = lun.get()
		while seeds != 0:
			n += 1
			if n < luns:
				lun = ls[n]
				lun.plant(wi, ni, bsk)
				seeds -= 1
				continue

			w = (w + 1) % 2
			ls = self.luns[w]
			n = -1

		if w != wi:
			while True:
				lun = ls[n]
				if not lun.c in (2, 3):
					break

				lun.capture(wi, bsk)
				n -= 1
				if n < 0:
					break

		return True

	def rows(self, r):
		return ''.join(map(lambda x: '%3d' % x.c, self.pluns(r)))

	def pluns(self, r):
		if r == 0:
			return reversed(self.luns[0])
		else:
			return self.luns[1]

	def show(self):
		for i in (0, 1):
			print self.rows(i)

			sa = ''
			sb = ''
			for l in self.pluns(i):
				sa += l.str(0)
				sb += l.str(1)
			if sa:
				print '\t' + sa
				print '\t' + sb


class basket:
	def __init__(self):
		self.c = [0, 0]


class vari:
	def __init__(self):
		self.brd = board()
		self.bsk = basket()
		self.turn = 0

	def show(self):
		print 'a: %d   b: %d' % (self.bsk.c[0], self.bsk.c[1])
		self.brd.show()

	def move(self, n):
		if self.brd.move(self.turn, n, self.bsk):
			self.turn = (self.turn + 1) % 2
		self.show()


g = vari()
g.show()

while True:
	l = raw_input('Move %s: ' % ('a', 'b')[g.turn])
	g.move(int(l))
