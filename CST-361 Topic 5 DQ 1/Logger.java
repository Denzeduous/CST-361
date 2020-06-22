package topic5dq1;

public class Logger implements ILogger {

	public int level;
	
	public Logger(int level) {
		this.level = level;
	}
	
	@Override
	public void error(String msg) {
		if (level <= 3) {
			System.out.print("Error: ");
			System.out.println(msg);
		}
	}

	@Override
	public void info(String msg) {
		if (level <= 2) {
			System.out.print("Info: ");
			System.out.println(msg);
		}
	}

	@Override
	public void debug(String msg) {
		if (level == 1) {
			System.out.print("Debug: ");
			System.out.println(msg);
		}
	}

}
