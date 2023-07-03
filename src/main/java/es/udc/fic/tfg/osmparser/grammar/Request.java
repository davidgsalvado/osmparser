/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.grammar;

public class Request {

    private String connectStatement;
    private boolean isConnect;
    private MessageQuery query;

    public Request(String connectStatement){
        this.connectStatement = connectStatement;
        this.isConnect = true;
    }

    public Request(MessageQuery query){
        this.query = query;
        this.isConnect = false;
    }

    public String getConnectStatement() {
        return connectStatement;
    }

    public void setConnectStatement(String connectStatement) {
        this.connectStatement = connectStatement;
    }

    public boolean getIsConnect() {
        return isConnect;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }

    public MessageQuery getQuery() {
        return query;
    }

    public void setQuery(MessageQuery query) {
        this.query = query;
    }
}
