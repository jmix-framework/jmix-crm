import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/** Asks the Spring Boot application on a local JMX port (default 9191, the first stand) to shut down gracefully. */
public class Shutdown {
    public static void main(String[] args) throws Exception {
        var url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + (args.length > 0 ? args[0] : "9191") + "/jmxrmi");
        JMXConnector connector = JMXConnectorFactory.connect(url);
        try {
            connector.getMBeanServerConnection().invoke(
                    new ObjectName("org.springframework.boot:type=Admin,name=SpringApplication"), "shutdown", null, null);
        } catch (java.rmi.UnmarshalException expected) {
            // The application may close the connection while it shuts down.
        }
        System.out.println("shutdown requested");
        try {
            connector.close();
        } catch (java.io.IOException expected) {
            // The application has already stopped and closed its JMX server.
        }
    }
}
