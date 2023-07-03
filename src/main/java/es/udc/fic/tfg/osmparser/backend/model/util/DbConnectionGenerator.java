/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import es.udc.fic.tfg.osmparser.DbConnection.DbElements;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.List;

public interface DbConnectionGenerator {

    boolean testConnection(String user, String password);

    String getCompleteConnectionString();

    String createQueryStringForGeoJson(String entityName, String user, String password);

    StringBuilder addValuesQueryString(StringBuilder queryString, DbElements values, int count,
                                       List<DbElements> attributesListMap);

    default boolean checkPointPartOfWay(Point point, List<DbElements> attributesListMap){
        boolean isPart = false;

        for (DbElements values : attributesListMap){
            for (String key : values.getMap().keySet()){
                if(values.getMap().get(key).getValue() instanceof LineString){ //way
                    LineString lineString = (LineString) values.getMap().get(key).getValue();
                    for (Coordinate coordinate : lineString.getCoordinates()){
                        if (point.getX() == coordinate.getX() && point.getY() == coordinate.getY())
                            isPart = true;
                    }
                }
            }
        }

        return isPart;
    }

}
