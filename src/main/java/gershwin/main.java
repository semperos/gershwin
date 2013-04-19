package gershwin;

import java.io.IOException;

public class main {

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        clojure.lang.RT.load("gershwin-repl");
    }
}
