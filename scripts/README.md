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
