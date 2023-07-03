/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.exceptions;

@SuppressWarnings("serial")
public class SyntaxErrorException extends Exception{

    public SyntaxErrorException(String msg){
        super(msg);
    }

}
