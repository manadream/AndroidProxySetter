# AndroidProxySetter
An android app that sets the proxy settings for a wifi access point by using adb.
This DOES NOT require root and will work with any device that has USB debugging on or any emulator (including Genymotion).

This app uses the Android Proxy Library from [here](https://github.com/shouldit/android-proxy/tree/master/android-proxy-library)

#NOTE
This app now also works on Android Emulators and Genymotion Emulators on Android Marshmallow 6.0 if you build/install the `emulator` build variant or APK.
The `emulator` variant is signed with the Android Emulator platform keystore and thus has permissions to modify the wifi access points again.
The `emulator` apk will only work on emulators, not real devices.

# Usage

Build (or [download](https://github.com/jpkrause/AndroidProxySetter/releases)) and install the apk to your device, then perform the following actions. 

Set proxy by executing this command with extras listed below:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity
	
Extras:

	-e host <host>					# The host ip or address for the proxy
	-e port <port>					# The port for the proxy
	-e ssid <ssid>					# The SSID of the wifi network to set proxy on
									  (optional, will apply on the first one if empty)
	-e key <shared key>				# The password/key for the wifi network
	-e bypass <bypass string>		# The bypass string to use for proxy settings
	-e reset-wifi <boolean>			# Whether or not to reset the wifi settings. This flag will tell
										the tool to forget all connected networks, make a new
										network config with the SSID and key given, and then
										attempt to connect to the wifi network. If no key is given,
										the wifi network is assumed to be unprotected/open
	-e clear <boolean>				# A flag that will clear the proxy settings for the given SSID
	
Note that for setting a proxy, only host, port and ssid are required. The other extras are optional.
Also note that if the `reset-wifi` flag is used it will "forget" any connected networks in order to reset the connection.
    
example of setting the proxy on an open wifi network with a bypass string and reset wifi flag set:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e host 192.168.56.1 -e port 8080 -e ssid PublicWifi -e bypass test.com,test2.com -e reset-wifi true
	
example of setting the proxy on a wifi network with a password:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e host 192.168.56.1 -e port 8080 -e ssid PrivateWifi -e key Passw0rd

The proxy can be cleared for an SSID by executing the following:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e ssid <ssid> -e clear true
	
example of a .sh script, pre-filling the IP Address, for daily updating of the unique SSID
	
	IPADDRESS=`ifconfig en0 | grep inet | grep -v inet6 | awk '{print $2}'`
	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e host $IPADDRESS -e port 8888
