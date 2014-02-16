package com.telepromptu;

import java.util.ArrayList;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.EncoderException;


/**
 * A speech traverser that figures out where in the speech the user is
 * based of off live recognized voice input.
 * @author Waseem Ahmad <waseem@rice.edu>
 *
 */
public class SpeechTraverser {
	
	private String content;
	private ArrayList<String> words;
	private int currentWord;  // points to the current word the user will speak
	private Soundex soundex;
	

	/**
	 * Initializes the speech traverser.
	 * @param content The content of the speech that the user will say.
	 */
	public SpeechTraverser(String content) {
		soundex = new Soundex();
		this.content = content;
		words = new ArrayList<String> ();
		currentWord = 0;
		for (String word : content.split(" ")) {
			words.add(word);
		}
	}
	
	/**
	 * Get the list of words in the document.
	 * @return ArrayList<String> words a list of words in the document
	 */
	public ArrayList<String> getWords() {
		return words;
	}
	
	/**
	 * Traverses the model past the text the user just spoke based off of
	 * speech recognition.
	 * @param recognizedText the text received from the speech recognition software
	 */
	public void inputSpeech(String recognizedText) {
		
	}
	

}
