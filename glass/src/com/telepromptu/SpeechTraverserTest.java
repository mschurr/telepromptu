package com.telepromptu;

import junit.framework.TestCase;

public class SpeechTraverserTest extends TestCase {
	
	private SpeechTraverser traverser;

	public void testBasic() {
		String text = "Hello my name is Waseem Ahmad! I'm a Computer Science major studying at Rice University. We're working on a teleprompter app for Google Glass.";
		traverser = new SpeechTraverser(text);
		traverser.inputSpeech("my name is Waseem");
		String lastWord = traverser.getWords().get(traverser.getCurrentWord());
		System.out.println("Last word spoken is: " + lastWord);
	}

}
