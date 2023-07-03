/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.DbConnection;

import java.util.LinkedHashMap;
import java.util.Map;

public class DbElements {

    private Map<String, DbElementsValues> map;

    public DbElements(Map<String, DbElementsValues> map){
        this.map = map;
    }

    public DbElements(){
        this.map = new LinkedHashMap<>();
    }

    public Map<String, DbElementsValues> getMap() {
        return map;
    }

    public void setMap(Map<String, DbElementsValues> map) {
        this.map = map;
    }

    public void addValues(String currentBdField, Object currentValue, boolean isGeom){
        map.put(currentBdField, new DbElementsValues(isGeom, currentValue));
    }

    public void addValues(String currentBdField, Object currentValue, boolean isGeom, String function){
        map.put(currentBdField, new DbElementsValues(isGeom, currentValue, function));
    }
}
