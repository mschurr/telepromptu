package com.telepromptu;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.EncoderException;
import java.util.ArrayList;
import java.util.Arrays;

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

	/**
	 * Initializes the speech traverser.
	 * @param content The content of the speech that the user will say.
	 */
	public SuperSpeechTraverser(String content) {
		this.soundex = new Soundex();
		this.content = content;
		this.words = new ArrayList<String>();
		this.currentWord = 0;
		for (String word : content.split(" ")) {
			this.words.add(word);
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
		SuperAligner aligner = new SuperAligner();

		ArrayList<String> spokenWords = new ArrayList<String>();
		for (String word : recognizedText.split(" ")) {
			spokenWords.add(word);
		}
		
		int idxL = Math.max(0, currentWord - 3);
		int idxR = Math.min(words.size(), currentWord + spokenWords.size() + 4);
		
		Alignment alignment = aligner.global_pairwise_alignment(words.subList(idxL, idxR), spokenWords);
		
//		for(int i = 0; i < alignment.xprime.size(); i++) {
//			System.out.format("%s %s\n", alignment.xprime.get(i), alignment.yprime.get(i));
//		}

		for(int i = alignment.yprime.size() - 1; i >= 0; i--) {
			if(!alignment.yprime.get(i).equals("-")) {
				this.currentWord = idxL + i;
				break;
			}
		}
	}
}
