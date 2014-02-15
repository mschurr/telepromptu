
public abstract class SoundRecognitionAlgorithm
{
	/* The threshold under which distances must be to be acceptable. */
	public abstract int getThreshold();

	/* The recognition distance between two words, word1 and word2. */
	public abstract int distance(String word1, String word2);
}

