
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

		while(i > 0 || j > 0) {
			// UP
			if(X[i][j] == X[i-1][j] + this.scorer.score()) {
				stringX.add(this.x[i]);
				stringY.add(null);
				i -= 1;
			}

			// LEFT
			if(X[i][j] == X[i][j-1] + this.scorer.score()) {
				stringX.add(null);
				stringY.add(this.y[j]);
				j -= 1;
			}

			// UPPER LEFT
			if(X[i][j] == X[i-1][j-1] + this.scorer.score()) {
				stringX.add(this.x[i]);
				stringY.add(this.y[j]);
				i -= 1;
				j -= 1;
			}
		}
		/*
		0 (left), 45 (upper left), or 90 (up)
        if P[i][j] == 45:
            Xprime = X[i] + Xprime        #Align A[i] with B[j] and traverse to the upper-left cell
            Yprime = Y[j] + Yprime
            i-=1
            j-=1
        elif P[i][j] == 90:
            Xprime = X[i] + Xprime        #Align A[i] with a dash, and traverse to the left cell
            Yprime = '-' + Yprime
            i-=1
        elif P[i][j] == 0:
            Xprime = '-' + Xprime
            Yprime = Y[j] + Yprime        #Align B[j] with a dash, and traverse to the upper cell
            j-=1
		*/
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