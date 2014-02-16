import java.util.ArrayList;

public class SuperSpeechTraverserTester
{
	public static void main(String[] args)
	{
		/*
		SuperAligner aligner = new SuperAligner();
		ArrayList<String> prompt = aligner.stringToWords("Hello! My name is Matthew. I am a student in Houston, Texas. I am very well known for all of the applications I created, including the book exchange.");
		ArrayList<String> query = aligner.stringToWords("my name matthew student exchange");
		Alignment alignment = aligner.global_pairwise_alignment(prompt, query);
		System.out.println(prompt);
		System.out.println(query);
		aligner.printAlignment(alignment);
		*/

		String prompt = "Hello; My name is Waseem. I am a computer science major. I founded Rice Apps in 2013 because I like to code and be generally cool. I also think that Facebook is awesome because PHP.";
		SuperSpeechTraverser traverser = new SuperSpeechTraverser(prompt);
		traverser.inputSpeech("I'm Waseem, a computer science student at Rice. I founded rice apps because uh");
		System.out.println(traverser.getCurrentWordString());

		traverser.inputSpeech("i like to code");
		System.out.println(traverser.getCurrentWordString());

		traverser.inputSpeech("and uhh i'm pretty cool");
		System.out.println(traverser.getCurrentWordString());

		traverser.inputSpeech("i also love facebook becuz they use php");
		System.out.println(traverser.getCurrentWordString());
	}

	
}