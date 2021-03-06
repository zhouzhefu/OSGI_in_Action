package cap4;

import org.osgi.framework.*;
import org.osgi.service.log.LogService;

/**
 * Broken code example showing single lookup of a service at bundle startup. The
 * client can now tell when the service is removed, but it doesn't automatically
 * switch to other LogService implementations after the service has disappeared.
 * 
 * To run this example:
 * 
 *   ./chapter04/dynamics/PICK_EXAMPLE 2
 * 
 * Expected result:
 * 
 *   <3> thread="LogService Tester", bundle=2 : logging ON
 *   <3> thread="LogService Tester", bundle=2 : ping
 *   ...
 * 
 * if you stop the simple LogService with "stop 1" you should see the following:
 * 
 *   <3> thread="Thread-1", bundle=2 : logging OFF
 *   <--> thread="LogService Tester", bundle=2 : LogService has gone
 *   ...
 * 
 * Which shows the client bundle knows the LogService it was using has now gone.
 * 
 * Restarting the simple LogService with "start 1" should have no affect, it is
 * only when you restart the client with "stop 2" and "start 2" that it repeats
 * the query in the start method and finds the new service:
 * 
 *   <5> thread="LogService Tester", bundle=2 : logging ON
 *   <5> thread="LogService Tester", bundle=2 : ping
 *   ...
 * 
 * Note that the new LogService has an increased "service.id" property of 5.
 **/
public class Activator_broken_lookup_single implements BundleActivator {

  ServiceReference m_logServiceRef;

  BundleContext m_context;

  /**
   * START LOG TEST
   **/
  public void start(BundleContext context) {

    // this time we store the indirect service reference in the field instead of the instance
    m_logServiceRef = context.getServiceReference(LogService.class.getName());

    // we also need to remember our context so that we can dereference the handle later on
    m_context = context;

    // start new thread to test LogService - remember to keep bundle activator methods short!
    startTestThread();
  }

  /**
   * STOP LOG TEST
   **/
  public void stop(BundleContext context) {

    stopTestThread();
  }

  // Test LogService by periodically sending a message
  class LogServiceTest implements Runnable {
    public void run() {

      while (Thread.currentThread() == m_logTestThread) {

        // we use the saved bundle context and service reference to get the real instance
        LogService logService = (LogService) m_context.getService(m_logServiceRef);

        // if the dereferenced instance is null then we know the service has been removed
        if (logService != null) {
          logService.log(LogService.LOG_INFO, "ping");
        } else {
          alternativeLog("LogService has gone");
        }

        pauseTestThread();
      }
    }
  }

  //------------------------------------------------------------------------------------------
  //  The rest of this is just support code, not meant to show any particular best practices
  //------------------------------------------------------------------------------------------

  volatile Thread m_logTestThread;

  void startTestThread() {
    // start separate worker thread to run the actual tests, managed by the bundle lifecycle
    m_logTestThread = new Thread(new LogServiceTest(), "LogService Tester");
    m_logTestThread.setDaemon(true);
    m_logTestThread.start();
  }

  void stopTestThread() {
    // thread should cooperatively shutdown on the next iteration, because field is now null
    Thread testThread = m_logTestThread;
    m_logTestThread = null;
    if (testThread != null) {
      testThread.interrupt();
      try {testThread.join();} catch (InterruptedException e) {}
    }
  }

  protected void pauseTestThread() {
    try {
      // sleep for a bit
      Thread.sleep(5000);
    } catch (InterruptedException e) {}
  }

  void alternativeLog(String message) {
    // this provides similar style debug logging output for when the LogService disappears
    String tid = "thread=\"" + Thread.currentThread().getName() + "\"";
    String bid = "bundle=" + m_context.getBundle().getBundleId();
    System.out.println("<--> " + tid + ", " + bid + " : " + message);
  }
}
