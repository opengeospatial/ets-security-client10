#!/usr/bin/env ruby
require 'base64'
require 'faraday'
require 'logger'
require 'nokogiri'

# Parse arguments
service_type = ARGV[0].upcase
endpoint_url = ARGV[1]

namespaces = {
  'ows' => 'http://www.opengis.net/ows/1.1',
  'ows2' => 'http://www.opengis.net/ows/2.0',
  'ows_security' => 'http://www.opengis.net/security/1.0',
  'saml' => 'urn:oasis:names:tc:SAML:2.0:assertion',
  'samlp' => 'urn:oasis:names:tc:SAML:2.0:protocol',
  'xlink' => 'http://www.w3.org/1999/xlink'
}

if (service_type != "WMS")
  # For OWS Common services, use OWS2 in place of the 'ows' prefix
  namespaces['ows'] = namespaces['ows2']
end

def print_response(response)
  # Print status
  puts "#{response.status} #{response.reason_phrase}"
end

# Re-use a connection object with SSL verification disabled, because
# the test suite is probably using a self-signed certificate.
# If running the test suite with a real certificate, change this to 
# `true`.
conn = Faraday.new({ ssl: { verify: false } }) do |faraday|
  # Custom logger is used to produce aligned output that is easier to
  # read in STDOUT
  logger = Logger.new(STDOUT)
  logger.formatter = proc do |severity, datetime, progname, msg|
    prefix = "#{progname}: "

    while prefix.length < 10
      prefix = " #{prefix}"
    end

    msg.split("\n").map { |line| "#{prefix}#{line}" }.join("\n") + "\n"
  end
  faraday.response :logger, logger
  faraday.adapter  Faraday.default_adapter
end

# 1. Get Partial Capabilities Document from Service Provider
response = conn.get do |req|
  req.url endpoint_url
  req.params['request'] = 'GetCapabilities'
  req.params['service'] = service_type
  req.headers['Accept'] = 'text/xml, application/xml, */*'
end

puts ""

raise "Unexpected Response: #{response.status}" if response.status != 200

# Parse Complete Capabilities URL from ExtendedSecurityCapabilities
basic_capabilities_doc = Nokogiri::XML(response.body)

if (service_type == "WMS")
  complete_capabilities_url = basic_capabilities_doc.xpath("//ows_security:ExtendedSecurityCapabilities//ows:Operation[@name='GetCapabilities']//ows:Get/@xlink:href", namespaces).to_s
elsif (service_type == "WPS")
  complete_capabilities_url = basic_capabilities_doc.xpath("//ows:OperationsMetadata//ows:Operation[@name='GetCapabilities']//ows:Get/@xlink:href", namespaces).to_s
end

raise "Missing Complete Capabilities URL" if (complete_capabilities_url.nil? || complete_capabilities_url == "")

# 2. Get Complete Capabilities Document from Service Provider
response2 = conn.get do |req|
  req.url complete_capabilities_url
  req.params['request'] = 'GetCapabilities'
  req.params['service'] = service_type
  req.headers['Accept'] = 'text/xml, application/xml, */*'
end

puts ""

raise "Unexpected Response: #{response2.status}" if response2.status != 302

# Parse Location URL for SSO
sso_url = response2.headers["Location"]

# Parse Location URL for RelayState Token
relay_state = sso_url[/RelayState=([^&]+)/, 1]

# 3. Get Single Sign-On from Identity Provider
response3 = conn.get do |req|
  req.url sso_url
  req.headers['Accept'] = 'text/xml, application/xml, */*'
end

puts ""

raise "Missing authentication challenge header" if response3.headers["WWW-Authenticate"].nil?
raise "Wrong authentication challenge type" if !response3.headers["WWW-Authenticate"].start_with?("Basic")

# 4. Get Single Sign-On with Credentials from Identity Provider
response4 = conn.get do |req|
  encoded_auth = Base64.strict_encode64("test-user:test-pass")
  req.url sso_url
  req.headers['Accept'] = 'text/xml, application/xml, */*'
  req.headers['Authorization'] = "Basic #{encoded_auth}"
end

puts ""

raise "Unexpected Response: #{response.status}" if response4.status != 200

# Parse auth response for callback URL in SAML Audience element
encoded_auth_response = response4.body
auth_response_doc = Nokogiri::XML(Base64.decode64(encoded_auth_response))
callback_url = auth_response_doc.xpath('/samlp:Response//saml:Audience', namespaces).text

# 5. Post SAML Authentication Response to Service Provider
response5 = conn.post do |req|
  req.url callback_url
  req.headers['Content-Type'] = 'www-form-urlencoded'
  req.body = "RelayState=#{relay_state}SAMLResponse=#{encoded_auth_response}"
end

puts ""

raise "Unexpected Response: #{response5.status}" if response5.status != 200
raise "Missing set-cookie header" if response5.headers["Set-Cookie"].nil?

puts response5.body
