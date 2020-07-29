package kk.run;

import kk.beans.Os;
import kk.conf.Conf;

public class Main {
	public static void main(String[] args) {
		if (Conf.os == Os.LINUX) {
			System.setProperty("jdk.gtk.version", "2");
			System.setProperty("prism.verbose", "true");
			System.setProperty("jdk.gtk.verbose", "true");
		}
		MainFx.main(args);
	}
}
