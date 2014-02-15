
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.EncoderException;

public class SoundexRecognitionAlgorithm extends SoundRecognitionAlgorithm
{
	protected Soundex soundex;

	public SoundexRecognitionAlgorithm()
	{
		this.soundex = new Soundex();

	}

	public int getThreshold()
	{
		return 10;
	}

	public int distance(String word1, String word2)
	{
		try {
			return Math.abs(this.soundex.difference(word1,word2));
		}
		catch(EncoderException e) {
			throw new RuntimeException("Soundex Matching Failed!");
		}
	}
}