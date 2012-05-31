package info.ajaxplorer.client.util;

import java.io.File;
import java.io.IOException;

public class RdiffProcessor {

	private String rdiffPath = "G:\\PROGRAMS\\rdiff\\rdiff.exe";
	
	public static boolean rdiffEnabled(){
		return true;
	}
	
	public RdiffProcessor(){
		
	}
	
	public void signature(File source, File signature){
		try {
			String[] command = {rdiffPath, "signature", "\""+source.getCanonicalPath()+"\"", "\""+signature.getCanonicalPath()+"\""};
			Process p = Runtime.getRuntime().exec(command);
			int exitValue = p.waitFor();
			//System.out.println("Process exitValue: " + exitValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void delta(File signature, File source, File delta){
		try {
			String[] cmd = {
					rdiffPath, 
					"delta", 
					"\""+signature.getCanonicalPath()+"\"",
					"\""+source.getCanonicalPath()+"\"",
					"\""+delta.getCanonicalPath()+"\""
				};
			Process p = Runtime.getRuntime().exec(cmd);
			int exitValue = p.waitFor();
			//System.out.println("Process exitValue: " + exitValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void patch(File source, File delta, File target){
		try {
			String[] cmd = {
					rdiffPath, 
					"patch", 
					"\""+source.getCanonicalPath()+"\"",
					"\""+delta.getCanonicalPath()+"\"",
					"\""+target.getCanonicalPath()+"\""
				};
			Process p = Runtime.getRuntime().exec(cmd);
			int exitValue = p.waitFor();
			//System.out.println("Process exitValue: " + exitValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
