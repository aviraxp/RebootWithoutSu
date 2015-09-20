lib/AndroidHiddenAPI.jar: AndroidHiddenAPI/android/os/SystemProperties.java AndroidHiddenAPI/android/app/ActivityThread.java
	javac AndroidHiddenAPI/android/os/*.java AndroidHiddenAPI/android/app/*.java
	cd AndroidHiddenAPI; jar -cvf ../lib/AndroidHiddenAPI.jar android/os/SystemProperties.class android/app/ActivityThread.class
