package com.sidutti.charlie.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ExpandedTitleContentHandler;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class WordToHtmlService {
    public static void main(String[] args) throws IOException {
        WordToHtmlService wordToHtmlService = new WordToHtmlService();
        wordToHtmlService.understandPdf("");

    }
    public void understandPdf(String word) throws IOException {
        try (var document = new PdfDocument(new PdfReader("/nas/CodeDataset/pdfs/Language Models are Unsupervised Multitask Learners.pdf"))) {
            var strategy = new SimpleTextExtractionStrategy();
            for (int i = 1; i < document.getNumberOfPages(); i++) {
                String text = PdfTextExtractor.getTextFromPage(document.getPage(i), strategy);
                System.out.println(text);
            }
        }

    }
    public void wordToHtml(String word) {


        try {
            FileInputStream finStream = new FileInputStream("/nas/CodeDataset/pdfs/Language Models are Unsupervised Multitask Learners.pdf");
            AutoDetectParser tikaParser = new AutoDetectParser();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler handler = factory.newTransformerHandler();


            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
            handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "utf-8");
            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
            handler.setResult(new StreamResult(out));
            ExpandedTitleContentHandler handler1 = new ExpandedTitleContentHandler(handler);
            tikaParser.parse(finStream, handler1, new Metadata());
            String html = out.toString(StandardCharsets.UTF_8);
            System.out.println();
            FileOutputStream fos = new FileOutputStream("/nas/CodeDataset/sample.html");


            try {
                BufferedWriter bout = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
                bout.write(html);
                bout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
