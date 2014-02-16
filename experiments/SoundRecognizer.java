import java.util.ArrayList;

/**
 * When combined with a speech recognition service, can be used to determine what
 *  words that a user has spoken within a provided prompt.
 */
public class SoundRecognizer
{
	public final int bufferSize = 5;
	public final int matchThreshold = 4;
	public final int wordTolerance = 2;

	protected String[] prompt;
	protected String[] buffer;
	protected SoundRecognitionAlgorithm recognizer;
	protected int position;

	/**
	 * Instantiates a new recognizer with the provided prompt.
	 */
	public SoundRecognizer(SoundRecognitionAlgorithm recognizer, String prompt)
	{
		this.recognizer = recognizer;
		this.position = 0;
		this.buffer = new String[this.bufferSize];
		this.prompt = prompt.split("\\s+");
	}

	/**
	 * Feed a single word that the user has spoken into the recognizer.
	 */
	public void feed(String word)
	{
		// Shift the buffer left.
		for(int i = 0; i < this.buffer.length - 1; i++)
			this.buffer[i] = this.buffer[i+1];

		// Add the word to the end of the buffer.
		this.buffer[this.buffer.length-1] = word;

		
	}

	/**
	 * Returns the prompt text as an array of words.
	 */
	public String[] getPrompt()
	{
		return this.prompt;
	}

	/**
	 * Returns the position of the word in the prompt that the user is currently speaking at.
	 */
	public int getPosition()
	{
		return this.position;
	}

	/**
	 * Sets the position in the prompt; useful if you want to backtrack.
	 */
	public void setPosition(int position)
	{
		this.position = position;
	}

	/**
	 * Returns whether or not the user has fully spoken the prompt.
	 */
	public boolean isFinished()
	{
		return this.position >= this.prompt.length;
	}

	/**
	 * Prints out the state of the object at a given point.
	 */
	public void debug()
	{
		System.out.format("----------------------\n");
		System.out.format("SoundRecognizer Debug\n");
		System.out.format("prompt=%s\n", java.util.Arrays.toString(this.prompt));
		System.out.format("buffer=%s\n", java.util.Arrays.toString(this.buffer));
		System.out.format("pos=%d\n", this.getPosition());
		System.out.format("finished=%b\n", this.isFinished());
	}
}