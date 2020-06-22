package topic5dq1;

public class LoggerTest {
	public ILogger log;
	
	public LoggerTest(ILogger log) {
		this.log = log;
	}
	
	public void run() {
		log.debug("Hi");
		log.info ("Hi");
		log.error("Hi");
	}
}
