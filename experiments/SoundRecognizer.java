import java.util.ArrayList;

/**
 * When combined with a speech recognition service, can be used to determine what
 *  words that a user has spoken within a provided prompt.
 */
public class SoundRecognizer
{
	public static final int bufferSize = 1;

	protected String prompt;
	protected SoundRecognitionAlgorithm recognizer;

	/**
	 * Instantiates a new recognizer with the provided prompt.
	 */
	public SoundRecognizer(SoundRecognitionAlgorithm recognizer, String prompt)
	{
		this.recognizer = recognizer;
		this.prompt = prompt;
	}

	/**
	 * Feed a single word that the user has spoken into the recognizer.
	 */
	public void feed(String word)
	{

	}

	/**
	 * Returns the prompt text.
	 */
	public String getPrompt()
	{
		return this.prompt;
	}

	/**
	 * Returns the position of the word in the prompt that the user is currently speaking at.
	 */
	public int getPosition()
	{
		return 0;
	}

	/**
	 * Returns whether or not the user has fully spoken the prompt.
	 */
	public boolean isFinished()
	{
		return false;
	}
}