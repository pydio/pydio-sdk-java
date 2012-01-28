package info.ajaxplorer.client.http;

public interface MessageListener {

	public static int MESSAGE_WHAT_MAIN = 0;
	public static int MESSAGE_WHAT_STATE = 1;
	public static int MESSAGE_WHAT_FINISH = 2;
	public static int MESSAGE_WHAT_ERROR = -1;

	static int MESSAGE_STATE_INTERRUPT = 2;
	
	public void sendMessage(int what, Object obj);	
	
	public void requireInterrupt();
	public boolean isInterruptRequired();
	
}
