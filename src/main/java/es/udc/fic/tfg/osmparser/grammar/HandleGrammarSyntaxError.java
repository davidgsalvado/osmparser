/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.grammar;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class HandleGrammarSyntaxError {

    private PrintStream console;
    private PrintStream out;

    public HandleGrammarSyntaxError() throws FileNotFoundException {
        this.console = System.out;
        this.out = new PrintStream(System.getProperty("user.dir") +
                "/src/main/resources/buffer.txt");
    }

    public void setOutFile(){
        System.setOut(this.out);
    }

    public void setOutConsole(){
        System.setOut(this.console);
    }

}
