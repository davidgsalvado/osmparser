/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import java.util.concurrent.*;

public class ExecutorSingleton {

    private final static int NUM_THREADS = 10;

    private static ExecutorService instance = null;

    private ExecutorSingleton(){}

    public static ExecutorService getInstance(){
        if(instance == null){
            instance = Executors.newFixedThreadPool(NUM_THREADS);
        }
        return instance;
    }

}
