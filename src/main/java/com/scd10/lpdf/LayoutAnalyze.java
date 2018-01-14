/**
 * 
 */
package com.scd10.lpdf;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.FilteredEventListener;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.PublicITextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextChunk;

/**
 * @author SHI Chongde
 *
 */
public class LayoutAnalyze {
	PdfDocument srcDoc = null;
	PdfDocument tgtDoc = null;
	PdfReader reader = null;
	PdfWriter writer = null;
	Rectangle pageSize = null;
	List<TextBlock> allTextBlock = null;
	List<TextChunk> shrinkBlocks = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		// LayoutAnalyze pla = new LayoutAnalyze("D:\\e5_pic.pdf");
		LayoutAnalyze pla = new LayoutAnalyze("D:\\e5_pic.pdf", "D:\\example_copy.pdf");
		// pla.extractTextExample();
		pla.analyze();
		pla.drawLineBlocks();
		//pla.draw();
		pla.close();

	}

	public LayoutAnalyze(String srcfn, String tgtfn) {
		try {
			srcDoc = new PdfDocument(new PdfReader(srcfn));
			pageSize = srcDoc.getFirstPage().getPageSize();
			tgtDoc = new PdfDocument(new PdfWriter(tgtfn));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void drawLineBlocks() throws IOException {
		PdfPage page = tgtDoc.addNewPage();

		String CFONT = "C:/windows/fonts/msyh.ttf";
		int fontsize = 12;
		PdfFont cfont = PdfFontFactory.createFont(CFONT, PdfEncodings.IDENTITY_H, true);

		PdfCanvas pdfCanvas = new PdfCanvas(page);
		pdfCanvas.setFontAndSize(cfont, fontsize);
		for (TextChunk tc : shrinkBlocks) {
			Vector start = tc.getLocation().getStartLocation();
			Vector end = tc.getLocation().getEndLocation();
			float x1 = start.get(0);
			float x2 = end.get(0);
			float y1 = start.get(1);
			String text = tc.getText();
			//System.out.println(text);
			float height = cfont.getAscent(text, fontsize);

			pdfCanvas.setLineWidth(0.5f);
			pdfCanvas.setStrokeColor(ColorConstants.RED);
			Rectangle rectangle = new Rectangle(x1, y1, x2 - x1, height);
			pdfCanvas.rectangle(rectangle);
			pdfCanvas.stroke();
		}
	}

	/*
	 * draw TextBlock
	 */
	public void draw() throws IOException {
		tgtDoc.setDefaultPageSize(new PageSize(pageSize));

		// Document is with high level opreation
		// Document doc = new Document(tgtDoc);
		// PdfFont bf = PdfFontFactory.createFont(StandardFonts.HELVETICA);
		// Text title = new Text("This is a test.").setFont(bf);
		// Paragraph p = new Paragraph().add(title);
		// doc.add(p).close();

		// canvas
		PdfPage page = tgtDoc.addNewPage();

		String CFONT = "C:/windows/fonts/msyh.ttf";
		int fontsize = 12;
		PdfFont cfont = PdfFontFactory.createFont(CFONT, PdfEncodings.IDENTITY_H, true);

		PdfCanvas pdfCanvas = new PdfCanvas(page);
		pdfCanvas.setFontAndSize(cfont, fontsize);

		for (TextBlock tb : allTextBlock) {
			Vector start = tb.getStart();
			Vector end = tb.getEnd();
			float x1 = start.get(0);
			float x2 = end.get(0);
			float y1 = start.get(1);
			String text = tb.getText();
			float height = cfont.getAscent(text, fontsize);

			pdfCanvas.setLineWidth(1);
			pdfCanvas.setStrokeColor(ColorConstants.RED);
			Rectangle rectangle = new Rectangle(x1, y1, x2 - x1, height);
			pdfCanvas.rectangle(rectangle);
			pdfCanvas.stroke();
		}
	}

	public static String toUnicode(String s) {
		char[] chars = s.toCharArray();
		String returnStr = "";
		for (int i = 0; i < chars.length; i++) {
			returnStr += "\\u" + Integer.toString(chars[i], 16);
		}
		return returnStr;
	}

	/*
	 * 直接抽取所有文字，缺省的itext功能不能够分栏，同一行的内容会显示在一行
	 */
	public void extractTextExample() {
		FilteredEventListener listener = new FilteredEventListener();
		LocationTextExtractionStrategy extractionStrategy = listener
				.attachEventListener(new LocationTextExtractionStrategy());
		new PdfCanvasProcessor(listener).processPageContent(srcDoc.getFirstPage());

		String actualText = extractionStrategy.getResultantText();
		System.out.println(actualText);
	}

	public void analyze() throws IOException {

		FilteredEventListener listener = new FilteredEventListener();
		// ITextExtractionStrategy es = listener.attachEventListener(new
		// ITextExtractionStrategy());
		PublicITextExtractionStrategy extractionStrategy = listener
				.attachEventListener(new PublicITextExtractionStrategy());
		new PdfCanvasProcessor(listener).processPageContent(srcDoc.getFirstPage());

		List<TextChunk> locationalResult = extractionStrategy.getLocationalResult();
		for (TextChunk tc : locationalResult) {
			Vector start = tc.getLocation().getStartLocation();
			Vector end = tc.getLocation().getEndLocation();
			// System.out.println(tc.getText());
		}

		allTextBlock = extractionStrategy.buildContinuousLine();
		shrinkBlocks = extractionStrategy.shrinkBlocks();
		System.out.println("---------lines:" + shrinkBlocks.size());
		// findFontInForm();
		// showFontInfo();

	}

	/*
	 * 将一个页面下所有的TextBlock区域进行组织，划分成文本块。
	 */
	public List<TextBlock> orginizeTextBlock(List<TextBlock> lineblocks) {
		return null;
	}

	/*
	 * 显示字体目录的所有字体
	 */
	public void showFontInfo() {
		PdfFontFactory.registerDirectory("C:\\Windows\\Fonts");
		Set<String> fonts = new HashSet<String>(FontProgramFactory.getRegisteredFonts());
		for (String fontname : fonts) {
			try {
				PdfFont font = PdfFontFactory.createRegisteredFont(fontname, PdfEncodings.IDENTITY_H);
				System.out.println(font);
			} catch (Exception e) {

				e.printStackTrace();
				;
			}

		}
	}

	/*
	 * 显示一个pdf页面中的字体信息
	 */
	public PdfFont findFontInForm() throws IOException {
		PdfDictionary acroForm = srcDoc.getCatalog().getPdfObject().getAsDictionary(PdfName.AcroForm);
		if (acroForm == null) {
			return null;
		}
		PdfDictionary dr = acroForm.getAsDictionary(PdfName.DR);
		if (dr == null) {
			return null;
		}
		PdfDictionary font = dr.getAsDictionary(PdfName.Font);
		if (font == null) {
			return null;
		}
		for (PdfName key : font.keySet()) {
			System.out.println("Found");
			System.out.println(key);
			/*
			 * if (key.equals(fontName)) { return
			 * PdfFontFactory.createFont(font.getAsDictionary(key)); }
			 */
		}
		return null;
	}

	public void close() {
		srcDoc.close();
		tgtDoc.close();
	}

}
