package crawler;

public class DuplicateThreadException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DuplicateThreadException(String message) {
        super(message);
    }
	
	public DuplicateThreadException() {
        super("Duplicate thread detected!");
    }
}