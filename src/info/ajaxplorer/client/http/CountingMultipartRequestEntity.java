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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class CountingMultipartRequestEntity implements HttpEntity {
    private final HttpEntity delegate;

    private final ProgressListener listener;

    public CountingMultipartRequestEntity(final HttpEntity entity,
            final ProgressListener listener) {
        super();
        this.delegate = entity;
        this.listener = listener;
    }

    public void writeTo(final OutputStream out) throws IOException {
        this.delegate.writeTo(new CountingOutputStream(out, this.listener));
    }

    public static interface ProgressListener {
        void transferred(long num) throws IOException;
        void partTransferred(int part, int total) throws IOException;
    }
    

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener listener;

        private long transferred;

        public CountingOutputStream(final OutputStream out,
                final ProgressListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred);
        }

        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }
    }

	public void consumeContent() throws IOException {
		this.delegate.consumeContent();
		
	}

	public InputStream getContent() throws IOException, IllegalStateException {
		return this.delegate.getContent();
	}

	public Header getContentEncoding() {
		return this.delegate.getContentEncoding();
	}

	public boolean isChunked() {
		return this.delegate.isChunked();
	}

	public boolean isStreaming() {
		return this.delegate.isStreaming();
	}

    public long getContentLength() {
        return this.delegate.getContentLength();
    }

    public Header getContentType() {
        return this.delegate.getContentType();
    }

    public boolean isRepeatable() {
        return this.delegate.isRepeatable();
    }
	
	
}