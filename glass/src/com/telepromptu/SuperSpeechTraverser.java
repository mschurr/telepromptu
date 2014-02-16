//package com.telepromptu;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.EncoderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A speech traverser that figures out where in the speech the user is
 * based of off live recognized voice input.
 * @author Matthew Schurr <mschurr@rice.edu>
 *
 */
public class SuperSpeechTraverser {
	private String content;
	private ArrayList<String> words;
	private int currentWord;
	private Soundex soundex;	
	private SuperAligner aligner;

	/**
	 * Initializes the speech traverser.
	 * @param content The content of the speech that the user will say.
	 */
	public SuperSpeechTraverser(String content) {
		this.soundex = new Soundex();
		this.aligner = new SuperAligner();
		this.content = content;
		this.words = this.aligner.stringToWords(content);
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
	 * Gets the string version of the current word.
	 */
	public String getCurrentWordString() {
		return this.words.get(this.currentWord);
	}
	
	/**
	 * Traverses the model past the text the user just spoke based off of
	 * speech recognition.
	 * @param recognizedText the text received from the speech recognition software
	 */
	public void inputSpeech(String recognizedText) {
		ArrayList<String> spokenWords = this.aligner.stringToWords(recognizedText);

		int idxL = Math.max(0, currentWord - 3);
		int idxR = Math.min(words.size(), currentWord + spokenWords.size() + 6);
		
		List<String> matchWords = words.subList(idxL, idxR);
		Alignment alignment = aligner.global_pairwise_alignment(matchWords, spokenWords);
		//Alignment alignment = this.aligner.global_pairwise_alignment(this.words, spokenWords);
		this.aligner.printAlignment(alignment);

		// Find the word...
		int wordOffset = 0;

		for(int i = 0; i < alignment.xprime.size(); i++) {
			if(alignment.xprime.get(i).equals("-")) {
				wordOffset++;
				continue;
			}

			if(alignment.yprime.get(i).equals("-"))
				continue;

			String s1 = this.soundex.encode(alignment.xprime.get(i));
			String s2 = this.soundex.encode(alignment.yprime.get(i));
			if(!s1.equals(s2))
				continue;

			this.currentWord = idxL+i-wordOffset;
		}
	}
}
