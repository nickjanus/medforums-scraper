package crawler;

public class Main {
	public static void main(String[] args) {
		Crawler robot = new Crawler();
		robot.start("http://exchanges.webmd.com/" + args[0] + "/forum/index",  args[0]);
	}
}

//http://exchanges.webmd.com/asthma-exchange/forum/index
//http://exchanges.webmd.com/urology/forum/index
//http://exchanges.webmd.com/organ-transplant-exchange/forum/index