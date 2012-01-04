/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.workPlans.print;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.qcadoo.localization.api.utils.DateUtils;
import com.qcadoo.mes.orders.util.EntityNumberComparator;
import com.qcadoo.mes.workPlans.constants.WorkPlanType;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.report.api.PrioritizedString;
import com.qcadoo.report.api.pdf.PdfDocumentService;
import com.qcadoo.report.api.pdf.PdfUtil;
import com.qcadoo.security.api.SecurityService;

@Service
public class WorkPlanPdfService2 extends PdfDocumentService {

    private static Locale currentLocale = LocaleContextHolder.getLocale();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DateUtils.DATE_FORMAT, currentLocale);

    private final int[] defaultWorkPlanColumnWidth = new int[] { 15, 25, 20, 20, 20 };

    @Autowired
    private SecurityService securityService;

    @Autowired
    private WorkPlanProductsService workPlanProductsService;

    @Override
    public String getReportTitle(Locale locale) {
        return getTranslationService().translate("workPlans.workPlan.report.title", locale);
    }

    @Override
    public void buildPdfContent(Document document, Entity workPlan, Locale locale) throws DocumentException {
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance(locale);
        decimalFormat.setMaximumFractionDigits(3);
        decimalFormat.setMinimumFractionDigits(3);

        addMainHeader(document, workPlan, locale);

        PdfPTable orderTable = PdfUtil.createTableWithHeader(5, prepareOrdersTableHeader(document, workPlan, locale), false,
                defaultWorkPlanColumnWidth);

        List<Entity> orders = workPlan.getManyToManyField("orders");
        addOrderSeries(orderTable, orders, decimalFormat);
        document.add(orderTable);
        document.add(Chunk.NEWLINE);

        addOperations(document, workPlan, locale);
    }

    void addOperations(Document document, Entity workPlan, Locale locale) throws DocumentException {
        Map<Entity, Entity> operationComponent2Order = new HashMap<Entity, Entity>();

        for (Entry<PrioritizedString, List<Entity>> entry : getOperationComponentsWithDistinction(workPlan,
                operationComponent2Order, locale).entrySet()) {
            document.add(Chunk.NEXTPAGE);
            document.add(new Paragraph(entry.getKey().getString(), PdfUtil.getArialBold11Dark()));
            for (Entity operationComponent : entry.getValue()) {
                PdfPTable operationTable = PdfUtil.createPanelTable(3);

                addOperationInfoToTheOperationHeader(operationTable, operationComponent, locale);
                addOrderInfoToTheOperationHeader(operationTable, operationComponent2Order.get(operationComponent), locale);
                operationTable.setSpacingAfter(18);
                operationTable.setSpacingBefore(9);
                document.add(operationTable);
            }
        }
    }

    void addOperationInfoToTheOperationHeader(PdfPTable operationTable, Entity operationComponent, Locale locale) {
        String operationLevel = operationComponent.getStringField("nodeNumber");
        PdfUtil.addTableCellAsTable(operationTable,
                getTranslationService().translate("workPlans.workPlan.report.operation.level", locale), operationLevel, null,
                PdfUtil.getArialBold10Dark(), PdfUtil.getArialRegular10Dark());

        String operationName = operationComponent.getBelongsToField("operation").getStringField("name");
        PdfUtil.addTableCellAsTable(operationTable,
                getTranslationService().translate("workPlans.workPlan.report.operation.name", locale), operationName, null,
                PdfUtil.getArialBold10Dark(), PdfUtil.getArialRegular10Dark());

        String operationNumber = operationComponent.getBelongsToField("operation").getStringField("number");
        PdfUtil.addTableCellAsTable(operationTable,
                getTranslationService().translate("workPlans.workPlan.report.operation.number", locale), operationNumber, null,
                PdfUtil.getArialBold10Dark(), PdfUtil.getArialRegular10Dark());
    }

    void addOrderInfoToTheOperationHeader(PdfPTable operationTable, Entity order, Locale locale) {
        Entity technology = order.getBelongsToField("technology");
        String technologyString = null;
        if (technology != null) {
            technologyString = technology.getStringField("name");
        }
        PdfUtil.addTableCellAsTable(operationTable,
                getTranslationService().translate("workPlans.workPlan.report.operation.technology", locale), technologyString,
                null, PdfUtil.getArialBold10Dark(), PdfUtil.getArialRegular10Dark());

        String orderName = order.getStringField("name");
        PdfUtil.addTableCellAsTable(operationTable,
                getTranslationService().translate("workPlans.workPlan.report.operation.orderName", locale), orderName, null,
                PdfUtil.getArialBold10Dark(), PdfUtil.getArialRegular10Dark());

        String orderNumber = order.getStringField("number");
        PdfUtil.addTableCellAsTable(operationTable,
                getTranslationService().translate("workPlans.workPlan.report.operation.orderNumber", locale), orderNumber, null,
                PdfUtil.getArialBold10Dark(), PdfUtil.getArialRegular10Dark());
    }

    PrioritizedString generateOperationSectionTitle(Entity workPlan, Entity technology, Entity operationComponent, Locale locale) {
        String type = workPlan.getStringField("type");

        PrioritizedString title = null;

        if (WorkPlanType.NO_DISTINCTION.getStringValue().equals(type)) {
            title = new PrioritizedString(getTranslationService().translate("workPlans.workPlan.report.title.noDistinction",
                    locale));
        } else if (WorkPlanType.BY_END_PRODUCT.getStringValue().equals(type)) {
            Entity endProduct = technology.getBelongsToField("product");

            StringBuilder titleBuilder = new StringBuilder();
            titleBuilder.append(getTranslationService().translate("workPlans.workPlan.report.title.byEndProduct", locale));
            titleBuilder.append(" {");
            titleBuilder.append(endProduct.getStringField("name"));
            titleBuilder.append("}");
            title = new PrioritizedString(titleBuilder.toString());
        } else if (WorkPlanType.BY_WORKSTATION_TYPE.getStringValue().equals(type)) {
            Entity workstation = operationComponent.getBelongsToField("operation").getBelongsToField("workstationType");

            if (workstation == null) {
                title = new PrioritizedString(getTranslationService().translate(
                        "workPlans.workPlan.report.title.noWorkstationType", locale), 1);
            } else {
                StringBuilder titleBuilder = new StringBuilder();
                titleBuilder.append(getTranslationService()
                        .translate("workPlans.workPlan.report.title.byWorkstationType", locale));
                titleBuilder.append(" {");
                titleBuilder.append(workstation.getStringField("name"));
                titleBuilder.append("}");
                title = new PrioritizedString(titleBuilder.toString());
            }
        } else if (WorkPlanType.BY_DIVISION.getStringValue().equals(type)) {
            Entity workstation = operationComponent.getBelongsToField("operation").getBelongsToField("workstationType");

            if (workstation == null) {
                title = new PrioritizedString(getTranslationService().translate("workPlans.workPlan.report.title.noDivision",
                        locale), 1);
            } else {
                Entity division = workstation.getBelongsToField("division");

                if (division == null) {
                    title = new PrioritizedString(getTranslationService().translate("workPlans.workPlan.report.title.noDivision",
                            locale), 1);
                } else {
                    StringBuilder titleBuilder = new StringBuilder();
                    titleBuilder.append(getTranslationService().translate("workPlans.workPlan.report.title.byDivision", locale));
                    titleBuilder.append(" {");
                    titleBuilder.append(division.getStringField("name"));
                    titleBuilder.append("}");
                    title = new PrioritizedString(titleBuilder.toString());
                }
            }
        }

        return title;
    }

    Map<PrioritizedString, List<Entity>> getOperationComponentsWithDistinction(Entity workPlan,
            final Map<Entity, Entity> operationComponent2order, Locale locale) {
        Map<PrioritizedString, List<Entity>> operationComponentsWithDistinction = new TreeMap<PrioritizedString, List<Entity>>();

        List<Entity> orders = workPlan.getManyToManyField("orders");

        for (Entity order : orders) {
            Entity technology = order.getBelongsToField("technology");
            if (technology == null) {
                continue;
            }
            EntityTree operationComponents = technology.getTreeField("operationComponents");

            for (Entity operationComponent : operationComponents) {
                PrioritizedString title = generateOperationSectionTitle(workPlan, technology, operationComponent, locale);

                if (title == null) {
                    throw new IllegalStateException("undefined workplan type");
                }

                if (!operationComponentsWithDistinction.containsKey(title)) {
                    operationComponentsWithDistinction.put(title, new ArrayList<Entity>());
                }

                operationComponent2order.put(operationComponent, order);
                operationComponentsWithDistinction.get(title).add(operationComponent);
            }
        }

        for (List<Entity> operationComponents : operationComponentsWithDistinction.values()) {
            Collections.sort(operationComponents, new Comparator<Entity>() {

                @Override
                public int compare(Entity o1, Entity o2) {
                    String o1comp = operationComponent2order.get(o1).getStringField("number") + o1.getStringField("nodeNumber");
                    String o2comp = operationComponent2order.get(o2).getStringField("number") + o2.getStringField("nodeNumber");

                    return o1comp.compareTo(o2comp);
                }

            });
        }

        return operationComponentsWithDistinction;
    }

    void addOrderSeries(final PdfPTable table, final List<Entity> orders, final DecimalFormat df) throws DocumentException {
        Collections.sort(orders, new EntityNumberComparator());
        for (Entity order : orders) {
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(new Phrase(order.getField("number").toString(), PdfUtil.getArialRegular9Dark()));
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(new Phrase(order.getField("name").toString(), PdfUtil.getArialRegular9Dark()));

            String unitString = "";

            Entity product = (Entity) order.getField("product");
            if (product == null) {
                table.addCell(new Phrase("", PdfUtil.getArialRegular9Dark()));
            } else {
                String productString = product.getField("name").toString() + " (" + product.getField("number").toString() + ")";
                table.addCell(new Phrase(productString, PdfUtil.getArialRegular9Dark()));

                Object unit = product.getField("unit");
                if (unit != null) {
                    unitString = " " + unit.toString();
                }
            }

            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);

            BigDecimal plannedQuantity = (BigDecimal) order.getField("plannedQuantity");
            plannedQuantity = (plannedQuantity == null) ? BigDecimal.ZERO : plannedQuantity;
            String quantityString = df.format(plannedQuantity) + unitString;
            table.addCell(new Phrase(quantityString, PdfUtil.getArialRegular9Dark()));
            String formattedDateTo = "---";
            if (order.getField("dateTo") != null) {
                synchronized (this) {
                    formattedDateTo = DATE_FORMAT.format((Date) order.getField("dateTo"));
                }
            }
            table.addCell(new Phrase(formattedDateTo, PdfUtil.getArialRegular9Dark()));
        }
    }

    void addMainHeader(final Document document, final Entity entity, final Locale locale) throws DocumentException {
        String documenTitle = getTranslationService().translate("workPlans.workPlan.report.title", locale);
        String documentAuthor = getTranslationService().translate("qcadooReport.commons.generatedBy.label", locale);
        PdfUtil.addDocumentHeader(document, entity.getField("name").toString(), documenTitle, documentAuthor,
                (Date) entity.getField("date"), securityService.getCurrentUserName());
    }

    List<String> prepareOrdersTableHeader(final Document document, final Entity entity, final Locale locale)
            throws DocumentException {
        document.add(new Paragraph(getTranslationService().translate("workPlans.workPlan.report.ordersTable", locale), PdfUtil
                .getArialBold11Dark()));
        List<String> orderHeader = new ArrayList<String>();
        orderHeader.add(getTranslationService().translate("orders.order.number.label", locale));
        orderHeader.add(getTranslationService().translate("orders.order.name.label", locale));
        orderHeader.add(getTranslationService().translate("workPlans.workPlan.report.colums.product", locale));
        orderHeader.add(getTranslationService().translate("orders.order.plannedQuantity.label", locale));
        orderHeader.add(getTranslationService().translate("orders.order.dateTo.label", locale));
        return orderHeader;
    }

    String getOperationsTitle(Entity workPlan) {
        String type = workPlan.getStringField("type");

        if (WorkPlanType.NO_DISTINCTION.getStringValue().equals(type)) {
            return "workPlans.workPlan.report.title.allOperations";
        }

        throw new IllegalStateException("unnexpected workPlan type");
    }
}
