# Scripts

These bash scripts automate curl to act as a very simple Secure Client. I use these to verify the test suite, as I don't have access to other secure clients.

## `badclient`

Issues a GET request to the service URL, but omits the query parameters (an invalid request). Can be used to test service exception responses.

```terminal
$ ./badclient "https://localhost:10080/aabbccddee"
```

The headers and body from the response will be send to STDOUT.

## `client`

Issues a basic GetCapabilities request to a WMS or WPS. Take the URL from the test suite, and use it as the second argument. Quoting the argument is optional.

```terminal
$ ./client wms "https://localhost:10080/aabbccddee"
```

or

```terminal
$ ./client wps "https://localhost:10080/aabbccddee"
```

The headers and body from the response will be send to STDOUT.

## client.html

This web page contains a small AJAX application to emulate a secure client to test the SAML Browser SSO workflow. See the test suite site docs for "SAML2" for more information on the workflow.

The webpage must be served from a webserver and not from `file://`, as the origin header will be incorrect and the browser will disallow requests to the test suite because of Cross Origin Resource Sharing. There are simple insecure web server one line programs for Python and Ruby that are sufficient for serving the client webpage so you may test it in your web browser.

As you are probably using a self-signed certificate for the HTTPS from the test suite embedded server, your web browser will likely reject it. You can either use "http" in the test endpoint URL, or manually add the self-signed certificate to either your OS or browser certificate trust store.

## `saml-client.rb`

A ruby script that acts as a SAML2 secure client, used to test the test suite SAML2 workflow. It is recommended that Ruby 2.5 or newer is used, and the following Ruby gems are required:

* [faraday](https://github.com/lostisland/faraday) for managing requests and responses
* [nokogiri](https://github.com/sparklemotion/nokogiri) for parsing the XML

Once the gems are installed, the script can be run from the command line:

```terminal
$ ruby scripts/saml-client.rb "<test endpoint URL>"
```

Currently only WMS 1.1.1 is tested and supported.
