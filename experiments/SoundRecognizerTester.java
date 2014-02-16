
public class SoundRecognizerTester
{
	public static void main(String[] args)
	{	
		String[] prompt = {"hello", "my", "name", "is", "matthew", "and", "i", "am", "awesome"};
		String[] words = {"what", "are", "you", "doing", "hello", "mi", "name", "s", "matthew"};

		//SoundRecognitionAlgorithm recognitionAlgorithm = new SoundexRecognitionAlgorithm();
		//SoundRecognizer recognizer = new SoundRecognizer(recognitionAlgorithm, prompt);


		/*for(int i = 0; i < words.length; i++) {
			recognizer.feed(words[i]);
			recognizer.debug();
		}*/

		SequenceScorer scorer = new SoundexSequenceScorer();
		SequenceAligner aligner = new SequenceAligner(prompt, words, scorer);
		aligner.run();
	}
}