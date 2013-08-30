import py4j.GatewayServer;
public class PyInterface {

   public static void main(String[] args) {
    PyInterface app = new PyInterface();
    // app is now the gateway.entry_point
    GatewayServer server = new GatewayServer(app);
    server.start();
  }
  
  public static void runKeyGraph(String [] args) throws Exception
  {
	  
	  topicDetection.Main.main(args);
  }
}