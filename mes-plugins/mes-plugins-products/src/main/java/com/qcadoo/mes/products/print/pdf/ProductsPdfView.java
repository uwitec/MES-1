/**
 * ********************************************************************
 * Code developed by amazing QCADOO developers team.
 * Copyright (c) Qcadoo Limited sp. z o.o. (2010)
 * ********************************************************************
 */

package com.qcadoo.mes.products.print.pdf;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.view.document.AbstractPdfView;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.qcadoo.mes.api.TranslationService;
import com.qcadoo.mes.internal.DefaultEntity;
import com.qcadoo.mes.products.print.pdf.util.PdfPageNumbering;
import com.qcadoo.mes.products.print.pdf.util.PdfUtil;

public abstract class ProductsPdfView extends AbstractPdfView {

    @Autowired
    private TranslationService translationService;

    protected DecimalFormat df;

    @Value("${windowsFonts}")
    private String windowsFontsPath;

    @Value("${macosFonts}")
    private String macosFontsPath;

    @Value("${linuxFonts}")
    private String linuxFontsPath;

    @Override
    protected final void buildPdfDocument(final Map<String, Object> model, final Document document, final PdfWriter writer,
            final HttpServletRequest request, final HttpServletResponse response) throws Exception {

        Locale locale = PdfUtil.retrieveLocaleFromRequestCookie(request);
        df = (DecimalFormat) DecimalFormat.getInstance(locale);
        DefaultEntity entity = (DefaultEntity) model.get("entity");
        String fileName = addContent(document, entity, locale);
        String text = translationService.translate("products.report.endOfReport", locale);
        PdfUtil.addEndOfDocument(document, writer, text);
        response.setHeader("Content-disposition", "attachment; filename=" + fileName + PdfUtil.PDF_EXTENSION);
        writer.addJavaScript("this.print(false);", false);
    }

    @Override
    protected final void prepareWriter(final Map<String, Object> model, final PdfWriter writer, final HttpServletRequest request)
            throws DocumentException {
        Locale locale = PdfUtil.retrieveLocaleFromRequestCookie(request);
        super.prepareWriter(model, writer, request);
        writer.setPageEvent(new PdfPageNumbering(translationService.translate("products.report.page", locale), translationService
                .translate("products.report.in", locale), getFontsPath()));
    }

    @Override
    protected final Document newDocument() {
        Document doc = super.newDocument();
        doc.setMargins(40, 40, 60, 60);
        return doc;
    }

    @Override
    protected final void buildPdfMetadata(final Map<String, Object> model, final Document document,
            final HttpServletRequest request) {
        addTitle(document, PdfUtil.retrieveLocaleFromRequestCookie(request));
        PdfUtil.addMetaData(document);
    }

    protected String addContent(final Document document, final DefaultEntity entity, final Locale locale)
            throws DocumentException, IOException {
        document.add(new Paragraph("", PdfUtil.getArialRegular9Dark()));
        return "document";
    }

    protected final TranslationService getTranslationService() {
        return translationService;
    }

    protected abstract void addTitle(final Document document, final Locale locale);

    protected final String getFontsPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return windowsFontsPath;
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return macosFontsPath;
        } else if (SystemUtils.IS_OS_LINUX) {
            return linuxFontsPath;
        }
        return null;
    }
}
