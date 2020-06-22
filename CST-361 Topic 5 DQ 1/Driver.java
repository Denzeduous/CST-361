package topic5dq1;

public class Driver {

	public static void main(String[] args) {
		Logger log = new Logger(3);
		LoggerTest lt = new LoggerTest(log);
		
		lt.run();
		System.out.println();

		log.level = 2;
		lt.run();
		System.out.println();
		
		log.level = 1;
		lt.run();
		System.out.println();
	}

}
