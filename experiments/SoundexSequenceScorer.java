
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.EncoderException;

public class SoundexSequenceScorer implements SequenceScorer
{
	protected Soundex soundex;

	public SoundexSequenceScorer()
	{
		this.soundex = new Soundex();

	}

	public int score(String a, String b)
	{
		if(a == null || b == null) {
			return 0;
		}

		try {
			return 100 - Math.abs(this.soundex.difference(a,b));
		}
		catch(EncoderException e) {
			throw new RuntimeException("Soundex Matching Failed!");
		}
	}
}