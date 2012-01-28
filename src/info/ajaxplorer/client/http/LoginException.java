package info.ajaxplorer.client.http;

public class LoginException extends Exception {

	public Exception e;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8150442444237044031L;
	
	public LoginException (Exception e) {
		this.e=e;
	}

}
