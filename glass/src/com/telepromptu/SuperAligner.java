//package com.telepromptu;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.EncoderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SuperAligner
{
	protected Soundex soundex;

	public SuperAligner()
	{
		this.soundex = new Soundex();
	}

	public int score(String word1, String word2)
	{
		if(word1.equals("-") || word2.equals("-"))
			return 0;

		String word1s = this.soundex.encode(word1);
		String word2s = this.soundex.encode(word2);

		if(word1s.equals(word2s))
			return 1;
		return 0;
	}

	public void pairwise_matrix_cell(int i, int j, ArrayList<String> x, ArrayList<String> y, int[][] s, int[][] p)
	{
		if(i == 0) {
			if(j == 0) {
				s[i][j] = 0;
			}
			else {
				s[i][j] = s[i][j-1] + this.score("-", y.get(j));
				p[i][j] = 0;
			}
		}
		else if(j == 0) {
			s[i][j] = s[i-1][j] + this.score(x.get(i), "-");
			p[i][j] = 90;
		}
		else {
			int opt1 = s[i-1][j-1] + this.score(x.get(i), y.get(j));
			int opt2 = s[i-1][j] + this.score(x.get(i), "-");
			int opt3 = s[i][j-1] + this.score("-", y.get(j));

			s[i][j] = Math.max(opt1, Math.max(opt2, opt3));

			if(s[i][j] == opt1) {
				p[i][j] = 45;
			}
			else if(s[i][j] == opt2) {
				p[i][j] = 90;
			}
			else {
				p[i][j] = 0;
			}
		}
	}

	public Traceback pairwise_traceback(int i, int j, ArrayList<String> x, ArrayList<String> y, int[][] s, int[][] p, String mode)
	{
		ArrayList<String> xprime = new ArrayList<>();
		ArrayList<String> yprime = new ArrayList<>();

		while((mode.equals("global") && (i != 0 || j != 0)) || (mode.equals("local") && s[i][j] != 0)) {
			if(p[i][j] == 45) {
				xprime.add(0, x.get(i));
				yprime.add(0, y.get(j));
				i -= 1;
				j -= 1;
			}
			else if(p[i][j] == 90) {
				xprime.add(0, x.get(i));
				yprime.add(0, "-");
				i -= 1;
			}
			else if(p[i][j] == 0) {
				xprime.add(0, "-");
				yprime.add(0, y.get(j));
				j -= 1;
			}
		}

    	Traceback traceback = new Traceback();
    	traceback.xprime = xprime;
    	traceback.yprime = yprime;
    	return traceback;
	}

	public Alignment global_pairwise_alignment(List<String> x, List<String> y)
	{
		ArrayList<String> a = new ArrayList<>();
		a.add(" ");
		a.addAll(x);

		ArrayList<String> b = new ArrayList<>();
		b.add(" ");
		b.addAll(y);

		int m = a.size();
		int n = b.size();

		int[][] s = new int[m][n]; // init to zeros
		int[][] p = new int[m][n]; // init to zeros

		for(int i = 0; i < m; i++) {
			for(int j = 0; j < n; j++) {
				this.pairwise_matrix_cell(i, j, a, b, s, p);
			}
		}

		int i = m-1;
		int j = n-1;

		Traceback trace = this.pairwise_traceback(i, j, a, b, s, p, "global");
		ArrayList<String> xprime = trace.xprime;
		ArrayList<String> yprime = trace.yprime;

		Alignment alignment = new Alignment();
		alignment.xprime = xprime;
		alignment.yprime = yprime;
		alignment.score = s[m-1][n-1];

		return alignment;
	}

	public Alignment local_pairwise_alignment(ArrayList<String> x, ArrayList<String> y)
	{
		ArrayList<String> a = new ArrayList<>();
		a.add(" ");
		a.addAll(x);

		ArrayList<String> b = new ArrayList<>();
		b.add(" ");
		b.addAll(y);

		int m = a.size();
		int n = b.size();

		int[][] s = new int[m][n]; // init to zeros
		int[][] p = new int[m][n]; // init to zeros

		int smax = Integer.MIN_VALUE;
		int smax_i = 0;
		int smax_j = 0;

		for(int i = 0; i < m; i ++) {
			for(int j = 0; j < n; j++) {
				this.pairwise_matrix_cell(i, j, a, b, s, p);

				if(s[i][j] > smax) {
					smax = s[i][j];
					smax_i = i;
					smax_j = j;
				}

				if(s[i][j] < 0)
					s[i][j] = 0;
			}
		}

		Traceback trace = this.pairwise_traceback(smax_i, smax_j, a, b, s, p, "local");
		ArrayList<String> xprime = trace.xprime;
		ArrayList<String> yprime = trace.yprime;

		Alignment alignment = new Alignment();
		alignment.xprime = xprime;
		alignment.yprime = yprime;
		alignment.score = smax;
		return alignment;
	}

	public void printAlignment(Alignment alignment)
	{
		for(int i = 0; i < alignment.xprime.size(); i++) {
			System.out.format("%1$30s %2$30s\n", alignment.xprime.get(i), alignment.yprime.get(i));
		}
	}

	public ArrayList<String> stringToWords(String prompt) {
		String[] words = prompt.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().split("\\s+");
		ArrayList<String> wordList = new ArrayList<String>();
		for(int i = 0; i < words.length; i++)
			wordList.add(words[i]);
		return wordList;
	}

	public static void main(String[] args)
	{
		SuperAligner aligner = new SuperAligner();
		ArrayList<String> x = new ArrayList<String>(Arrays.asList("my", "name", "is", "matthew", "and", "i", "study", "computer", "science"));
		ArrayList<String> y = new ArrayList<String>(Arrays.asList("my", "name", "matthew", "i", "study", "science"));
		Alignment alignment = aligner.global_pairwise_alignment(x, y);

		for(int i = 0; i < alignment.xprime.size(); i++) {
			System.out.format("%s %s\n", alignment.xprime.get(i), alignment.yprime.get(i));
		}
	}
}

class Alignment {
	public ArrayList<String> xprime;
	public ArrayList<String> yprime;
	public int score;
}

class Traceback {
	public ArrayList<String> xprime;
	public ArrayList<String> yprime;
}