/**
 * 
 */
package com.itextpdf.kernel.pdf.canvas.parser.listener;

import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextChunkLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextChunk;

/**
 * @author Chongde SHI
 *
 */
public class TextChunkExtra extends TextChunk{
	public TextChunkExtra(String string, ITextChunkLocation loc) {
        super(string, loc);
    }
	
	public int getTextLength(){
		return this.text.length();
	}
	
	public static TextChunk concatTextChunk(TextChunk previous, TextChunk next){
		String preText = previous.getText();
		String nextText = next.getText();
		String concatText = preText+nextText;
		if (isChunkAtWordBoundary(next, previous) && !startsWithSpace(nextText) && !endsWithSpace(preText)) {
			concatText = preText + ' ' + nextText;
        }
		Vector start = previous.getLocation().getStartLocation();
		Vector end = next.getLocation().getEndLocation();
		ITextChunkLocation loc = new TextChunkLocationDefaultImp(start, end, next.getLocation().getCharSpaceWidth());
		return new TextChunk(concatText, loc);
	}
	
	protected static boolean isChunkAtWordBoundary(TextChunk chunk, TextChunk previousChunk) {
        return chunk.getLocation().isAtWordBoundary(previousChunk.getLocation());
    }
	private static boolean startsWithSpace(String str) {
        return str.length() != 0 && str.charAt(0) == ' ';
    }
	
	private static boolean endsWithSpace(String str) {
        return str.length() != 0 && str.charAt(str.length() - 1) == ' ';
    }
}
