#!/usr/bin/env python

for i in xrange(1, 100):
	s = "%2d: " % i
	for j in xrange(2, i):
		if i % j == 0:
			s += "%d " % j

	print s
