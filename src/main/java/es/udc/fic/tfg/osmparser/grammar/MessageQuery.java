/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.grammar;

import java.util.ArrayList;
import java.util.List;

public class MessageQuery{

    private String bbox;
    private String entity;
    private String fromExpression;
    private List<String> elements = new ArrayList<>();
    private List<String> attributeDefinition = new ArrayList<>();

    public String getBbox() {
        return bbox;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    public String getEntity(){
        return entity;
    }

    public void setEntity(String entity){
        this.entity = entity;
    }

    public String getFromExpression(){
        return fromExpression;
    }

    public void setFromExpression(String fromExpression){
        this.fromExpression = fromExpression;
    }

    public List<String> getAttributeDefinition(){
        return attributeDefinition;
    }

    public void setAttributeDefinition(List<String> attributeDefinition){
        this.attributeDefinition = attributeDefinition;
    }

    public void addAttribute(String attribute){
        this.attributeDefinition.add(attribute);
    }

    public List<String> getElements() {
        return elements;
    }

    public void setElements(List<String> elements) {
        this.elements = elements;
    }
}
