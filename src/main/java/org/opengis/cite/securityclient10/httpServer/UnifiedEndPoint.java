package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;

import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Callback;

/**
 * Subclass of EndPoint for reading the first few bytes of a client connection to determine if it is
 * a TLS handshake (HTTPS enabled) or not (HTTP only).
 * 
 * Source: https://stackoverflow.com/a/40076056/237958
 *
 */
public class UnifiedEndPoint implements EndPoint {
	
	/**
	 * Original endpoint being wrapped by this subclass
	 */
	private final EndPoint endPoint;
	/**
	 * Buffer for reading bytes from start
	 */
	private final ByteBuffer start;
	/**
	 * How many bytes from the start need to be read
	 */
	private int leftToRead;
	/**
	 * Bytes read from start
	 */
	private final byte[] bytes;
	/**
	 * Buffered exception for throwing later
	 */
	private IOException pendingException = null; 
	
	public UnifiedEndPoint(final EndPoint channel, final int readAheadLength) {
		this.endPoint = channel;
		start = ByteBuffer.wrap(bytes = new byte[readAheadLength]);
		start.flip();
		leftToRead = readAheadLength;
	}
	
	@Override
	public synchronized int fill(final ByteBuffer dst) throws IOException {
		throwPendingException();
		
		if (leftToRead > 0) {
			readAhead();
		}
		
		if (leftToRead > 0) {
			return 0;
		}
		
		final int sr = start.remaining();
		
		if (sr > 0) {
			dst.compact();
			final int n = readFromStart(dst);
			if (n < sr) {
				return n;
			}
		}
		
		return sr + endPoint.fill(dst);
	}
	
	public byte[] getBytes() {
		if (pendingException == null) {
			try {
				readAhead();
			} catch (final IOException e) {
				pendingException = e;
			}
		}
		
		return bytes;
	}
	
	// TODO: Add wait for slow clients
	private synchronized void readAhead() throws IOException {
		if (leftToRead > 0) {
			final int n = endPoint.fill(start);
			
			if (n == -1) {
				leftToRead = -1;
			} else {
				leftToRead -= n;
			}
			
			if (leftToRead <= 0) {
				start.rewind();
			}
		}
	}
	
	private int readFromStart(final ByteBuffer dst) throws IOException {
		final int n = Math.min(dst.remaining(), start.remaining());
		if (n > 0) {
			dst.put(bytes, start.position(), n);
			start.position(start.position() + n);
			dst.flip();
		}
		return n;
	}
	
	private void throwPendingException() throws IOException {
		if (pendingException != null) {
			final IOException e = pendingException;
			pendingException = null;
			throw e;
		}
	}

	//
	// These following methods are overridden, and defer to the superclass
	//
	
	@Override
	public long getIdleTimeout() {
		return endPoint.getIdleTimeout();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return endPoint.getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return endPoint.getRemoteAddress();
	}

	@Override
	public boolean isOpen() {
		return endPoint.isOpen();
	}

	@Override
	public long getCreatedTimeStamp() {
		return endPoint.getCreatedTimeStamp();
	}

	@Override
	public void shutdownOutput() {
		endPoint.shutdownOutput();
	}

	@Override
	public boolean isOutputShutdown() {
		return endPoint.isOutputShutdown();
	}

	@Override
	public boolean isInputShutdown() {
		return endPoint.isInputShutdown();
	}

	@Override
	public void close() {
		endPoint.close();
	}

	@Override
	public boolean flush(ByteBuffer... buffer) throws IOException {
		return endPoint.flush(buffer);
	}

	@Override
	public Object getTransport() {
		return endPoint.getTransport();
	}

	@Override
	public void setIdleTimeout(long idleTimeout) {
		endPoint.setIdleTimeout(idleTimeout);
	}

	@Override
	public void fillInterested(Callback callback) throws ReadPendingException {
		endPoint.fillInterested(callback);
	}

	@Override
	public boolean tryFillInterested(Callback callback) {
		return endPoint.tryFillInterested(callback);
	}

	@Override
	public boolean isFillInterested() {
		return endPoint.isFillInterested();
	}

	@Override
	public void write(Callback callback, ByteBuffer... buffers) throws WritePendingException {
		endPoint.write(callback, buffers);
	}

	@Override
	public Connection getConnection() {
		return endPoint.getConnection();
	}

	@Override
	public void setConnection(Connection connection) {
		endPoint.setConnection(connection);
	}

	@Override
	public void onOpen() {
		endPoint.onOpen();
	}

	@Override
	public void onClose() {
		endPoint.onClose();
	}

	@Override
	public boolean isOptimizedForDirectBuffers() {
		return endPoint.isOptimizedForDirectBuffers();
	}

	@Override
	public void upgrade(Connection newConnection) {
		endPoint.upgrade(newConnection);
	}
}
