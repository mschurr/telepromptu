
import java.util.ArrayList;
import java.util.Collections;

public class SequenceAligner
{
	protected String[] x;
	protected String[] y;
	protected SequenceScorer scorer;

	public SequenceAligner(String[] x, String[] y, SequenceScorer scorer)
	{
		this.x = x;
		this.y = y;
		/*this.x = new String[x.length+1];
		this.y = new String[y.length+1];
		this.x[0] = null;
		this.y[0] = null;
		System.arraycopy(x,0,this.x,1,x.length);
		System.arraycopy(y,0,this.y,1,y.length);*/
		this.scorer = scorer;
	}

	public void run()
	{
		// Build the scoring matrix of zeros.
		int[][] score = new int[this.x.length+1][this.y.length+1];

		for(int i = 0; i <= this.x.length; i++) {
			for(int j = 0; j <= this.y.length; j++) {
				score[i][j] = 0;
			}
		}

		// Put in base case scores.
		for(int i = 1; i <= this.x.length; i++) {
			score[i][0] = score[i-1][0] + this.scorer.score(this.x[i-1],null);
		}

		for(int j = 1; j <= this.y.length; j++) {
			score[0][j] = score[0][j-1] + this.scorer.score(null, this.y[j-1]);
		}

		// Generate the matrix.
		for(int i = 1; i <= this.x.length; i++) {
			for(int j = 1; j <= this.y.length; j++) {
				// Generate new score at (i,j) based on possible steps.
				int a = score[i-1][j-1] + this.scorer.score(this.x[i-1], this.y[j-1]);
				int b = score[i-1][j]   + this.scorer.score(this.x[i-1], null);
				int c = score[i][j-1]   + this.scorer.score(null, this.y[j-1]);

				// Choose the maximum of the possible scores.
				int[] values = {a, b, c};
				int max = 0;

				for(int k = 0; k < values.length; k++) {
					if(values[k] > max)
						max = values[k];
				}

				score[i][j] = max;
			}
		}

		for(int i = 0; i < score.length; i++)
			System.out.println(java.util.Arrays.toString(score[i]));

		// Traceback
		ArrayList<String> stringX = new ArrayList<>();
		ArrayList<String> stringY = new ArrayList<>();
		int i = this.x.length;
		int j = this.y.length;

		while(i != 0 && j != 0) {
			System.out.format("%d %d\n",i,j);
			// UP
			if(score[i][j] == score[i-1][j] + this.scorer.score(this.x[i],null)) {
				stringX.add(this.x[i]);
				stringY.add(null);
				i -= 1;
				continue;
			}

			// LEFT
			if(score[i][j] == score[i][j-1] + this.scorer.score(null, this.y[j])) {
				stringX.add(null);
				stringY.add(this.y[j]);
				j -= 1;
				continue;
			}

			// UPPER LEFT
			if(score[i][j] == score[i-1][j-1] + this.scorer.score(this.x[i], this.y[j])) {
				stringX.add(this.x[i]);
				stringY.add(this.y[j]);
				i -= 1;
				j -= 1;
				continue;
			}
		}
		
		System.out.println(stringX);
		System.out.println(stringY);
	}
}

class Pair<K, V> {

    private final K element0;
    private final V element1;

    public static <K, V> Pair<K, V> factory(K element0, V element1) {
        return new Pair<K, V>(element0, element1);
    }

    public Pair(K element0, V element1) {
        this.element0 = element0;
        this.element1 = element1;
    }

    public K getLeft() {
        return element0;
    }

    public V getRight() {
        return element1;
    }

}