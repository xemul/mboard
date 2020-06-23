import java.util.Random;

public class Prob
{
	private int num, den;

	public Prob(int num, int den) { this.num = num; this.den = den; }

	private static int gcd(int x, int y)
	{
		while (y != 0) {
			int i = x;
			x = y;
			y = i % y;
		}
		return x;
	}

	private void set_and_reduce(int nnum, int nden)
	{
		int red = Prob.gcd(nnum, nden);

		if (red != 1) {
			nnum /= red;
			nden /= red;
		}

		num = nnum;
		den = nden;
	}

	public void add(Prob prob)
	{
		int nnum = num * prob.den + prob.num * den;
		int nden = den * prob.den;
		set_and_reduce(nnum, nden);
	}

	public void mul(Prob prob)
	{
		int nnum = num * prob.num;
		int nden = den * prob.den;
		set_and_reduce(nnum, nden);
	}

	public int mul_and_get(int i) { return i * num / den; }

	public boolean is_zero()
	{
		if (num == 0) {
			if (den != 1)
				throw new Error();
			return true;
		}

		return false;
	}

	public boolean is_one()
	{
		if (num == 1 && den == 1)
			return true;
		if (num == den)
			throw new Error();
		return false;
	}

	public String s() { return String.format("%d/%d", num, den); }
	public String sx()
	{
		if (num < den)
			return s();

		int integ = num / den;
		int nnum = num - integ * den;

		if (nnum == 0)
			return String.format("%d", integ);
		else
			return String.format("%d %d/%d", integ, nnum, den);
	}

	/*
	 * Make a measurement and picks either prob1 or prob2 or
	 * none (if p1 + p2 < 1). The result is 1, 2 or 0 respectively.
	 */
	public static int measure2(Prob prob1, Prob prob2, Random rand)
	{
		// XXX -- is this sane?

		int p1 = prob1.num * prob2.den;
		int p2 = prob2.num * prob1.den;
		int tot = prob1.den * prob2.den;

		if (p1 + p2 > tot)
			throw new Error();

		int r = rand.nextInt(tot);

		if (r < p1)
			return 1;
		else if (r < p1 + p2)
			return 2;
		else
			return 0;
	}
}


