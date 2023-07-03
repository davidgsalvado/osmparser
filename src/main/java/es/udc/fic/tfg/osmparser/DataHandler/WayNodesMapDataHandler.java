/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.DataHandler;

import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;

import java.util.*;

public class WayNodesMapDataHandler implements MapDataHandler {

    private List<Map<String, Double>> nodesCoordinates;

    public List<Map<String, Double>> getCoordinates() {
        return nodesCoordinates;
    }

    public void setCoordinates(List<Map<String, Double>> nodesCoordinates) {
        this.nodesCoordinates = nodesCoordinates;
    }

    public WayNodesMapDataHandler(){
        nodesCoordinates = new ArrayList<>();
    }

    @Override
    public void handle(BoundingBox boundingBox) {
        //
    }

    @Override
    public void handle(Node node) {
        process(node);
    }

    @Override
    public void handle(Way way) {
        //
    }

    @Override
    public void handle(Relation relation) {
        //
    }

    private void process(Node node){
        Map<String, Double> aux = new LinkedHashMap<>();
        aux.put("lat", node.getPosition().getLatitude());
        aux.put("lon", node.getPosition().getLongitude());
        nodesCoordinates.add(aux);
    }
}
