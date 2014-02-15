
public class SoundRecognizerTester
{
	public static void main(String[] args)
	{	String prompt = "hello my name is matthew and i am awesome";
		SoundRecognitionAlgorithm recognitionAlgorithm = new SoundexRecognitionAlgorithm();
		SoundRecognizer recognizer = new SoundRecognizer(recognitionAlgorithm, prompt);
	}
}