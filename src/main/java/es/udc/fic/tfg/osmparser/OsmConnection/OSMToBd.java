/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.OsmConnection;

import de.westnordost.osmapi.common.errors.OsmApiException;
import de.westnordost.osmapi.common.errors.OsmApiReadResponseException;
import de.westnordost.osmapi.common.errors.OsmConnectionException;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import es.udc.fic.tfg.osmparser.backend.model.util.ServerCallback;
import org.apache.commons.lang3.exception.ExceptionUtils;
import es.udc.fic.tfg.osmparser.DataHandler.MyMapDataHandler;
import es.udc.fic.tfg.osmparser.DataHandler.WayNodesMapDataHandler;
import es.udc.fic.tfg.osmparser.DbConnection.BdConnection;
import es.udc.fic.tfg.osmparser.DbConnection.DbParameters;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OSMToBd {

    public static final int MAX_TRIES=20;

    private OverpassMapDataApi mapApi;
    private List<String> queriesList;
    private List<String> entities;
    private List<List<String>> attributesToSearch;
    private List<DbParameters> dbParameters;

    public OSMToBd(List<String> queriesList, List<String> entities, List<List<String>> attributesToSearch, OverpassMapDataApi mapApi,
                   List<DbParameters> dbParameters){
        this.queriesList = queriesList;
        this.entities = entities;
        this.attributesToSearch = attributesToSearch;
        this.mapApi = mapApi;
        this.dbParameters = dbParameters;
    }

    public OSMToBd(List<String> queriesList, OverpassMapDataApi mapApi){
        this.queriesList = queriesList;
        this.mapApi = mapApi;
    }

    public List<String> getQueriesList() {
        return queriesList;
    }

    public void setQueriesList(List<String> queriesList) {
        this.queriesList = queriesList;
    }

    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities;
    }

    public List<List<String>> getAttributesToSearch() {
        return attributesToSearch;
    }

    public void setAttributesToSearch(List<List<String>> attributesToSearch) {
        this.attributesToSearch = attributesToSearch;
    }

    public OverpassMapDataApi getMapApi() {
        return mapApi;
    }

    public void setMapApi(OverpassMapDataApi mapApi) {
        this.mapApi = mapApi;
    }

    public List<DbParameters> getDbParameters() {
        return dbParameters;
    }

    public void setDbParameters(List<DbParameters> dbParameters) {
        this.dbParameters = dbParameters;
    }

    public void doConnection(Connection connection, int currentQuery, int currentHandlerParameters,
                             DbParameters dbParameters)
            throws InterruptedException {
        MyMapDataHandler handler = new MyMapDataHandler(attributesToSearch.get(currentHandlerParameters),
                entities.get(currentHandlerParameters), mapApi);
        TimeUnit.SECONDS.sleep(2);
        mapApi.queryElements(queriesList.get(currentQuery), handler);
        BdConnection bd = new BdConnection(entities.get(currentHandlerParameters), handler.getMap(), dbParameters);
        bd.create(connection);
    }

    public void doConnection(Connection connection, int currentQuery, int currentHandlerParameters, ServerCallback serverCallback,
                             Long taskId, DbParameters dbParameters) throws InterruptedException {
        try{
            MyMapDataHandler handler = new MyMapDataHandler(attributesToSearch.get(currentHandlerParameters),
                    entities.get(currentHandlerParameters), mapApi, serverCallback, taskId);
            TimeUnit.SECONDS.sleep(2);
            mapApi.queryElements(queriesList.get(currentQuery), handler);
            BdConnection bd = new BdConnection(entities.get(currentHandlerParameters), handler.getMap(), dbParameters);
            bd.create(connection);
        }catch (Exception e){
            serverCallback.parseFinishWithError(taskId, ExceptionUtils.getStackTrace(e));
            System.out.println("THE TASK WITH ID " + taskId + " HAS FINISHED EXECUTION WITH AN ERROR");
            serverCallback.sendLogs(taskId, "THE TASK FINISHED EXECUTION WITH AN ERROR" +
                    ExceptionUtils.getStackTrace(e) + "\n", "ERROR");
            e.printStackTrace();
            throw(e);
        }

    }

    public List<Map<String, Double>> doWayNodesOSMConnection(ServerCallback serverCallback, Long taskId, int times)
            throws InterruptedException {
        if(times == 0) TimeUnit.SECONDS.sleep(2);
        else if(times > 0) // come from error
            TimeUnit.SECONDS.sleep(10);
        WayNodesMapDataHandler handler = new WayNodesMapDataHandler();
        if(times == MAX_TRIES){
            throw new RuntimeException();
        }
        try{
            mapApi.queryElements(queriesList.get(0), handler);
        }catch (OsmConnectionException e){
            if (serverCallback != null)
                serverCallback.sendLogs(taskId, "ERROR Code " + e.getErrorCode()+ ": OsmConnectionException; "
                        + e.getDescription() + "\n", "ERROR");
            System.out.println("ERROR Code " + e.getErrorCode()+ ": OsmConnectionException; " + e.getDescription());
            doWayNodesOSMConnection(serverCallback, taskId, ++times);
        }catch (OsmApiException e){
            if (serverCallback != null)
                serverCallback.sendLogs(taskId, "ERROR Code " + e.getErrorCode()+ ": OsmApiException; "
                        + e.getDescription() + "\n", "ERROR");
            System.out.println("ERROR Code " + e.getErrorCode()+ ": OsmApiException; " + e.getDescription());
            doWayNodesOSMConnection(serverCallback, taskId, ++times);
        } catch (OsmApiReadResponseException e){
            if (serverCallback != null)
                serverCallback.sendLogs(taskId, "OsmApiReadResponseException: \n" + ExceptionUtils.getStackTrace(e) + "\n",
                        "ERROR");
            System.out.println("OsmApiReadResponseException: ");
            e.printStackTrace();
            doWayNodesOSMConnection(serverCallback, taskId, ++times);
        }


        return handler.getCoordinates();
    }
}
