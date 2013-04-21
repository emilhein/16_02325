import controller.Controller;
public class main {
	
	public static void main(String[] args) {
		String sti = "log.txt";
		Controller.writeFile(sti);
		for(int i=0;i<10;i++){
			System.out.println("Hello world!");
		}

		new Controller();
	}

}
