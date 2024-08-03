package com.sidutti.charlie.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class WordToHtmlService {
        public static void main(String[] args) {
                WordToHtmlService wordToHtmlService = new WordToHtmlService();
                wordToHtmlService.wordToHtml("");
        }

        public void wordToHtml(String word) {


                try {
                        FileInputStream finStream = new FileInputStream("/nas/CodeDataset/Thetestdata_DOC_1MB/Doc_1MB/File-7kvR3.doc");
                        HWPFDocument doc = new HWPFDocument(finStream);
                        WordExtractor wordExtract = new WordExtractor(doc);
                        Document newDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(newDocument);
                        wordToHtmlConverter.processDocument(doc);

                        StringWriter stringWriter = new StringWriter();
                        Transformer transformer = TransformerFactory.newInstance().newTransformer();

                        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
                        transformer.setOutputProperty(OutputKeys.METHOD, "html");
                        transformer.transform(new DOMSource(wordToHtmlConverter.getDocument()), new StreamResult(stringWriter));

                        String html = stringWriter.toString();
                        FileOutputStream fos = new FileOutputStream("/nas/CodeDataset/sample.html");
                        DataOutputStream dos;

                        try {
                                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
                                out.write(html);
                                out.close();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }


                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}
