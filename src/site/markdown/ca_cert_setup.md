# CA Certificate Setup

This guide will show how to use an HTTPS certificate from a trusted
Certificate Authority (CA) with this ETS. It is assumed that you already 
have a running instance of TEAM Engine with ETS Security Client 1.0.

## Getting a Certificate

If you have an internet-facing server, you can get a 100% free cert from
[Let's Encrypt][LE]. It is a basic level cert, and lasts only 90 days. However
there are simple tools available to automatically renew the cert before
it expires.

In this example, I will set up a free certificate for the domain
"testbed.gswlab.ca" with Tomcat 7 running on port 8080 (HTTP) and 8443 
(HTTPS). The server is running Ubuntu Server 18.04 LTS.

I have already configured my DNS provider (AWS Route 53) with an `A` record
to point `testbed.gswlab.ca` to the public-facing IPv4 address for this
server. This is necessary for Let's Encrypt to verify ownership.

I will start by installing a newer version of [certbot][]:

```sh
$ sudo apt-get update
$ sudo apt-get install software-properties-common
$ sudo add-apt-repository ppa:certbot/certbot
$ sudo apt-get update
$ sudo apt-get install certbot
```

As the machine is not running a web server on port 80, we can use certbot's
built-in web server to validate the certificate for Let's Encrypt.

```sh
$ sudo certbot certonly --standalone -d testbed.gswlab.ca --email jpbadger@ucalgary.ca --agree-tos --no-eff-email
```

This will create a directory with configuration files in `/etc`, containing
all the certificates we need:

```sh
$ ls /etc/letsencrypt/live/testbed.gswlab.ca
README  cert.pem  chain.pem  fullchain.pem  privkey.pem
```

[certbot]: https://certbot.eff.org
[LE]: https://letsencrypt.org

## Setup Certs for Tomcat

Before Tomcat can use these certificates, they must be placed into a
Java Keystore. Start by creating a PKCS12 file with the certs:

```sh
$ sudo openssl pkcs12 -export -in /etc/letsencrypt/live/testbed.gswlab.ca/fullchain.pem -inkey /etc/letsencrypt/live/testbed.gswlab.ca/privkey.pem -out /etc/letsencrypt/live/testbed.gswlab.ca/full_and_key.p12 -password "pass:secure-key-pass" -name "tomcat"
$ sudo keytool -importkeystore -storetype JKS -deststorepass "key-pass" -destkeypass "key-pass" -destkeystore /opt/keystore.jks -srckeystore /etc/letsencrypt/live/testbed.gswlab.ca/full_and_key.p12 -srcstoretype PKCS12 -srcstorepass "secure-key-pass"
$ sudo chown tomcat7:tomcat7 /opt/keystore.jks
```

Substitute in your own randomly generated passwords for `key-pass` and 
`secure-key-pass`. **Please Note**: for the current version of the ETS, 
the keystore and key passwords must be the same.

Now you can edit Tomcat's `server.xml`, located at 
`/etc/tomcat7/server.xml` on Ubuntu, to specify the keystore for HTTPS:

```xml
<Connector port="8443" protocol="org.apache.coyote.http11.Http11Protocol" 
	URIEncoding="UTF-8" maxThreads="150" SSLEnabled="true" scheme="https" 
	secure="true" clientAuth="false" sslProtocol="TLS" 
	keystoreFile="/opt/keystore.jks" 
	keystorePass="key-pass" keyAlias="tomcat" keyPass="key-pass" />
```

Add this under the Connector for port 8080, then restart Tomcat. The
restart may take some time as it may generate random data for session
IDs.

You should now be able to access the HTTPS version of Tomcat at
[https://testbed.gswlab.ca:8443/](https://testbed.gswlab.ca:8443/).

## Automatically Update the Keystore

As the Let's Encrypt certificate expires every 90 days, cerbot has created
a cron daemon entry in `/etc/cron.d/certbot` to automatically renew the
certificate without our intervention. Unfortunately that only updates
the PEM files, not the Java Keystore.

To fix this, we will add a post-renew hook script to automatically
update the keystore. Create a new script file in 
`/etc/letsencrypt/renewal-hooks/post/tomcat` with the following contents:

```sh
#!/bin/bash
set -e

openssl pkcs12 -export -in /etc/letsencrypt/live/testbed.gswlab.ca/fullchain.pem \
	-inkey /etc/letsencrypt/live/testbed.gswlab.ca/privkey.pem \
	-out /etc/letsencrypt/live/testbed.gswlab.ca/full_and_key.p12 \
	-password "pass:secure-key-pass" -name "tomcat"

keytool -importkeystore -storetype JKS -deststorepass "key-pass" \
	-destkeypass "key-pass" -destkeystore /opt/keystore.jks \
	-srckeystore /etc/letsencrypt/live/testbed.gswlab.ca/full_and_key.p12 \
	-srcstoretype PKCS12 -srcstorepass "secure-key-pass"

chown tomcat7:tomcat7 /opt/keystore.jks
systemctl reload-or-restart tomcat7
```

Again, update the passwords with your secure versions. After saving the
script, make it executable:

```sh
$ sudo chmod +x /etc/letsencrypt/renewal-hooks/post/tomcat
```

Now after certbot renews the PEM certs, it will update the Keystore for
Tomcat too.

## Set up the ETS with the Keystore

After making sure that Tomcat works with HTTPS and has TEAM Engine 
installed, we must customize the CTL script for Security Client 1.0 to
support our keystore.

Edit the file `/opt/te_base/scripts/security-client10/1.0/ctl/security-client10-suite.ctl`,
and change the text contents of the following `<xsl:variable>` elements:

* `address` should stay as `0.0.0.0`
* `port` can stay as `10080`
* `host` should be the same as the domain used in the certificate
* `jks_path` should be changed to `/opt/keystore.jks`
* `jks_password` should be changed to `<!CDATA[key-pass]]`

Using CDATA is recommended for the password, especially if your password
contains special characters that may be incorrectly interpreted as XML.
**If you password contains `]]`** then you will have to escape that portion
of the password.

After saving the file, restart Tomcat. Login to TEAM Engine, and create
a new test session for ETS Security Client 1.0. After starting a test
session, it should give you an endpoint with the format:

`https://0.0.0.0:10080/<nonce>`

On your secure client, you should be able to use 
`https://testbed.gswlab.ca:10080/<nonce>` instead, **and** you should
not need to disable certificate verification.
