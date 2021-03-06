# Scripts

These bash scripts automate curl to act as a very simple Secure Client. I use these to verify the test suite, as I don't have access to other secure clients.

**Tip**: If you are using MacOS, you can copy the test endpoint URL to your clipboard and then use `$(pbpaste)` in the command to automatically paste the test endpoint URL. Example:

```terminal
$ ./client wms "$(pbpaste)"
```

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

## `saml-client.rb`

A ruby script that acts as a SAML2 secure client, used to test the test suite SAML2 workflow. It is recommended that Ruby 2.5 or newer is used, and the following Ruby gems are required:

* [faraday](https://github.com/lostisland/faraday) for managing requests and responses
* [nokogiri](https://github.com/sparklemotion/nokogiri) for parsing the XML

Once the gems are installed, the script can be run from the command line:

```terminal
$ ruby scripts/saml-client.rb <wms|wps> "<test endpoint URL>"
```

The following service types have been tested:

* WMS 1.1.1
* WMS 1.3.0
* WPS 2.0 (OWS Common 2.0)
