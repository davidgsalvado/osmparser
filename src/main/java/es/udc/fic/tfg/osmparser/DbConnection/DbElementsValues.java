/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.DbConnection;

public class DbElementsValues {

    private boolean isGeom;
    private Object value;
    private String function;

    public DbElementsValues(boolean isGeom, Object value){
        this.isGeom = isGeom;
        this.value = value;
        this.function = "";
    }

    public DbElementsValues(boolean isGeom, Object value, String function){
        this.isGeom = isGeom;
        this.value = value;
        this.function = function;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public DbElementsValues(){}

    public boolean isGeom() {
        return isGeom;
    }

    public void setGeom(boolean geom) {
        isGeom = geom;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
