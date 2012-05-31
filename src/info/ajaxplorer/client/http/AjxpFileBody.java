package info.ajaxplorer.client.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.apache.http.entity.mime.content.FileBody;

public class AjxpFileBody extends FileBody {

	private String customFileName;
	private int chunkSize = 0;
	private int chunkIndex = 0;
	private int totalChunks;
	private int lastChunkSize;
	
	public AjxpFileBody(File file, String fileName) {
		super(file);
		customFileName = fileName;
	}
	public void chunkIntoPieces(int chunkSize){
		this.chunkSize = chunkSize;			
		totalChunks = (int) Math.ceil( (float)this.getFile().length() / (float)this.chunkSize );
		if( ((float)this.getFile().length() % (float)this.chunkSize ) == 0 ){
			lastChunkSize = chunkSize;
		}else{
			lastChunkSize = (int) getFile().length() - (this.chunkSize*(totalChunks-1));
		}
	}
	public int getCurrentIndex(){
		return this.chunkIndex;
	}
	public int getTotalChunks(){
		return this.totalChunks;
	}
	public void resetChunkIndex(){
		chunkIndex = 0;
	}
	public boolean isChunked(){
		return this.chunkSize > 0;
	}
	public boolean allChunksUploaded(){
		return this.chunkIndex >= totalChunks;
	}
	public String getRootFilename(){
		return customFileName;
	}
	
	@Override
	public String getFilename(){
		if(this.chunkSize > 0){
			if(this.chunkIndex == 0) return customFileName;
			else return customFileName + "-" + this.chunkIndex;				
		}
		return customFileName;
	}
	
	public long getContentLength(){
		if(this.chunkSize > 0) {
			if(this.chunkIndex == (totalChunks - 1)){
				return (long) lastChunkSize;
			}else{
				return (long) this.chunkSize;
			}
		}
		else return getFile().length();
	}
	
	public void writeTo(OutputStream out){
		InputStream in;
		try {
			if(this.chunkSize > 0){
				//System.out.println("Uploading file part " + this.chunkIndex);
				RandomAccessFile raf = new RandomAccessFile(getFile(), "r");
				int start = chunkIndex * this.chunkSize;
				int count = 0;
				int limit = chunkSize;
				if(chunkIndex == (totalChunks -1)){
					limit = lastChunkSize;
				}
				raf.seek(start);
				while(count < limit){
					int byt =raf.read();
					out.write(byt);
					count++;
				}					
				raf.close();
				//System.out.println("Sent " + count);				
			}else{
				in = new FileInputStream(getFile());
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0){
					out.write(buf, 0, len);
				}
				in.close();
			}
			this.chunkIndex++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
			
}
