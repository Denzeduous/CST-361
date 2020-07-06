package singleton;

public class Driver {
	public static void main(String[] args) {
		System.out.println("Getting instance");
		Configuration config = Configuration.getInstance();
		
		System.out.println(config.getA());
		System.out.println(config.getB());
		System.out.println(config.getC());
		System.out.println();
		
		System.out.println("Reloading instance");
		config = Configuration.reload();
		
		System.out.println(config.getA());
		System.out.println(config.getB());
		System.out.println(config.getC());
	}
}
