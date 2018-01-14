/**
 * 
 */
package com.itextpdf.kernel.pdf.canvas.parser.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.itextpdf.kernel.geom.LineSegment;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.canvas.CanvasTag;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.scd10.lpdf.TextBlock;

/**
 * @author SHI Chongde
 *
 */
public class PublicITextExtractionStrategy implements ITextExtractionStrategy {

	private static boolean DUMP_STATE = false;

	private List<TextChunk> locationalResult = new ArrayList<TextChunk>();

	private ITextChunkLocationStrategy tclStrat;

	private boolean useActualText = false;

	private boolean rightToLeftRunDirection = false;

	private TextRenderInfo lastTextRenderInfo;

	public PublicITextExtractionStrategy() {
		this(new ITextChunkLocationStrategy() {
			public ITextChunkLocation createLocation(TextRenderInfo renderInfo, LineSegment baseline) {
				return new TextChunkLocationDefaultImp(baseline.getStartPoint(), baseline.getEndPoint(),
						renderInfo.getSingleSpaceWidth());
			}
		});
	}

	public PublicITextExtractionStrategy(ITextChunkLocationStrategy strat) {
		tclStrat = strat;
	}

	public List<TextChunk> getLocationalResult() {
		return locationalResult;
	}

	/**
	 * Changes the behavior of text extraction so that if the parameter is set
	 * to {@code true}, /ActualText marked content property will be used instead
	 * of raw decoded bytes. Beware: the logic is not stable yet.
	 *
	 * @param useActualText
	 *            true to use /ActualText, false otherwise
	 * @return this object
	 */
	public PublicITextExtractionStrategy setUseActualText(boolean useActualText) {
		this.useActualText = useActualText;
		return this;
	}

	/**
	 * Sets if text flows from left to right or from right to left. Call this
	 * method with <code>true</code> argument for extracting Arabic, Hebrew or
	 * other text with right-to-left writing direction.
	 *
	 * @param rightToLeftRunDirection
	 *            value specifying whether the direction should be right to left
	 * @return this object
	 */
	public PublicITextExtractionStrategy setRightToLeftRunDirection(boolean rightToLeftRunDirection) {
		this.rightToLeftRunDirection = rightToLeftRunDirection;
		return this;
	}

	/**
	 * Gets the value of the property which determines if /ActualText will be
	 * used when extracting the text
	 *
	 * @return true if /ActualText value is used, false otherwise
	 */
	public boolean isUseActualText() {
		return useActualText;
	}

	public void eventOccurred(IEventData data, EventType type) {
		if (type.equals(EventType.RENDER_TEXT)) {
			TextRenderInfo renderInfo = (TextRenderInfo) data;
			LineSegment segment = renderInfo.getBaseline();
			/*
			 * 注释掉是因为我们想先过滤掉上下标的文字，如果要用的话取消注释； if (renderInfo.getRise() != 0) {
			 * // remove the rise from the baseline - we do this because the
			 * text from a super/subscript render operations should probably be
			 * considered as part of the baseline of the text the super/sub is
			 * relative to Matrix riseOffsetTransform = new Matrix(0,
			 * -renderInfo.getRise()); segment =
			 * segment.transformBy(riseOffsetTransform); }
			 */

			if (useActualText) {
				CanvasTag lastTagWithActualText = lastTextRenderInfo != null
						? findLastTagWithActualText(lastTextRenderInfo.getCanvasTagHierarchy()) : null;
				if (lastTagWithActualText != null
						&& lastTagWithActualText == findLastTagWithActualText(renderInfo.getCanvasTagHierarchy())) {
					// Merge two text pieces, assume they will be in the same
					// line
					TextChunk lastTextChunk = locationalResult.get(locationalResult.size() - 1);
					Vector mergedStart = new Vector(
							Math.min(lastTextChunk.getLocation().getStartLocation().get(0),
									segment.getStartPoint().get(0)),
							Math.min(lastTextChunk.getLocation().getStartLocation().get(1),
									segment.getStartPoint().get(1)),
							Math.min(lastTextChunk.getLocation().getStartLocation().get(2),
									segment.getStartPoint().get(2)));
					Vector mergedEnd = new Vector(
							Math.max(lastTextChunk.getLocation().getEndLocation().get(0), segment.getEndPoint().get(0)),
							Math.max(lastTextChunk.getLocation().getEndLocation().get(1), segment.getEndPoint().get(1)),
							Math.max(lastTextChunk.getLocation().getEndLocation().get(2),
									segment.getEndPoint().get(2)));
					TextChunk merged = new TextChunk(lastTextChunk.getText(),
							tclStrat.createLocation(renderInfo, new LineSegment(mergedStart, mergedEnd)));
					locationalResult.set(locationalResult.size() - 1, merged);
				} else {
					String actualText = renderInfo.getActualText();
					TextChunk tc = new TextChunk(actualText != null ? actualText : renderInfo.getText(),
							tclStrat.createLocation(renderInfo, segment));
					locationalResult.add(tc);
				}
			} else {
				TextChunk tc = new TextChunk(renderInfo.getText(), tclStrat.createLocation(renderInfo, segment));
				locationalResult.add(tc);
			}

			lastTextRenderInfo = renderInfo;
		}
	}

