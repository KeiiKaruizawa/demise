package gr.larryngton.demise.utils.fun;

import lombok.experimental.UtilityClass;

import java.io.IOException;

@UtilityClass
@SuppressWarnings("all")
public class ShitUtils {
    // EZ
    public void crashJVM() {
        Object[] o = null;
        try {
            while (true) {
                Object[] newO = new Object[1];
                newO[0] = o;
                o = newO;
            }
        } finally {
            crashJVM();
        }
    }

    public void shutDown() throws RuntimeException, IOException {
        String shutdownCommand = "";
        String operatingSystem = System.getProperty("os.name");

        if (operatingSystem.equals("Linux") || operatingSystem.equals("Mac OS X")) {
            shutdownCommand = "shutdown -h now";
        } else if (operatingSystem.contains("Windows")) {
            shutdownCommand = "shutdown.exe -s -t 0";
        } else {
            crashJVM();
        }

        Runtime.getRuntime().exec(shutdownCommand);
    }
}