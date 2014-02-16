package com.telepromptu;

import java.util.ArrayList;
import java.util.List;

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
	 * Gets the index of the current word that the speaker has traversed to.
	 * @return index of the current word in the words list
	 */
	public int getCurrentWord() {
		return currentWord;
	}
	
	/**
	 * Traverses the model past the text the user just spoke based off of
	 * speech recognition.
	 * @param recognizedText the text received from the speech recognition software
	 */
	public void inputSpeech(String recognizedText) {
		int rL = recognizedText.length();
		String bestMatch = "";
		int bestMatchLastWordIndex = -1;
		int bestScore = 0;
		// Iterate through possible speech string lengths sL
		for (int sL = Math.max(1,rL-2); sL < rL+2; sL++) {
			
			// Iterate through speech positions p
			for (int p = Math.max(currentWord - 10, 0); p < Math.min(words.size() - 1, currentWord + 10); p++) {
				int endIndex = Math.min(p + sL, words.size());
				List<String> subString = words.subList(p, endIndex);
				String match = "";
				if (subString.size() > 0) {
					match = subString.get(0);
					for (String w : subString.subList(1, subString.size())) {
						match += " " + w;
					}
				}
				int score;
				try {
					score = soundex.difference(recognizedText, match);
    				if (score > bestScore) {
    					bestMatch = match;
    					bestScore = score;
    					bestMatchLastWordIndex = endIndex;
    				}
				} catch (EncoderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		if (bestMatchLastWordIndex > -1) {
			currentWord = bestMatchLastWordIndex;
		} else {
			throw new RuntimeException("No match found for speech input " + recognizedText);
		}
		
	}
	

}
