# Security Client 1.0 Test Suite

This document contains additional guidelines for using the test suite,
including manual testing procedures that cannot be automated.

## Manual Checklist: Capabilities with No Content Section

Requirements Class: https://www.opengis.net/def/security/1.0/cr/clientParsing/3

This is a manual test that must be verified by the test operator.

### 1. Conformance Test "HTTPS"

Does the client parse the HTTPS response from the test suite?

* YES - Pass
* NO  - Failure

### 2. Conformance Test "Working on Capabilities with no Content section"

Does the test suite Capabilities contain a `<Content>` section?

(Note that the "Content" section is represented as `<Layer>` in WMS;
see Section 8.2.2 of v1.0 of the specification for other services.)

* YES - Skip Test
* NO  - Continue

Does the test suite Capabilities contain an endpoint URL for the 
`GetCapabilities` operation?

* YES - Continue
* NO  - Failure

Does the `GetCapabilities` operation on that endpoint URL succeed?

* YES - Continue
* NO  - Failure

Does the Capabilities document contain a `<Content>` section?

* YES - Pass
* NO  - Failure

If either Conformance Test went to a "Failure" option, then that Test 
should be marked as a "Failure".
