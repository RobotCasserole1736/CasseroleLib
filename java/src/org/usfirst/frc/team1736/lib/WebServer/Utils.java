package org.usfirst.frc.team1736.lib.WebServer;

public class Utils {
	
	static String nameTransform(String name_in){
		// This is technically very bad cuz it maps input names (arbitrary strings) to fewer bits
		//  Sort of a super snazzy silly hash. This might cause collisions if the user is not careful.
		//  We don't warn you about those collisions, so yah... if you're reading this, hope it's not cuz you have 
		//  those problems!
		return name_in.toLowerCase().replace(" ", "_").replaceAll("[\\W]|_", "");
	}

}
