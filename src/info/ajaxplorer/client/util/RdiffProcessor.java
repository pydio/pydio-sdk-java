package info.ajaxplorer.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RdiffProcessor {

	private String rdiffPath;
	private boolean testEnabled = false;
	private String escape = "";
	
	public boolean rdiffEnabled(){
		return testEnabled;
	}
	
	public RdiffProcessor(String path){
		rdiffPath = path;
		try{
			testEnabled = testCommand();
		}catch(Exception e){
			testEnabled = false;
		}
		String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0) this.escape = "\"";

	}
	
	protected boolean testCommand() throws IOException, InterruptedException{
		
		String line;
		StringBuilder sb = new StringBuilder();
		Process p = Runtime.getRuntime().exec(rdiffPath);
		BufferedReader bri = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		while ((line = bri.readLine()) != null) {
			sb.append(line);
		}
		bri.close();
		p.waitFor();
		String s = sb.toString();
		return (s.indexOf("signature") > -1 && s.indexOf("patch") > -1 && s.indexOf("delta") > -1);
	}
	
	
	public void signature(File source, File signature){
		try {
			String q = this.escape;
			String[] command = {
					rdiffPath, 
					"signature", 
					q+source.getCanonicalPath()+q, 
					q+signature.getCanonicalPath()+q
					};
			Process p = Runtime.getRuntime().exec(command);
			StringBuffer sb = new StringBuffer();
			String line = new String();
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = bri.readLine()) != null) {
				sb.append(line);
			}
			bri.close();			
			int exitValue = p.waitFor();
			if(exitValue != 0) throw new Exception(sb.toString());			//System.out.println("Process exitValue: " + exitValue);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void delta(File signature, File source, File delta){
		try {
			String q = this.escape;
			String[] cmd = {
					rdiffPath, 
					"delta", 
					q+signature.getCanonicalPath()+q,
					q+source.getCanonicalPath()+q,
					q+delta.getCanonicalPath()+q
				};
			Process p = Runtime.getRuntime().exec(cmd);
			StringBuffer sb = new StringBuffer();
			String line = new String();
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = bri.readLine()) != null) {
				sb.append(line);
			}
			bri.close();			
			int exitValue = p.waitFor();
			if(exitValue != 0) throw new Exception(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void patch(File source, File delta, File target){
		try {
			String q = this.escape;

			String[] cmd = {
					rdiffPath, 
					"patch", 
					q+source.getCanonicalPath()+q,
					q+delta.getCanonicalPath()+q,
					q+target.getCanonicalPath()+q
				};
			Process p = Runtime.getRuntime().exec(cmd);
			StringBuffer sb = new StringBuffer();
			String line = new String();
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = bri.readLine()) != null) {
				sb.append(line);
			}
			bri.close();			
			int exitValue = p.waitFor();
			if(exitValue != 0) throw new Exception(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
}
