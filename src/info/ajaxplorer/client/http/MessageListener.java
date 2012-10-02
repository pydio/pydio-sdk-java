/**
 *  Copyright 2012 Charles du Jeu
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  This file is part of the AjaXplorer Java Client
 *  More info on http://ajaxplorer.info/
 */
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
