/**
 * 表示pdf文档的一块文本区域
 * 首先通过PublicITextExtractionStrategy::buildContinuousLine()按照行进行合并
 * 然后使用LayoutAnalyze::orginizeTextBlock()进行组块
 */
package com.scd10.lpdf;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextChunkLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextChunk;

/**
 * @author Chongde SHI
 *
 */
public class TextBlock {
	private Rectangle scope;
	private Vector start;
	private Vector end;
	private String text = "";
	
	public TextBlock(TextChunk chunk){
		this.start = chunk.getLocation().getStartLocation();
		this.end = chunk.getLocation().getEndLocation();
		this.text = chunk.getText();
	}
	
	public void append(TextChunk chunk){
		ITextChunkLocation chunkpos = chunk.getLocation();
		Vector start = chunkpos.getStartLocation();  // need check
		String txt = chunk.getText();
		if (txt.trim().length() != 0){
			Vector end = chunkpos.getEndLocation();
			this.end = end;
			text += chunk.getText();
		}
		
		//System.out.println(this.start+" ||| "+this.end);
		
		//scope.
	}
	
	public int length(){
		return this.text.length();
	}
	
	public String getText(){
		return this.text;
	}
	
	public Vector getStart(){
		return this.start;
	}
	
	public Vector getEnd(){
		return this.end;
	}
}
