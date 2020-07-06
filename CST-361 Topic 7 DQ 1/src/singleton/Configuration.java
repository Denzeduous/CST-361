package singleton;

public class Configuration {
	private static Configuration instance;
	private int a;
	private int b;
	private int c;
	
	private Configuration(int a, int b, int c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public int getA() {
		return a;
	}
	
	public int getB() {
		return b;
	}
	
	public int getC() {
		return c;
	}
	
	public static Configuration getInstance() {
		if (instance == null)
			instance = new Configuration(0, 1, 2);
		
		return instance;
	}
	
	public static Configuration reload() {
		instance = new Configuration(3, 4, 5);
		
		return instance;
	}
}
