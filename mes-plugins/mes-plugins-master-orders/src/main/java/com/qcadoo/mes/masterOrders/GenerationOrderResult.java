package com.qcadoo.mes.masterOrders;

import com.google.common.collect.Lists;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;

import java.util.List;

public class GenerationOrderResult {

    private TranslationService translationService;

    public GenerationOrderResult(TranslationService translationService) {
        this.translationService = translationService;
    }

    private List<MasterOrderProductErrorContainer> productOrderErrors = Lists.newArrayList();

    private List<String> generatedOrderNumbers = Lists.newArrayList();

    private List<String> ordersWithoutPps = Lists.newArrayList();

    public void addNotGeneratedProductError(MasterOrderProductErrorContainer err) {
        productOrderErrors.add(err);
    }

    public void addGeneratedOrderNumber(String number) {
        generatedOrderNumbers.add(number);
    }

    public void addOrderWithoutPps(String number) {
        ordersWithoutPps.add(number);
    }

    public void showMessage(ViewDefinitionState view) {
        if (!generatedOrderNumbers.isEmpty()) {
            view.addMessage("masterOrders.masterOrder.generationOrder.generatedOrderNumbers", ComponentState.MessageType.INFO,
                    false, String.join(", ", generatedOrderNumbers));
        }

        if (!ordersWithoutPps.isEmpty()) {
            view.addMessage("masterOrders.masterOrder.generationOrder.ordersWithoutPps", ComponentState.MessageType.INFO, false,
                    String.join(", ", ordersWithoutPps));
        }

        if (!productOrderErrors.isEmpty()) {
            productOrderErrors.forEach(err -> {
                StringBuilder msg = new StringBuilder();
                msg.append(translationService.translate(
                        "masterOrders.masterOrder.generationOrder.productNumbersForNotGeneratedOrders", view.getLocale(),
                        err.getProduct(), err.getMasterOrder(), err.getQuantity().toPlainString()));
                msg.append("</br>");
                err.getErrorMessages().forEach(
                        errorMessage -> {
                            msg.append(translationService.translate(errorMessage.getMessage(), view.getLocale(),
                                    errorMessage.getVars()));
                        });
                view.addTranslatedMessage(msg.toString(), ComponentState.MessageType.INFO, false);
            });
        }
    }
}