	public Set<EventType> getSupportedEvents() {
		return null;
	}

	// main code is used by buildTextBlock()
	public String getResultantText() {
		return "";
	}

	public List<TextChunk> shrinkBlocks() {
		if (DUMP_STATE)
			dumpState();

		List<TextChunk> textChunks = new ArrayList<TextChunk>(locationalResult);
		sortWithMarks(textChunks);
		List<TextChunk> blocks = new ArrayList<TextChunk>();

		TextChunk lastChunk = null;
		TextChunk tempChunk = null;
		for (TextChunk chunk : textChunks) {
			//System.out.println(chunk.getText());
			if (lastChunk == null) {
				tempChunk = chunk;
			} else {
				if (chunk.sameLine(lastChunk)) {
					float space = chunk.getLocation().getStartLocation().get(0)
							- lastChunk.getLocation().getEndLocation().get(0);
					System.out.println(chunk.getText()+"|||"+lastChunk.getText()+"|||"+space+"|||"+chunk.getLocation().getCharSpaceWidth());
					if (isChunkAtWordBoundary(chunk, lastChunk)
							&& (space / chunk.getLocation().getCharSpaceWidth() > 1.5)) {
						blocks.add(tempChunk);
						tempChunk = chunk;
					} else {
						tempChunk = TextChunkExtra.concatTextChunk(tempChunk, chunk);
					}
				} else {
					blocks.add(tempChunk);
					tempChunk = chunk;
				}
			}
			//blocks.add(tempChunk);
			lastChunk = chunk;
		}
		return blocks;
	}

	public List<TextBlock> buildContinuousLine() {
		if (DUMP_STATE)
			dumpState();

		List<TextChunk> textChunks = new ArrayList<TextChunk>(locationalResult);
		sortWithMarks(textChunks);

		StringBuilder sb = new StringBuilder();
		List<TextBlock> richlines = new ArrayList<TextBlock>();
		TextChunk lastChunk = null;
		TextBlock temprl = null;
		for (TextChunk chunk : textChunks) {
			if (lastChunk == null) {
				// ---------------------------

				temprl = new TextBlock(chunk);
				// ---------------------------
				sb.append(chunk.text);
			} else {
				if (chunk.sameLine(lastChunk)) {
					// we only insert a blank space if the trailing character of
					// the previous string wasn't a space, and the leading
					// character of the current string isn't a space
					if (isChunkAtWordBoundary(chunk, lastChunk) && !startsWithSpace(chunk.text)
							&& !endsWithSpace(lastChunk.text)) {
						sb.append(' ');
					}
					// -----------------------
					temprl.append(chunk);
					// -----------------------
					sb.append(chunk.text);
				} else {

					sb.append('\n');
					sb.append(chunk.text);
					// -----------------------
					if (temprl != null) {
						if (temprl.getText().trim().length() != 0) {
							richlines.add(temprl);
							// System.out.println("ADD continues line: <<< "+
							// temprl.getText()+" >>>");
							// System.out.println(temprl.getStart()+" |||
							// "+temprl.getEnd());
						}
					}
					temprl = new TextBlock(chunk);
					// -----------------------
				}
			}
			lastChunk = chunk;
		}

		return richlines;
	}

	/**
	 * Determines if a space character should be inserted between a previous
	 * chunk and the current chunk. This method is exposed as a callback so
	 * subclasses can fine time the algorithm for determining whether a space
	 * should be inserted or not. By default, this method will insert a space if
	 * the there is a gap of more than half the font space character width
	 * between the end of the previous chunk and the beginning of the current
	 * chunk. It will also indicate that a space is needed if the starting point
	 * of the new chunk appears *before* the end of the previous chunk (i.e.
	 * overlapping text).
	 *
	 * @param chunk
	 *            the new chunk being evaluated
	 * @param previousChunk
	 *            the chunk that appeared immediately before the current chunk
	 * @return true if the two chunks represent different words (i.e. should
	 *         have a space between them). False otherwise.
	 */
	protected boolean isChunkAtWordBoundary(TextChunk chunk, TextChunk previousChunk) {
		return chunk.getLocation().isAtWordBoundary(previousChunk.getLocation());
	}

