public class Score
{
	public int v;
	public int start_r;
	public int start_c;
	public int r, c;

	public Score(int sr, int sc)
	{
		start_r = sr;
		start_c = sc;
	}

	public Score(Score o, int r, int c)
	{
		this(o.start_r, o.start_c);

		this.v = o.v;
		this.r = r;
		this.c = c;
	}
}
