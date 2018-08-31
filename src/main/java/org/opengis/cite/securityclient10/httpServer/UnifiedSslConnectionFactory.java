package org.opengis.cite.securityclient10.httpServer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Wrapper class for SslConnectionFactory to implement a read ahead (peek) on a client connection, to
 * determine if the client is connecting with an HTTPS (SSL/TLS) handshake or without (implying regular
 * HTTP connection).
 * 
 * Usage: Use `UnifiedSslConnectionFactory` in place of `SslConnectionFactory`. This will make the server
 * connector accept both HTTPS *and* HTTP connections; the HttpServletRequest.isSecure() method will now
 * return true or false depending on HTTPS from the client.
 * 
 * Source: https://stackoverflow.com/a/40076056/237958
 *
 */
public class UnifiedSslConnectionFactory extends AbstractConnectionFactory {
	
	private final SslContextFactory _sslContextFactory;
	private final String _nextProtocol;
	
	public UnifiedSslConnectionFactory() {
		this(HttpVersion.HTTP_1_1.asString());
	}
	
	public UnifiedSslConnectionFactory(@Name("next") final String nextProtocol) {
		this((SslContextFactory) null, nextProtocol);
	}
	
	public UnifiedSslConnectionFactory(@Name("sslContextFactory") final SslContextFactory factory, @Name("next") final String nextProtocol) {
		super("SSL");
		this._sslContextFactory = factory == null ? new SslContextFactory() : factory;
		this._nextProtocol = nextProtocol;
		this.addBean(this._sslContextFactory);
	}
	
	public SslContextFactory getSslContextFactory() {
		return this._sslContextFactory;
	}
	
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		final SSLEngine engine = this._sslContextFactory.newSSLEngine();
		engine.setUseClientMode(false);
		final SSLSession session = engine.getSession();
		if (session.getPacketBufferSize() > this.getInputBufferSize()) {
			this.setInputBufferSize(session.getPacketBufferSize());
		}
	}

	@Override
	public Connection newConnection(final Connector connector, final EndPoint realEndPoint) {
		final UnifiedEndPoint aheadEndPoint = new UnifiedEndPoint(realEndPoint, 1);
		final byte[] bytes = aheadEndPoint.getBytes();
		final boolean isSSL; // TODO: rename
		
		if (bytes == null || bytes.length == 0) {
			isSSL = true;
		} else {
			// TLS first byte is 0x16
			// SSLv2 first byte is >= 0x80
			// HTTP is many ASCII bytes
			final byte b = bytes[0];
			isSSL = b >= 0x7F || (b < 0x20 && b != '\n' && b != '\r' && b != '\t');
		}
		
		final EndPoint plainEndPoint;
		final SslConnection sslConnection;
		
		if (isSSL) {
			final SSLEngine engine = this._sslContextFactory.newSSLEngine(aheadEndPoint.getRemoteAddress());
			engine.setUseClientMode(false);
			sslConnection = this.newSslConnection(connector, aheadEndPoint, engine);
			sslConnection.setRenegotiationAllowed(this._sslContextFactory.isRenegotiationAllowed());
			this.configure(sslConnection, connector, aheadEndPoint);
			plainEndPoint = sslConnection.getDecryptedEndPoint();
		} else {
			sslConnection = null;
			plainEndPoint = aheadEndPoint;
		}
		
		final ConnectionFactory next = connector.getConnectionFactory(_nextProtocol);
		final Connection connection = next.newConnection(connector, plainEndPoint);
		
		plainEndPoint.setConnection(connection);
		
		return sslConnection == null ? connection : sslConnection;
	}
	
	protected SslConnection newSslConnection(final Connector connector, final EndPoint endPoint, final SSLEngine engine) {
		return new SslConnection(connector.getByteBufferPool(), connector.getExecutor(), endPoint, engine);
	}
	
	@Override
	public String toString() {
		return String.format("%s@%x{%s->%s}", new Object[] { 
				this.getClass().getSimpleName(),
				Integer.valueOf(this.hashCode()),
				this.getProtocol(),
				this._nextProtocol
		});
	}

}
