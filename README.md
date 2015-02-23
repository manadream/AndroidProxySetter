# AndroidProxySetter
An android app that sets the proxy settings for a wifi access point by using adb. 
This DOES NOT require root and will work with any device that has USB debugging on or any simulator (including Genymotion).

This app uses the Android Proxy Library from [here](https://github.com/shouldit/android-proxy/tree/master/android-proxy-library)

# Usage

Build and install the apk to your device, then perform the following actions. 

Set proxy by executing this command with extras listed below:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity
	
Extras:

	-e host <host>					# The host ip or address for the proxy
	-e port <port>					# The port for the proxy
	-e ssid <ssid>					# The SSID of the wifi network to set proxy on
	-e key <shared key>				# The password/key for the wifi network
	-e bypass <bypass string>		# The bypass string to use for proxy settings
	-e reset-wifi <boolean>			# Whether or not to reset the wifi settings. This flag will tell
										the tool to forget all connected networks, make a new
										network config with the SSID and key given, and then
										attempt to connect to the wifi network. If no key is given,
										the wifi network is assumed to be unprotected/open
	-e clear <boolean>				# A flag that will clear the proxy settings for the given SSID
	
Note that for setting a proxy, only host, port and ssid are required. The other extras are optional.
    
example of setting the proxy on an open wifi network with a bypass string and reset wifi flag set:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e host 192.168.56.1 -e port 8080 -e ssid PublicWifi -e bypass test.com,test2.com -e reset-wifi true

The proxy can be cleared for an SSID by executing the following:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e ssid <ssid> -e clear true
