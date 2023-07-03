/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.DataHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FunctionSelector {

    protected enum PredefinedFunctions{
        toBoolean
    }

    private FunctionSelector(){}

    public static Object selectFunction(String function, List<String> parameters){

        switch(function){
            case "toBoolean":
                return toBoolean(parameters.get(0));

            default:
                return null;
        }
    }

    private static Boolean toBoolean(String value){
        return Objects.equals(value, "yes");
    }
}
