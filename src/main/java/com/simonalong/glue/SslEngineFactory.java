package com.simonalong.glue;

import lombok.experimental.UtilityClass;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

@UtilityClass
public final class SslEngineFactory {

    private final String PROTOCOL = "TLS";

    private SSLContext SERVER_CONTEXT;//服务器安全套接字协议

    private SSLContext CLIENT_CONTEXT;//客户端安全套接字协议

    private final String PASSWORD_KEY = "glue-rpc-key";

    public SSLEngine getServerSslEngine() {
        if (SERVER_CONTEXT != null) {
            SSLEngine sslEngine = SERVER_CONTEXT.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        }
        InputStream in = null;

        try {
            //密钥管理器
            KeyManagerFactory kmf;
            //密钥库KeyStore
            KeyStore ks = KeyStore.getInstance("JKS");
            //加载服务端证书
            in = SslEngineFactory.class.getResourceAsStream("/ssl/glue-server-store.jks");
            //加载服务端的KeyStore  ；sNetty是生成仓库时设置的密码，用于检查密钥库完整性的密码
            ks.load(in, PASSWORD_KEY.toCharArray());
            kmf = KeyManagerFactory.getInstance("SunX509");
            //初始化密钥管理器
            kmf.init(ks, PASSWORD_KEY.toCharArray());
            //获取安全套接字协议（TLS协议）的对象
            SERVER_CONTEXT = SSLContext.getInstance(PROTOCOL);
            //初始化此上下文
            //参数一：认证的密钥      参数二：对等信任认证  参数三：伪随机数生成器 。 由于单向认证，服务端不用验证客户端，所以第二个参数为null
            SERVER_CONTEXT.init(kmf.getKeyManagers(), null, null);

        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        SSLEngine sslEngine = SERVER_CONTEXT.createSSLEngine();
        sslEngine.setUseClientMode(false);
        return sslEngine;
    }

    public SSLEngine getClientSslEngine() {
        if (CLIENT_CONTEXT != null) {
            SSLEngine sslEngine = CLIENT_CONTEXT.createSSLEngine();
            sslEngine.setUseClientMode(true);
            return sslEngine;
        }

        InputStream in = null;
        try {
            //信任库
            TrustManagerFactory tf;
            //密钥库KeyStore
            KeyStore tks = KeyStore.getInstance("JKS");
            //加载客户端证书
            in = SslEngineFactory.class.getResourceAsStream("/ssl/glue-client-store.jks");
            tks.load(in, PASSWORD_KEY.toCharArray());
            tf = TrustManagerFactory.getInstance("SunX509");
            // 初始化信任库
            tf.init(tks);

            CLIENT_CONTEXT = SSLContext.getInstance(PROTOCOL);
            //设置信任证书
            CLIENT_CONTEXT.init(null, tf == null ? null : tf.getTrustManagers(), null);
        } catch (Exception e) {
            throw new Error("Failed to initialize the client-side SSLContext");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        SSLEngine sslEngine = CLIENT_CONTEXT.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return sslEngine;
    }
}