	/**
	 * Checks if the string starts with a space character, false if the string
	 * is empty or starts with a non-space character.
	 *
	 * @param str
	 *            the string to be checked
	 * @return true if the string starts with a space character, false if the
	 *         string is empty or starts with a non-space character
	 */
	private boolean startsWithSpace(String str) {
		return str.length() != 0 && str.charAt(0) == ' ';
	}

	/**
	 * Checks if the string ends with a space character, false if the string is
	 * empty or ends with a non-space character
	 *
	 * @param str
	 *            the string to be checked
	 * @return true if the string ends with a space character, false if the
	 *         string is empty or ends with a non-space character
	 */
	private boolean endsWithSpace(String str) {
		return str.length() != 0 && str.charAt(str.length() - 1) == ' ';
	}

	/**
	 * Used for debugging only
	 */
	private void dumpState() {
		for (TextChunk location : locationalResult) {
			location.printDiagnostics();
			System.out.println();
		}
	}

	private CanvasTag findLastTagWithActualText(List<CanvasTag> canvasTagHierarchy) {
		CanvasTag lastActualText = null;
		for (CanvasTag tag : canvasTagHierarchy) {
			if (tag.getActualText() != null) {
				lastActualText = tag;
				break;
			}
		}
		return lastActualText;
	}

	private void sortWithMarks(List<TextChunk> textChunks) {
		Map<TextChunk, TextChunkMarks> marks = new HashMap<TextChunk, TextChunkMarks>();
		List<TextChunk> toSort = new ArrayList<TextChunk>();

		for (int markInd = 0; markInd < textChunks.size(); markInd++) {
			ITextChunkLocation location = textChunks.get(markInd).getLocation();
			if (location.getStartLocation().equals(location.getEndLocation())) {
				boolean foundBaseToAttachTo = false;
				for (int baseInd = 0; baseInd < textChunks.size(); baseInd++) {
					if (markInd != baseInd) {
						ITextChunkLocation baseLocation = textChunks.get(baseInd).getLocation();
						if (!baseLocation.getStartLocation().equals(baseLocation.getEndLocation())
								&& TextChunkLocationDefaultImp.containsMark(baseLocation, location)) {
							TextChunkMarks currentMarks = marks.get(textChunks.get(baseInd));
							if (currentMarks == null) {
								currentMarks = new TextChunkMarks();
								marks.put(textChunks.get(baseInd), currentMarks);
							}

							if (markInd < baseInd) {
								currentMarks.preceding.add(textChunks.get(markInd));
							} else {
								currentMarks.succeeding.add(textChunks.get(markInd));
							}

							foundBaseToAttachTo = true;
							break;
						}
					}
				}

				if (!foundBaseToAttachTo) {
					toSort.add(textChunks.get(markInd));
				}
			} else {
				toSort.add(textChunks.get(markInd));
			}
		}

		Collections.sort(toSort,
				new TextChunkLocationBasedComparator(new DefaultTextChunkLocationComparator(!rightToLeftRunDirection)));

		textChunks.clear();

		for (TextChunk current : toSort) {
			TextChunkMarks currentMarks = marks.get(current);
			if (currentMarks != null) {
				if (!rightToLeftRunDirection) {
					for (int j = 0; j < currentMarks.preceding.size(); j++) {
						textChunks.add(currentMarks.preceding.get(j));
					}
				} else {
					for (int j = currentMarks.succeeding.size() - 1; j >= 0; j--) {
						textChunks.add(currentMarks.succeeding.get(j));
					}
				}
			}
			textChunks.add(current);
			if (currentMarks != null) {
				if (!rightToLeftRunDirection) {
					for (int j = 0; j < currentMarks.succeeding.size(); j++) {
						textChunks.add(currentMarks.succeeding.get(j));
					}
				} else {
					for (int j = currentMarks.preceding.size() - 1; j >= 0; j--) {
						textChunks.add(currentMarks.preceding.get(j));
					}
				}
			}
		}
	}

	public interface ITextChunkLocationStrategy {
		ITextChunkLocation createLocation(TextRenderInfo renderInfo, LineSegment baseline);
	}

	private static class TextChunkMarks {
		List<TextChunk> preceding = new ArrayList<TextChunk>();
		List<TextChunk> succeeding = new ArrayList<TextChunk>();
	}

}
