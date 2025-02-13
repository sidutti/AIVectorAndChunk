package com.sidutti.charlie.tool;

import com.google.cloud.documentai.v1.Document;
import com.sidutti.charlie.model.Element;
import com.sidutti.charlie.model.ExtractedDocument;
import com.sidutti.charlie.model.Page;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransformerUtil {

    public ExtractedDocument transform(Document input) {
        List<Element> response = getDocumentBlock(input);
        List<Page> pages = new ArrayList<>();
        response.stream()
                .collect(Collectors.groupingBy(Element::pageNumber))
                .forEach((k, v) -> pages.add(new Page(v, null, null, k, null)));
        return new ExtractedDocument(pages);
    }

    private List<Element> getDocumentBlock(Document input) {
        List<Element> response = new ArrayList<>();
        for (Document.DocumentLayout.DocumentLayoutBlock block : input.getDocumentLayout().getBlocksList()) {
            processInnerSubBlocks(block, response);
        }
        return response;
    }

    private void processSubBlock(Document.DocumentLayout.DocumentLayoutBlock block, List<Element> response) {
        processInnerSubBlocks(block, response);
    }

    private void processInnerSubBlocks(Document.DocumentLayout.DocumentLayoutBlock block, List<Element> response) {
        if (block.hasTextBlock()) {
            response.add(processTextBlock(block));
            processBlock(block, response);
        }
        if (block.hasListBlock()) {
            response.add(processListBlock(block));
            processBlock(block, response);
        }
        if (block.hasTableBlock()) {
            response.add(processTableBlock(block));
            processBlock(block, response);
        }
    }

    private void processBlock(Document.DocumentLayout.DocumentLayoutBlock block, List<Element> response) {
        for (Document.DocumentLayout.DocumentLayoutBlock documentLayoutBlock : block.getTextBlock().getBlocksList()) {
            if (!documentLayoutBlock.getTextBlock().getText().isBlank()) {
                response.add(new Element(block.getBlockId(), block.getTextBlock().getType(), block.getTextBlock().getText(), block.getPageSpan().getPageStart()));
            }
            processSubBlock(documentLayoutBlock, response);
        }
    }

    private Element processTextBlock(Document.DocumentLayout.DocumentLayoutBlock block) {
        return new Element(block.getBlockId(), block.getTextBlock().getType(), block.getTextBlock().getText(), block.getPageSpan().getPageStart());
    }

    private Element processTableBlock(Document.DocumentLayout.DocumentLayoutBlock block) {
        StringBuilder data = new StringBuilder();
        for (Document.DocumentLayout.DocumentLayoutBlock.LayoutTableRow headers : block.getTableBlock().getHeaderRowsList()) {
            processCells(data, headers);
        }
        for (Document.DocumentLayout.DocumentLayoutBlock.LayoutTableRow bodyRows : block.getTableBlock().getBodyRowsList()) {
            processCells(data, bodyRows);
        }
        return new Element(block.getBlockId(), "Table", data.toString(), block.getPageSpan().getPageStart());
    }

    private void processCells(StringBuilder data, Document.DocumentLayout.DocumentLayoutBlock.LayoutTableRow headers) {
        for (Document.DocumentLayout.DocumentLayoutBlock.LayoutTableCell cell : headers.getCellsList()) {
            for (Document.DocumentLayout.DocumentLayoutBlock block : cell.getBlocksList()) {
                data.append("|")
                        .append(block.getTextBlock().getText());
            }
            data.append("|");
        }
        data.append(StringUtil.NEWLINE);
    }

    private Element processListBlock(Document.DocumentLayout.DocumentLayoutBlock block) {
        return new Element(block.getBlockId(), block.getListBlock().getType(), block.getTextBlock().getText(), block.getPageSpan().getPageStart());
    }


}
