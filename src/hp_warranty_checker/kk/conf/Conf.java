package kk.conf;

import kk.beans.Os;

public class Conf {
	public static Os os;

	static {
		String o = System.getProperty("os.name").toLowerCase();
		if (o.startsWith("linux"))
			os = Os.LINUX;
		else if (o.startsWith("mac"))
			os = Os.MAC;
		else if (o.startsWith("windows")) {
			os = Os.WINDOWS;
			System.setProperty("line.separator", "\n");
		}
	}
}
