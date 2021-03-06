/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.jboss.arquillian.graphene.Graphene.waitAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public final class WaitUtils {

    public static void waitAjaxForElement(WebElement element) {
        waitAjax().until()
                .element(element).is().present();
    }

    public static void waitAjaxForElementNotPresent(WebElement element) {
        waitAjax().until()
                .element(element).is().not().present();
    }

    public static void waitGuiForElement(By element, String message) {
        waitGui().until(message)
                .element(element).is().present();
    }

    public static void waitGuiForElement(By element) {
        waitGuiForElement(element, null);
    }

    public static void waitGuiForElement(WebElement element) {
        waitGuiForElementPresent(element, null);
    }

    public static void waitGuiForElementPresent(WebElement element, String message) {
        waitGui().until(message).element(element).is().present();
    }

    public static void waitGuiForElementNotPresent(WebElement element) {
        waitGui().until().element(element).is().not().present();
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Logger.getLogger(WaitUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
