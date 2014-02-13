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
	
	// default upload chunk size;
	private int uploadChunkSize = RestStateHolder.FILE_UPLOAD_CHUNK_1K;

	private MessageListener messageListener;

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
		if (messageListener != null) {
			messageListener.log("AjxpFileBody - Resetting Chunk Size");
		}
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
				byte[] buf = new byte[uploadChunkSize];
				while(count < limit){
					int len = raf.read(buf);
					// fix the length if
					if (limit - count < len) {
						len = limit - count;
					}
					out.write(buf, 0, len);
					count += len;
					if (messageListener != null) {
						messageListener.log("AjxpFileBody - Writing file: limit=" + limit + " count=" + count);
					}

				}					
				raf.close();

				if (messageListener != null) {
					messageListener.log("AjxpFileBody - Writing another chunk of file: " + this.getFile().getPath() + " STATE: start="
							+ start + " limit="
							+ limit + " chunkIndex=" + chunkIndex);
				}

				//System.out.println("Sent " + count);				
			}else{
				long time = System.currentTimeMillis();
				in = new FileInputStream(getFile());
				byte[] buf = new byte[uploadChunkSize];
				int len;
				while ((len = in.read(buf)) > 0){
					out.write(buf, 0, len);
				}
				in.close();
				if (messageListener != null) {
					messageListener.log("Chunk size: " + uploadChunkSize + " - time for upload: " + (System.currentTimeMillis() - time));
				}
			}
			this.chunkIndex++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public void setUploadChunkSize(int uploadChunkSize) {
		this.uploadChunkSize = uploadChunkSize;
	}

	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}
			
}
